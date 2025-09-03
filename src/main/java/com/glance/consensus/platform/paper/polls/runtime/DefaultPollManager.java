package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.utils.Mini;
import com.glance.consensus.utils.StringUtils;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Slf4j
@Singleton
@AutoService(Manager.class)
public final class DefaultPollManager implements PollManager {

    private final Plugin plugin;
    private final Logger logger;

    // TODO: add to config
    private static final Duration CLOSED_RETENTION = Duration.ofDays(3);

    // Build Wizard Managers
    private final PollBuildNavigator navigator;
    private final PollBuilderSessions sessions;

    // Storage
    private final Provider<PollStorage> storageProvider;

    // Poll Cache
    private final Map<UUID, PollRuntime> polls = new ConcurrentHashMap<>();

    private static final long SWEEP_PERIOD_TICKS = 20L * 30; // 30s task
    private BukkitTask sweepTask;

    @Inject
    public DefaultPollManager(
        @NotNull final Plugin plugin,
        @NotNull final PollBuildNavigator navigator,
        @NotNull final PollBuilderSessions sessions,
        @NotNull final Provider<PollStorage> storage
    ) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.navigator = navigator;
        this.sessions = sessions;
        this.storageProvider = storage;
    }

    @Override
    public void startBuildSession(@NotNull Player player, @Nullable String suppliedId) {
        if (this.sessions.has(player.getUniqueId())) {
            this.navigator.open(player, PollBuildSession.Stage.OVERRIDE);
            return;
        }

        PollBuildSession session = this.sessions.getOrCreate(player.getUniqueId());
        session.setSuppliedId(suppliedId);
        this.navigator.open(player, PollBuildSession.Stage.GENERAL);
    }

    @Override
    public @NotNull Poll buildFromSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    ) {
        String question = Objects.requireNonNullElse(session.getQuestionRaw(), "").trim();
        if (question.isBlank()) throw new IllegalArgumentException("Question cannot be empty");
        if (session.getAnswers().size() < 2) throw new IllegalArgumentException("At least 2 options are required");

        List<PollOption> normalized = new ArrayList<>();
        for (int i = 0; i < session.getAnswers().size(); i++) {
            var option = session.getAnswers().get(i);
            String label = option.labelRaw().trim();
            if (label.isBlank()) throw new IllegalArgumentException("Option " + (i+1) + " has no label");
            normalized.add(new PollOption(i, label, StringUtils.emptyToNull(option.tooltipRaw()), 0));
        }

        int minutes = Math.max(1, session.resolveDurationMins()); // min 1 minute
        var rules = new PollRules(
                session.isMultipleChoice(),
                Math.max(1, Math.min(PollBuildSession.MAX_OPTIONS, session.getMaxSelections())),
                session.isAllowResubmission(),
                session.isViewResults()
        );

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant closeAt = now.plus(Duration.ofMinutes(minutes));

        var poll = new Poll(
            id,
            session.getSuppliedId() != null ? session.getSuppliedId() : id.toString(),
            creator.getUniqueId(),
            question,
            now,
            closeAt,
            null,
            normalized,
            rules
        );
        poll.setClosed(false);

        return poll;
    }

    @Override
    public CompletableFuture<UUID> registerPoll(@NotNull Player creator, @NotNull Poll poll) {
        return storageProvider.get().createPoll(poll)
            .thenApply(p -> {
                var runtime = new PollRuntime(poll);
                polls.put(poll.getId(), runtime);
                return poll.getId();
            })
            .exceptionally(ex -> {
               logger.severe("Failed to register poll " + poll.getPollIdentifier()
                       + " | " + ex.getMessage());
               return null;
            });
    }

    @Override
    public Optional<PollRuntime> get(@NotNull UUID pollId) {
        return Optional.ofNullable(polls.get(pollId));
    }

    @Override
    public Collection<PollRuntime> all() {
        return polls.values();
    }

    @Override
    public Collection<PollRuntime> active() {
        return polls.values().stream().filter(p -> !p.getPoll().isClosed()).toList();
    }

    @Deprecated
    @Override
    public void markVoted(@NotNull UUID pollId, @NotNull UUID voterId) {}

    @Override
    public boolean close(@NotNull UUID pollId) {
        var runtime = polls.get(pollId);
        if (runtime == null) return false;
        return safeClose(runtime.getPoll(), Instant.now());
    }

    @Override
    public Set<UUID> findVoters(@NotNull UUID pollId) {
        var opt = get(pollId);
        return opt.isEmpty() ? Set.of() : opt.get().votersSnapshot();
    }

    private boolean safeClose(@NotNull Poll poll, Instant closeTime) {
        UUID pollId = poll.getId();
        storageProvider.get().closePoll(pollId, closeTime)
            .thenRun(() -> {
                Set<UUID> voters = findVoters(pollId);
                if (voters.isEmpty()) return;

                Component msg = Mini.parseMini(
                    "<newline>" +
                    "<gray>Poll:<b><newline><white>\"</white>" + poll.getQuestionRaw() +
                    "<white>\"</white><newline></b>Has now <red><u>closed</u></red></gray><newline>" +
                    "<hover:show_text:'<green>Click to view results</green>'>" +
                    "<click:run_command:'/poll results " + poll.getPollIdentifier() + "'>" +
                    "<aqua>[View Results]</aqua>" +
                    "</click></hover>" +
                    "<newline>"
                );

                for (UUID voterId : voters) {
                    Player p = Bukkit.getPlayer(voterId);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(msg);
                    }
                }
            })
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to persist poll close for: " +
                        poll.getPollIdentifier());
                return null;
            });

        var runtime = polls.get(pollId);
        if (runtime == null) return false;
        runtime.close();
        return true;
    }

    @Override
    public void onEnable() {
        loadPolls();
    }

    private void loadPolls() {
        PollStorage storage = storageProvider.get();
        if (storage == null) throw new IllegalStateException("A Storage system was not initialized");

        storage.loadRecentPolls(CLOSED_RETENTION).thenAccept(list -> {
            if (list == null) return;

            List<CompletableFuture<Void>> voteLoadFutures = new ArrayList<>(list.size());

            for (Poll p : list) {
                var future = storage.loadAllSelections(p.getId())
                    .thenAccept(selections -> {
                        PollRuntime rt = polls.computeIfAbsent(p.getId(), __ -> new PollRuntime(p));
                        selections.forEach(rt::supplySelectionBootstrap);
                    })
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("Failed to reload/supply vote selections for poll " +
                                p.getPollIdentifier());
                        return null;
                    });

                voteLoadFutures.add(future);
            }

            CompletableFuture.allOf(voteLoadFutures.toArray(CompletableFuture[]::new))
                .whenComplete((d, ex) -> {
                    if (ex != null) {
                        plugin.getLogger().warning("One of more polls failed to load their votes: "
                                + ex.getMessage());
                    }

                    if (sweepTask != null) {
                        sweepTask.cancel();
                    }

                    sweepTask = plugin
                            .getServer()
                            .getScheduler()
                            .runTaskTimerAsynchronously(plugin, this::sweepOnce, 20L * 2, SWEEP_PERIOD_TICKS);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to load recent polls: " + ex.getMessage());
                    if (sweepTask != null) sweepTask.cancel();
                    // still start a sweeper for anything still in memory
                    sweepTask = plugin
                            .getServer()
                            .getScheduler()
                            .runTaskTimerAsynchronously(plugin, this::sweepOnce, 20L * 2, SWEEP_PERIOD_TICKS);

                    return null;
                });
        })
        .exceptionally(ex -> {
            logger.warning("Failed to load recent polls: " + ex.getMessage());
            return null;
        });
    }

    private void sweepOnce() {
        closeDuePolls();
        reapOldPolls();
    }

    private void closeDuePolls() {
        final Instant now = Instant.now();

        List<Poll> due = polls.values().stream()
                .map(PollRuntime::getPoll)
                .filter(p -> !p.isClosed() && !now.isBefore(p.getClosesAt()))
                .toList();

        due.forEach(p -> safeClose(p, p.getClosesAt()));
    }

    private void reapOldPolls() {
        long cutoff = System.currentTimeMillis() - CLOSED_RETENTION.toMillis();
        polls.values().removeIf(rt -> {
            var p = rt.getPoll();
            if (!p.isClosed()) return false;

            long closedAt = p.getClosedAt() != null
                    ? p.getClosedAt().toEpochMilli()
                    : p.getClosesAt().toEpochMilli();
            return closedAt < cutoff;
        });
    }

    @Override
    public void onDisable() {
        // todo persistence update here?
        polls.clear();
    }

}

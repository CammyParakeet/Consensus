package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.utils.StringUtils;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Singleton
@AutoService(Manager.class)
public final class DefaultPollManager implements PollManager {

    private final Plugin plugin;

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
    public CompletableFuture<UUID> createFromBuildSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    ) throws IllegalArgumentException {
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
            session.isAllowResubmission()
        );

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant closeAt = now.plus(Duration.ofMinutes(minutes));

        var poll = new Poll(
            id,
            session.getSuppliedId(),
            creator.getUniqueId(),
            question,
            now,
            closeAt,
            null,
            normalized,
            rules
        );
        poll.setClosed(false);

        return storageProvider.get().createPoll(poll).thenApply(p -> {
            var runtime = new PollRuntime(poll);

            log.warn("Created poll {}", poll);
            polls.put(id, runtime);

            return id;
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

    @Override
    public boolean close(@NotNull UUID pollId) {
        var runtime = polls.get(pollId);
        if (runtime == null) return false;
        return safeClose(runtime.getPoll());
    }

    private boolean safeClose(@NotNull Poll poll) {
        UUID id = poll.getId();
        storageProvider.get().closePoll(id, Instant.now())
                .thenRun(() -> {
                    // TODO
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Failed to persist poll close for: " +
                            poll.getPollIdentifier());
                    return null;
                });

        var runtime = polls.get(id);
        if (runtime == null) return false;
        runtime.close();
        return true;
    }

    @Override
    public void onEnable() {
        PollStorage storage = storageProvider.get();
        if (storage == null) throw new IllegalStateException("A Storage system was not initialized");

        storage.loadRecentPolls(CLOSED_RETENTION).thenAccept(list -> {
            for (Poll p : list) {
                PollRuntime r = new PollRuntime(p);
                // todo get tallies from storage
//                storage.loadTallies(p.getId()).thenAccept(t -> {
//                    // apply counts to runtime’s answers
//                    var opts = p.getOptions();
//                    for (int i = 0; i < opts.size(); i++) {
//                        var a = opts.get(i);
//                        opts.set(i, a.withVotes(t.getOrDefault(i, 0)));
//                    }
//                });
                polls.put(p.getId(), r);
            }

            sweepTask = plugin
                    .getServer()
                    .getScheduler()
                    .runTaskTimerAsynchronously(plugin, this::sweepOnce, 20L * 2, SWEEP_PERIOD_TICKS);
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

        due.forEach(this::safeClose);
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

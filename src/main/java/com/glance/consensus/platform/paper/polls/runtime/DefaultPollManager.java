package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollAnswer;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.utils.StringUtils;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@AutoService(PollManager.class)
public class DefaultPollManager implements PollManager {

    private final Plugin plugin;

    // Build Wizard Managers
    private final PollBuildNavigator navigator;
    private final PollBuilderSessions sessions;

    // Poll Cache
    private final Map<UUID, PollRuntime> polls = new ConcurrentHashMap<>();

    @Inject
    public DefaultPollManager(
        @NotNull Plugin plugin,
        @NotNull PollBuildNavigator navigator,
        @NotNull PollBuilderSessions sessions
    ) {
        this.plugin = plugin;
        this.navigator = navigator;
        this.sessions = sessions;
    }

    @Override
    public UUID createFromBuildSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    ) throws IllegalArgumentException {
        String question = Objects.requireNonNullElse(session.getQuestionRaw(), "").trim();
        if (question.isBlank()) throw new IllegalArgumentException("Question cannot be empty");
        if (session.getAnswers().size() < 2) throw new IllegalArgumentException("At least 2 options are required");

        List<PollAnswer> normalized = new ArrayList<>();
        for (int i = 0; i < session.getAnswers().size(); i++) {
            var option = session.getAnswers().get(i);
            String label = option.labelRaw().trim();
            if (label.isBlank()) throw new IllegalArgumentException("Option " + (i+1) + " has no label");
            normalized.add(new PollAnswer(i, label, StringUtils.emptyToNull(option.tooltipRaw()), 0));
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

        var poll = new Poll(id, creator.getUniqueId(), question, now, closeAt, normalized, rules);
        var runtime = new PollRuntime(poll);

        log.warn("Created poll {}", poll);
        polls.put(id, runtime);

        return id;
    }

    @Override
    public Optional<PollRuntime> get(@NotNull UUID pollId) {
        return Optional.ofNullable(polls.get(pollId));
    }

    @Override
    public Collection<PollRuntime> active() {
        log.warn("Active poll? {}", polls);
        return polls.values().stream().filter(p -> !p.getPoll().isClosed()).toList();
    }

    @Override
    public boolean close(@NotNull UUID pollId) {
        return safeClose(pollId);
    }

    private boolean safeClose(@NotNull UUID pollId) {
        var runtime = polls.get(pollId);
        if (runtime == null) return false;
        runtime.close();
        return true;
    }

    @Override
    public void onEnable() {
        // todo populate a cache?
    }

    @Override
    public void onDisable() {
        // todo persistence update here?
        polls.clear();
    }
}

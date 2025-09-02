package com.glance.consensus.platform.paper.polls.display;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.polls.runtime.VoteManager;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.glance.consensus.platform.paper.utils.Mini;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Slf4j
@Singleton
@AutoService(Manager.class)
public class DefaultPollDisplayNavigator implements PollDisplayNavigator {

    private final Map<PollDisplay.Mode, PollDisplay> displays;
    private final VoteManager voteManager;

    @Inject
    public DefaultPollDisplayNavigator(
        @NotNull Map<PollDisplay.Mode, PollDisplay> displays,
        @NotNull VoteManager voteManager
    ) {
        this.voteManager = voteManager;
        this.displays = displays;
    }

    @Override
    public void openVoting(@NotNull Player player, @NotNull PollRuntime runtime) {
        PollDisplay display = displayFor(runtime);
        PollRules effective = RuleUtils.effectiveRules(player, runtime.getPoll().getRules());
        display.openVoting(player, runtime, effective);
    }

    @Override
    public void openResults(@NotNull Player player, @NotNull PollRuntime runtime) {
        PollDisplay display = displayFor(runtime);
        PollRules effective = RuleUtils.effectiveRules(player, runtime.getPoll().getRules());
        display.openResults(player, runtime, effective);
    }

    @Override
    public void refresh(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        PollDisplay.@NotNull RefreshCause cause
    ) {
        PollDisplay display = displayFor(runtime);
        display.refresh(player, runtime, cause);
    }

    @Override
    public void handleVoteClick(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        int optionIndex
    ) {
        voteManager.attemptVote(player, runtime, optionIndex).thenAccept(result -> {
            if (result.userMsg() != null && !result.userMsg().isBlank()) {
                player.sendMessage(Mini.parseMini(result.userMsg()));
            }

            // Map the outcome to a refresh cause
            final PollDisplay.RefreshCause cause = switch (result.status()) {
                case ACCEPTED, NO_OP -> PollDisplay.RefreshCause.SELECTION_CHANGED;
                case REJECTED_RULES -> PollDisplay.RefreshCause.RULES_REJECTED;
                case REJECTED_CLOSED -> PollDisplay.RefreshCause.POLL_CLOSED;
                case ROLLBACK -> PollDisplay.RefreshCause.STORAGE_ERROR;
            };

            refresh(player, runtime, cause);
        });
    }

    private @NotNull PollDisplay displayFor(@NotNull PollRuntime runtime) {
        PollDisplay.Mode mode = runtime.getMode();
        PollDisplay display = displays.get(mode);
        if (display == null) {
            throw new IllegalStateException("No PollDisplay supported for mode " + mode);
        }
        return display;
    }

}

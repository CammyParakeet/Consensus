package com.glance.consensus.platform.paper.polls.display;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import com.glance.consensus.platform.paper.polls.runtime.VoteManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PollDisplayNavigator extends Manager {

    void openVoting(
        @NotNull Player player,
        @NotNull PollRuntime runtime
    );

    void openResults(
        @NotNull Player player,
        @NotNull PollRuntime runtime
    );

    /**
     * Unified click entry point for displays
     * <p>
     * Handles voting via {@link VoteManager}, surfaces a short message,
     * and triggers a display refresh with an appropriate cause
     */
    void handleVoteClick(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        int optionIndex
    );

    void refresh(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull PollDisplay.RefreshCause cause
    );

}

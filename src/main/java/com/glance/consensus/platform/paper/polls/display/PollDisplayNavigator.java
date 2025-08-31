package com.glance.consensus.platform.paper.polls.display;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
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
    void refresh(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull PollDisplay.RefreshCause cause
    );
}

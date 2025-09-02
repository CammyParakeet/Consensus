package com.glance.consensus.platform.paper.polls.display;

import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PollDisplay {

    @NotNull Mode mode();

    void openVoting(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull PollRules rules
    );

    void openResults(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull PollRules ctx
    );

    void refresh(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        @NotNull RefreshCause cause
    );

    enum Mode {
        BOOK,
        DIALOG,
        CHAT,
        GUI
    }

    enum RefreshCause {
        SELECTION_CHANGED,
        RULES_REJECTED,
        POLL_CLOSED,
        STORAGE_ERROR,
        MANUAL
    }

}

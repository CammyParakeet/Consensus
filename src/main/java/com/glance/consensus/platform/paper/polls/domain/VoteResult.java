package com.glance.consensus.platform.paper.polls.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public record VoteResult(
    Status status,
    UUID pollId,
    Set<Integer> selection,
    String messageRaw,
    NextAction nextAction
) {

    public static VoteResult of(
        @NotNull Status status,
        @NotNull UUID pollId,
        @NotNull Set<Integer> sel,
        @NotNull String msg,
        @NotNull NextAction nextAction
    ) {
        return new VoteResult(status, pollId, sel, msg, nextAction);
    }

    public enum Status {
        SUCCESS,
        CLOSED, // poll closed
        NOT_FOUND,  // poll not found
        INVALID_OPTION, // invalid index for vote
        REJECTED // rule violation
    }

    public enum NextAction {
        REFRESH_DISPLAY,   // allow more actions within the display
        OPEN_RESULTS,   // auto-switch into results view
        NONE
    }

}

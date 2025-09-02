package com.glance.consensus.platform.paper.polls.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public record VoteResult(
    @NotNull UUID pollId,
    @NotNull Status status,
    @NotNull Set<Integer> effectiveSelections, // server truth after operation
    @Nullable String userMsg
) {

    public static VoteResult of(
        @NotNull UUID pollId,
        @NotNull Status status,
        @NotNull Set<Integer> sel,
        @NotNull String msg
    ) {
        return new VoteResult(pollId, status, sel, msg);
    }

   public enum Status {
       ACCEPTED,
       REJECTED_CLOSED,
       REJECTED_RULES,
       NO_OP,
       ROLLBACK
   }

}

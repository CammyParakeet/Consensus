package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.domain.VoteResult;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Central manager handling votes within a poll
 *
 * @author Cammy
 */
public interface VoteManager {

    /**
     * Casts or toggles a vote for a specific option index depending on poll rules
     * <li>Single-choice: sets the chosen option (resubmission rules apply)</li>
     * <li>Multi-choice: toggles the chosen option (maxSelections enforced)</li>
     *
     * @return a result describing outcome and recommended UI action
     */
    CompletableFuture<VoteResult> attemptVote(
        @NotNull UUID pollId,
        @NotNull UUID voterId,
        int optionIndex
    );



}

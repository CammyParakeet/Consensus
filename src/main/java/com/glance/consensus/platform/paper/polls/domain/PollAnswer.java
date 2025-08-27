package com.glance.consensus.platform.paper.polls.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Value object representing a single poll option/answer
 *
 * <p>Fields:</p>
 * <ul>
 *   <li><b>index</b> - Zero-based position within the poll</li>
 *   <li><b>labelRaw</b> - Display label in raw MiniMessage format</li>
 *   <li><b>votes</b> - Total recorded votes for this option</li>
 * </ul>
 *
 * <p>Immutability:</p>
 * <ul>
 *   <li>Instances are immutable; use the provided {@link #withVotes(int)} or {@link #withAddedVotes(int)} to derive
 *       updated copies</li>
 * </ul>
 *
 * @author Cammy
 */
public record PollAnswer(
    int index,
    @NotNull String labelRaw,
    @Nullable String tooltipRaw,
    int votes
) {
    /** Returns a copy with an absolute vote count */
    public PollAnswer withVotes(int newVotes) {
        return new PollAnswer(index, labelRaw, tooltipRaw, newVotes);
    }
    /** Returns a copy with {@code addedVotes} added to the current count */
    public PollAnswer withAddedVotes(int addedVotes) {
        return new PollAnswer(index, labelRaw, tooltipRaw, this.votes + addedVotes);
    }

}

package com.glance.consensus.platform.paper.polls.domain;

/**
 * Value object representing a single poll option
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
public record PollOption(int index, String labelRaw, int votes) {
    /** Returns a copy with an absolute vote count */
    public PollOption withVotes(int newVotes) {
        return new PollOption(index, labelRaw, newVotes);
    }
    /** Returns a copy with {@code addedVotes} added to the current count */
    public PollOption withAddedVotes(int addedVotes) {
        return new PollOption(index, labelRaw, this.votes + addedVotes);
    }
}

package com.glance.consensus.platform.paper.polls.domain;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single poll
 * <p>Fields:</p>
 * <ul>
 *     <li><b>id</b> - Unique id for this poll (UUID)</li>
 *     <li><b>questionRaw</b> - The poll question in raw MiniMessage/placeholder format</li>
 *     <li><b>createdAt</b> - Timestamp the poll was created</li>
 *     <li><b>closesAt</b> - Timestamp the poll is scheduled to close (can be closed early)</li>
 *     <li><b>options</b> - List of {@code PollOption}s capturing label & vote counts</li>
 *     <li><b>closed</b> - If true, the poll no longer accepts votes, and is treated as completed</li>
 * </ul>
 *
 * @author Cammy
 */
@Data
public final class Poll {

    private final UUID id;
    private final UUID owner;
    private final String questionRaw; // MiniMessage supported
    private final Instant createdAt;
    private final Instant closesAt;
    private final List<PollAnswer> options;
    private final PollRules rules;
    private volatile boolean closed = false;

}

package com.glance.consensus.platform.paper.polls.domain;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single poll
 * <p>Fields:</p>
 * <ul>
 *     <li><b>id</b> - Unique id for this poll (UUID)</li>
 *     <li><b>pollIdentifier</b> - Optional readable ID for this poll - defaults to the UUID id</li>
 *     <li><b>owner</b> - Unique id of the owner for this poll (UUID)</li>
 *     <li><b>questionRaw</b> - The poll question in raw MiniMessage/placeholder format</li>
 *     <li><b>createdAt</b> - Timestamp the poll was created</li>
 *     <li><b>closesAt</b> - Timestamp the poll is scheduled to close (can be closed early)</li>
 *     <li><b>closedAt</b> - Timestamp the poll is actually closed (closed polls only)</li>
 *     <li><b>options</b> - List of {@code PollOption}s capturing label & vote counts</li>
 *     <li><b>closed</b> - If true, the poll no longer accepts votes, and is treated as completed</li>
 * </ul>
 *
 * @author Cammy
 */
@Data
public final class Poll {

    private final @NotNull UUID id;
    private final @NotNull String pollIdentifier;
    private final @NotNull UUID owner;
    private final @NotNull String questionRaw; // MiniMessage supported
    private final @NotNull Instant createdAt;
    private final @NotNull Instant closesAt;
    private final @Nullable Instant closedAt;
    private final @NotNull List<PollOption> options;
    private final @NotNull PollRules rules;
    private volatile boolean closed = false;

    @Override
    public String toString() {
        return "Poll{" +
                "id=" + id +
                ", owner=" + owner +
                ", question='" + questionRaw + '\'' +
                ", createdAt=" + createdAt +
                ", closesAt=" + closesAt +
                ", options=" + options.size() +
                ", rules=" + rules +
                ", closed=" + closed +
                '}';
    }

}

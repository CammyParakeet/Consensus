package com.glance.consensus.platform.paper.polls.persistence;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence boundary for the Polls system
 *
 * <p>Design principles:</p>
 * <ul>
 *   <li><b>Selections-first:</b> Per-voter selections are the source of truth; tallies are derived</li>
 *   <li><b>Async API:</b> All methods are asynchronous and non-blocking from the caller's perspective</li>
 *   <li><b>Backend-agnostic:</b> Implementations may be FlatFile (JSON) or SQL (Jdbi), chosen by configuration</li>
 * </ul>
 *
 * <p>Consistency model:</p>
 * <ul>
 *   <li>Each write method is atomic with respect to its key (e.g, a single voter’s selection set for a poll)</li>
 *   <li>Aggregated tallies returned by this API reflect durable storage at the time of completion</li>
 *   <li>Callers may optimistically update in-memory counts, but should prefer tallies returned from persistence
 *       to avoid drift after restarts</li>
 * </ul>
 *
 * @author Cammy
 */
public interface PollStorage {

    /**
     * Creates or upserts a poll definition and its answers
     * <p>No per-answer vote totals are stored here; those are derived from selections</p>
     *
     * @param poll the poll aggregate to persist (answers {@code votes} ignored by storage)
     * @return future that completes when the poll has been durably written
     */
    CompletableFuture<Void> createPoll(@NotNull Poll poll);

    /**
     * Loads a single poll aggregate (without derived tallies)
     *
     * @param pollId target poll id
     * @return future containing the poll if present, otherwise empty
     */
    CompletableFuture<Optional<Poll>> loadPoll(@NotNull UUID pollId);

    /**
     * Loads all currently active (open) polls
     *
     * @return future with list of active polls
     */
    CompletableFuture<List<Poll>> loadActivePolls();

    /**
     * Loads polls that should be visible in runtime
     * <li>all active</li>
     * <li>closed polls within the retention window</li>
     *
     * @param retention how long to keep closed polls visible
     * @return future with active + recently-closed polls
     */
    CompletableFuture<List<Poll>> loadRecentPolls(@NotNull Duration retention);

    default CompletableFuture<List<Poll>> loadAllPolls() {
        return loadRecentPolls(Duration.ofDays(-1));
    }

    /**
     * Marks a poll as closed and records the closure time
     *
     * @param pollId poll to close
     * @param closedAt timestamp to record
     * @return future completing when state is updated
     */
    CompletableFuture<Void> closePoll(@NotNull UUID pollId, @NotNull Instant closedAt);

    // TODO: direct answer and voting queries

    /**
     * Deletes a poll and its associated selections
     *
     * @param pollId poll id to delete
     * @return future that completes after deletion
     */
    CompletableFuture<Void> deletePoll(@NotNull UUID pollId);

}

package com.glance.consensus.platform.paper.polls.persistence;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
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

    /* Poll Specific */

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

    /**
     * Deletes a poll and its associated selections
     *
     * @param pollId poll id to delete
     * @return future that completes after deletion
     */
    CompletableFuture<Void> deletePoll(@NotNull UUID pollId);

    /* Voter Specific */

    /**
     * Persists (creates or replaces) the full selection set for a single voter
     *
     * <p>Selections are stored atomically per (poll, voter). Any existing selection
     * is overwritten. Implementations must ensure that the write is durable before
     * completing the returned future</p>
     *
     * @param pollId poll id
     * @param voterId voter making the selection
     * @param indices indices of chosen options (maybe empty to clear vote)
     * @return future completing when the selection has been stored
     */
    CompletableFuture<Void> saveVoterSelection(
        @NotNull UUID pollId,
        @NotNull UUID voterId,
        @NotNull Set<Integer> indices
    );

    /**
     * Removes a voter's selection from a poll
     *
     * <p>This is equivalent to {@link #saveVoterSelection(UUID, UUID, Set)} with
     * an empty set, but is provided as a convenience for administrative rollback
     * or cleanup scenarios</p>
     *
     * @param pollId poll id
     * @param voterId voter whose selection should be removed
     * @return future completing when the removal has been durably written
     */
    default CompletableFuture<Void> deleteVoterSelection(
        @NotNull UUID pollId,
        @NotNull UUID voterId
    ) {
        return null;
    }

    /**
     * Loads the stored selection set for a single voter in a poll
     *
     * <p>Selections are returned as the set of option indices chosen by the voter.
     * If the voter has not yet voted, the result will be an empty set</p>
     *
     * @param pollId poll id
     * @param voterId voter whose selection should be loaded
     * @return future completing with the set of chosen option indices (possibly empty)
     */
    default CompletableFuture<Set<Integer>> loadVoterSelection(
        @NotNull UUID pollId,
        @NotNull UUID voterId
    ) {
        return null;
    }

    /**
     * Loads all voters who currently have a stored selection for the given poll
     *
     * <p>The returned set contains only voter UUIDs; to inspect individual choices,
     * see {@link #loadAllSelections(UUID)}</p>
     *
     * @param pollId poll id
     * @return future completing with the set of voter UUIDs
     */
     default CompletableFuture<Set<UUID>> loadVoters(@NotNull UUID pollId) {
         return null;
     }

    /**
     * Loads the complete set of stored selections for a poll
     *
     * <p>Returns a mapping of voter UUID -> set of option indices chosen
     *
     * @param pollId poll id
     * @return future completing with a map of voter ids to their chosen option indices
     */
    CompletableFuture<Map<UUID, Set<Integer>>> loadAllSelections(@NotNull UUID pollId);

}

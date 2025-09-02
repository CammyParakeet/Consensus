package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.display.PollDisplay;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime wrapper around a {@link Poll} that stores live per-voter selections
 * and maintains tallies for fast UI reads
 * <p>
 * Thread-safe: vote and close operations are synchronized
 *
 * @author Cammy
 */
@Slf4j
@RequiredArgsConstructor
public final class PollRuntime {

    /** Backing poll definition (mutable option vote counts, closed flag) */
    @Getter
    private final Poll poll;

    /** Display mode in use (kept for your current architecture) */
    @Getter
    private final PollDisplay.Mode mode = PollDisplay.Mode.BOOK;

    /** voterId -> indices chosen (complete set per voter) */
    private final Map<UUID, Set<Integer>> votes = new ConcurrentHashMap<>();

    /* ------------ Snapshots ------------ */

    public synchronized boolean hasVoted(@NotNull UUID voter) {
        var s = votes.get(voter);
        return s != null && !s.isEmpty();
    }

    public synchronized Set<UUID> votersSnapshot() {
        return Set.copyOf(votes.keySet());
    }

    public synchronized Set<Integer> selectionSnapshot(@NotNull UUID voter) {
        var s = votes.get(voter);
        return (s == null) ? Set.of() : Set.copyOf(s);
    }

    /* ------------ Mutations owned by VoteManager ------------ */

    /**
     * Applies (or clears) the full selection set for a voter and updates tallies.
     * <p>
     * This method does <b>not</b> enforce any rules—callers must ensure validity.
     *
     * @param voter voter id
     * @param newSelection new selection set; empty to clear
     * @return true if state changed; false if no change
     */
    public synchronized boolean applySelection(
            @NotNull UUID voter,
            @NotNull Set<Integer> newSelection
    ) {
        final Set<Integer> before = votes.getOrDefault(voter, Set.of());
        if (before.equals(newSelection)) return false;

        // Copy to stable sets for delta calc
        final Set<Integer> beforeCopy = new HashSet<>(before);
        final Set<Integer> afterCopy  = new HashSet<>(newSelection);

        if (afterCopy.isEmpty()) {
            votes.remove(voter);
        } else {
            // store a defensive copy
            votes.put(voter, Set.copyOf(afterCopy));
        }

        applyTalliesDelta(beforeCopy, afterCopy);
        return true;
    }

    /**
     * Seeds runtime with previously persisted selections for a voter
     */
    public synchronized void supplySelectionBootstrap(
            @NotNull UUID voter,
            @NotNull Collection<Integer> indices
    ) {
        applySelection(voter, Set.copyOf(new HashSet<>(indices)));
    }

    /** Marks this poll as closed */
    public synchronized void close() {
        poll.setClosed(true);
    }

    /* ------------ Internals ------------ */

    /**
     * Incrementally updates tallies given a single voters transition
     */
    private synchronized void applyTalliesDelta(
            @NotNull Set<Integer> before,
            @NotNull Set<Integer> after
    ) {
        if (before.equals(after)) return;

        final int optCount = poll.getOptions().size();
        // Build a working array from current PollOption votes
        int[] counts = new int[optCount];
        for (int i = 0; i < optCount; i++) counts[i] = poll.getOptions().get(i).votes();

        // Compute delta: remove vanished, add new
        for (int i : before) if (!after.contains(i) && inBounds(i, optCount)) counts[i] = Math.max(0, counts[i] - 1);
        for (int i : after)  if (!before.contains(i) && inBounds(i, optCount)) counts[i] = counts[i] + 1;

        writeTallies(counts);
    }

    private synchronized void writeTallies(int[] counts) {
        final int optCount = poll.getOptions().size();
        for (int i = 0; i < optCount; i++) {
            var option = poll.getOptions().get(i);
            poll.getOptions().set(i, new PollOption(
                    option.index(), option.labelRaw(), option.tooltipRaw(), counts[i]
            ));
        }
    }

    private static boolean inBounds(int idx, int size) {
        return idx >= 0 && idx < size;
    }

}

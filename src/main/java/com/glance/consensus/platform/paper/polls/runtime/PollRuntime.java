package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime wrapper around a {@link Poll}, handling live votes and tallies
 * <p>
 * Stores per-voter selections, enforces {@link PollRules}, and updates
 * {@link PollOption} vote counts in-place on each vote
 * <p>
 * Thread-safe: vote and close operations are synchronized
 *
 * @author Cammy
 */
@RequiredArgsConstructor
public final class PollRuntime {

    /** The backing poll definition (mutable vote counts, closed flag) */
    @Getter
    private final Poll poll;

    // voterId -> indices chosen
    private final Map<UUID, Set<Integer>> votes = new ConcurrentHashMap<>();

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

    public synchronized void supplyVotes(
        @NotNull UUID voter,
        @NotNull Collection<Integer> indices
    ) {
        if (indices.isEmpty()) return;
        votes.put(voter, new HashSet<>(indices));

        recomputeTallies();
    }

    /**
     * Records a vote for the given voter
     *
     * @param voter voter id
     * @param voteIndices indices of selected options
     * @return true if vote accepted, false if rejected by validation/rules
     */
    public synchronized boolean vote(
        @NotNull UUID voter,
        Collection<Integer> voteIndices
    ) {
        if (poll.isClosed()) return false;
        if (voteIndices.isEmpty()) return false;

        var optCount = poll.getOptions().size();
        // validate votes
        for (int i : voteIndices) if (i < 0 || i >= optCount) return false;

        var rules = poll.getRules();
        if (!rules.multipleChoice()) {
            int chosen = voteIndices.iterator().next();
            Set<Integer> existing = votes.get(voter);
            if (existing != null && !existing.isEmpty()) {
                if (!rules.allowResubmissions()) return false;
                existing.clear();
                existing.add(chosen);
            } else {
                votes.put(voter, new HashSet<>(Set.of(chosen)));
            }
        } else {
            if (voteIndices.size() > rules.maxSelections()) return false;
            Set<Integer> set = new HashSet<>(voteIndices);
            if (set.size() != voteIndices.size()) return false;
            votes.put(voter, set);
        }

        recomputeTallies();
        return true;
    }

    private void recomputeTallies() {
        final int optCount = poll.getOptions().size();
        int[] counts = new int[optCount];
        for (Set<Integer> s : votes.values()) for (int i : s) counts[i]++;

        for (int i = 0; i < optCount; i++) {
            var option = poll.getOptions().get(i);
            poll.getOptions().set(i, new PollOption(
                    option.index(), option.labelRaw(), option.tooltipRaw(), counts[i]));
        }
    }

    /**
     * Marks this poll as closed, preventing new votes
     */
    public synchronized void close() {
        poll.setClosed(true);
    }

}

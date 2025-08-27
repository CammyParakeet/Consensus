package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollOption;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public final class PollRuntime {

    @Getter
    private final Poll poll;

    // voterId -> indices chosen
    private final Map<UUID, Set<Integer>> votes = new ConcurrentHashMap<>();

    public synchronized boolean vote(
        @NotNull UUID voter,
        Collection<Integer> voteIndices,
        boolean replaceIfSingle
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

        int[] counts = new int[optCount];
        for (Set<Integer> s : votes.values()) for (int i : s) counts[i]++;
        for (int i = 0; i < optCount; i++) {
            var option = poll.getOptions().get(i);
            poll.getOptions().set(i, new PollOption(
                    option.index(), option.labelRaw(), option.tooltipRaw(), counts[i]));
        }
        return true;
    }

    public synchronized void close() {
        poll.setClosed(true);
    }

}

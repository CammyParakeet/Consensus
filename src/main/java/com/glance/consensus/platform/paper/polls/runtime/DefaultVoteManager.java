package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.domain.VoteResult;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DefaultVoteManager implements VoteManager {

    private final Plugin plugin;
    private final PollManager pollManager;
    private final Provider<PollStorage> storageProvider;

    public DefaultVoteManager(
        @NotNull final Plugin plugin,
        @NotNull final PollManager pollManager,
        @NotNull final Provider<PollStorage> storageProvider
    ) {
        this.plugin = plugin;
        this.pollManager = pollManager;
        this.storageProvider = storageProvider;
    }

    @Override
    public CompletableFuture<VoteResult> attemptVote(
        @NotNull UUID pollId,
        @NotNull UUID voterId,
        int optionIdx
    ) {
        var rtOpt = pollManager.get(pollId);
        if (rtOpt.isEmpty()) {
            return CompletableFuture.completedFuture(
                VoteResult.of(VoteResult.Status.NOT_FOUND, pollId, Set.of(),
                    "<red>Poll not found</red>", VoteResult.NextAction.NONE));
        }

        var rt = rtOpt.get();
        var poll = rt.getPoll();
        var options = poll.getOptions();

        if (poll.isClosed()) {
            return CompletableFuture.completedFuture(
                VoteResult.of(VoteResult.Status.CLOSED, pollId, rt.selectionSnapshot(voterId),
                        "<red>Poll is no longer accepting votes!</red>", VoteResult.NextAction.NONE));
        }

        if (optionIdx < 0 || optionIdx >= options.size()) {
            return CompletableFuture.completedFuture(
                VoteResult.of(VoteResult.Status.INVALID_OPTION, pollId, rt.selectionSnapshot(voterId),
                        "<red>Invalid poll option</red>", VoteResult.NextAction.NONE));
        }

        var rules = poll.getRules();
        var current = rt.selectionSnapshot(voterId);
        var next = new HashSet<>(current);

        if (!rules.multipleChoice()) {
            boolean alreadyVoted = current.size() == 1 && current.contains(optionIdx);
            if (alreadyVoted && !rules.allowResubmissions()) {
                // no more voting allowed
                return CompletableFuture.completedFuture(
                    VoteResult.of(VoteResult.Status.REJECTED, pollId, current,
                            "<gold>You've already voted for that option</gold>",
                            VoteResult.NextAction.OPEN_RESULTS));
            }

            // resubmission
            next.clear();
            next.add(optionIdx);
        } else {
            if (next.contains(optionIdx)) {
                next.remove(optionIdx);
            } else {
                if (next.size() >= rules.maxSelections()) {
                    return CompletableFuture.completedFuture(
                        VoteResult.of(VoteResult.Status.REJECTED, pollId, current,
                            "<red>You can select up to " + rules.maxSelections() + " options</red>",
                                VoteResult.NextAction.REFRESH_DISPLAY));
                }
                next.add(optionIdx);
            }
        }

        boolean accepted = rt.vote(voterId, next);

        return CompletableFuture.completedFuture(null);
    }

}

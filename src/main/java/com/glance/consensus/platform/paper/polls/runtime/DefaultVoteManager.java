package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.domain.PollRules;
import com.glance.consensus.platform.paper.polls.domain.VoteResult;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.utils.RuleUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class DefaultVoteManager implements VoteManager {

    private final @NotNull Provider<PollStorage> storageProvider;

    @Inject
    public DefaultVoteManager(
        @NotNull Provider<PollStorage> storageProvider
    ) {
        this.storageProvider = storageProvider;
    }

    /** in-flight guard: pollId -> set of playerIds currently persisting a selection */
    private final Map<UUID, Set<UUID>> inFlight = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<VoteResult> attemptVote(
            @NotNull Player player,
            @NotNull PollRuntime runtime,
            int optionIndex
    ) {
        final Poll poll = runtime.getPoll();
        final UUID pollId = poll.getId();
        final UUID playerId = player.getUniqueId();

        // Serialize attempts per (poll, player)
        if (!acquireInFlight(pollId, playerId)) {
            return CompletableFuture.completedFuture(new VoteResult(
                    pollId,
                    VoteResult.Status.NO_OP,
                    runtime.selectionSnapshot(playerId),
                    "<gray>Still saving your previous click...</gray>"
            ));
        }

        // Fast validations
        if (poll.isClosed()) {
            releaseInFlight(pollId, playerId);
            return CompletableFuture.completedFuture(new VoteResult(
                    pollId,
                    VoteResult.Status.REJECTED_CLOSED,
                    runtime.selectionSnapshot(playerId),
                    "<red>This poll is closed</red>"
            ));
        }

        if (optionIndex < 0 || optionIndex >= poll.getOptions().size()) {
            releaseInFlight(pollId, playerId);
            return CompletableFuture.completedFuture(new VoteResult(
                    pollId,
                    VoteResult.Status.REJECTED_RULES,
                    runtime.selectionSnapshot(playerId),
                    "<red>Invalid option</red>"
            ));
        }

        // Compute proposed selection based on rules
        final PollRules effective = RuleUtils.effectiveRules(player, poll.getRules());
        final Set<Integer> before = runtime.selectionSnapshot(playerId);
        final Set<Integer> proposed = computeNewSelectionSet(before, optionIndex, effective);

        // Resubmission policy (only applies if change is real)
        if (!effective.allowResubmissions() && !before.isEmpty() && !before.equals(proposed)) {
            releaseInFlight(pollId, playerId);
            return CompletableFuture.completedFuture(new VoteResult(
                    pollId,
                    VoteResult.Status.REJECTED_RULES,
                    before,
                    "<red>You already voted. Resubmissions are disabled</red>"
            ));
        }

        // Max selections
        if (effective.multipleChoice()) {
            final int max = Math.max(1, effective.maxSelections());
            if (proposed.size() > max) {
                releaseInFlight(pollId, playerId);
                return CompletableFuture.completedFuture(new VoteResult(
                        pollId,
                        VoteResult.Status.REJECTED_RULES,
                        before,
                        "<red>You can select at most " + max + " option" + (max == 1 ? "" : "s") + "</red>"
                ));
            }
        }

        if (before.equals(proposed)) {
            releaseInFlight(pollId, playerId);
            return CompletableFuture.completedFuture(new VoteResult(
                    pollId,
                    VoteResult.Status.NO_OP,
                    before,
                    "<gray>No change</gray>"
            ));
        }

        // Optimistic apply to runtime
        runtime.applySelection(playerId, proposed);

        // Persist atomically
        return storageProvider.get()
                .saveVoterSelection(pollId, playerId, proposed)
                .thenApply(v -> new VoteResult(
                        pollId,
                        VoteResult.Status.ACCEPTED,
                        proposed,
                        successMessage(effective, before, proposed)
                ))
                .exceptionally(ex -> {
                    // Transactional Rollback optimistic change
                    runtime.applySelection(playerId, before);
                    return new VoteResult(
                            pollId,
                            VoteResult.Status.ROLLBACK,
                            before,
                            "<red>Couldn’t save your vote. Please try again</red>"
                    );
                })
                .whenComplete((r, t) -> releaseInFlight(pollId, playerId));
    }

    /* -------------------- helpers -------------------- */

    private static @NotNull Set<Integer> computeNewSelectionSet(
            @NotNull Set<Integer> before,
            int clickedIndex,
            @NotNull PollRules rules
    ) {
        if (!rules.multipleChoice()) {
            // single-choice: always replace with the clicked option
            return Set.of(clickedIndex);
        }

        // multiple-choice: toggle clicked index
        final Set<Integer> next = new HashSet<>(before);
        if (next.contains(clickedIndex)) next.remove(clickedIndex);
        else next.add(clickedIndex);

        return Collections.unmodifiableSet(next);
    }

    private static String successMessage(PollRules rules, Set<Integer> before, Set<Integer> after) {
        if (rules.multipleChoice()) {
            return "<green>Selection updated.</green>";
        }
        if (before.isEmpty()) return "<green>Vote recorded.</green>";
        if (before.equals(after)) return "<gray>No change.</gray>";
        return "<green>Vote changed.</green>";
    }

    private boolean acquireInFlight(UUID pollId, UUID playerId) {
        inFlight.computeIfAbsent(pollId, k -> ConcurrentHashMap.newKeySet());
        return inFlight.get(pollId).add(playerId);
    }

    private void releaseInFlight(UUID pollId, UUID playerId) {
        final Set<UUID> set = inFlight.get(pollId);
        if (set == null) return;
        set.remove(playerId);
        if (set.isEmpty()) inFlight.remove(pollId);
    }

}

package com.glance.consensus.platform.paper.polls.persistence.sql;

import com.glance.consensus.platform.paper.polls.domain.Poll;
import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Singleton
public class JdbiPollStorage implements PollStorage {

    @Inject
    public JdbiPollStorage(@NotNull Plugin plugin) {

    }

    @Override
    public CompletableFuture<Void> createPoll(@NotNull Poll poll) {
        return null;
    }

    @Override
    public CompletableFuture<Optional<Poll>> loadPoll(@NotNull UUID pollId) {
        return null;
    }

    @Override
    public CompletableFuture<List<Poll>> loadActivePolls() {
        return null;
    }

    @Override
    public CompletableFuture<List<Poll>> loadRecentPolls(@NotNull Duration retention) {
        return null;
    }

    @Override
    public CompletableFuture<Void> closePoll(@NotNull UUID pollId, @NotNull Instant closedAt) {
        return null;
    }

    @Override
    public CompletableFuture<Void> deletePoll(@NotNull UUID pollId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> saveVoterSelection(@NotNull UUID pollId, @NotNull UUID voterId, @NotNull Set<Integer> indices) {
        return null;
    }

    @Override
    public CompletableFuture<Void> deleteVoterSelection(@NotNull UUID pollId, @NotNull UUID voterId) {
        return null;
    }

    @Override
    public CompletableFuture<Set<Integer>> loadVoterSelection(@NotNull UUID pollId, @NotNull UUID voterId) {
        return null;
    }

    @Override
    public CompletableFuture<Set<UUID>> loadVoters(@NotNull UUID pollId) {
        return null;
    }

    @Override
    public CompletableFuture<Map<UUID, Set<Integer>>> loadAllSelections(@NotNull UUID pollId) {
        return null;
    }

}

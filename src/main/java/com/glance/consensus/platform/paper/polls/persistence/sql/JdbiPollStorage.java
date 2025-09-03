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

    private final Plugin plugin;
    private final SqlBootstrap sql;

    @Inject
    public JdbiPollStorage(
        @NotNull final Plugin plugin,
        @NotNull final SqlBootstrap sql
    ) {
        this.plugin = plugin;
        this.sql = sql;

        if (sql.dialect() == SqlBootstrap.Dialect.SQLITE) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("""
                        SQLite driver not found. Options:
                          - Use the '-with-sqlite' jar of the plugin, or
                          - Install a dedicated SQLite driver plugin, or
                          - Switch backend to FLATFILE in config.
                        """);
                throw new RuntimeException(e);
            }
        }

        // TODO

        plugin.getLogger().info("JDBI poll storage initialised (" + sql.dialect() + ")");
    }

    /* ---- Helpers ---- */

    /* ---- Poll API ---- */

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

    /* ---- Vote API ---- */

    @Override
    public CompletableFuture<Void> saveVoterSelection(@NotNull UUID pollId, @NotNull UUID voterId, @NotNull Set<Integer> indices) {
        return null;
    }

    @Override
    public CompletableFuture<Map<UUID, Set<Integer>>> loadAllSelections(@NotNull UUID pollId) {
        return null;
    }

}

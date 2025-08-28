package com.glance.consensus.platform.paper.polls.persistence.config;

import com.glance.consensus.platform.paper.polls.persistence.PollStorage;
import com.glance.consensus.platform.paper.polls.persistence.file.FlatFilePollStorage;
import com.glance.consensus.platform.paper.polls.persistence.sql.JdbiPollStorage;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * DI provider that selects a {@link PollStorage} implementation at runtime
 * based on Bukkit configuration
 *
 * <p>Selection logic:</p>
 * <ul>
 *   <li>{@code FLATFILE} -> {@link FlatFilePollStorage}</li>
 *   <li>{@code SQLITE}/{@code MYSQL} -> {@link JdbiPollStorage} if driver present, else fallback to flatfile</li>
 * </ul>
 *
 * <p>Driver checks:</p>
 * <ul>
 *   <li>SQLite: {@code org.sqlite.JDBC}</li>
 *   <li>MySQL: {@code com.mysql.cj.jdbc.Driver} (NYI)</li>
 * </ul>
 *
 * <p>Caches the selected instance for subsequent calls to {@link #get()}</p>
 */
@Singleton
public final class PollStorageProvider implements Provider<PollStorage> {

    private final Plugin plugin;
    private final PollStorageConfig cfg;
    private final Provider<FlatFilePollStorage> flat;
    private final Provider<JdbiPollStorage> sql;

    private volatile PollStorage cached;

    @Inject
    public PollStorageProvider(
        @NotNull final Plugin plugin,
        @NotNull final PollStorageConfig cfg,
        @NotNull final Provider<FlatFilePollStorage> flat,
        @NotNull final Provider<JdbiPollStorage> sql
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.flat = flat;
        this.sql = sql;
    }

    @Override
    public PollStorage get() {
        PollStorage c = cached;
        if (c != null) return c;

        PollStorageConfig.Backend be = cfg.getBackend();
        if (be == PollStorageConfig.Backend.FLATFILE) {
            plugin.getLogger().info("Using FlatFile poll storage (JSON)");
            return cached = flat.get();
        }

        if (be == PollStorageConfig.Backend.SQLITE && !classPresent("org.sqlite.JDBC")) {
            plugin.getLogger().warning("SQLite driver missing. Falling back to FlatFile.");
            return cached = flat.get();
        }
        if (be == PollStorageConfig.Backend.MYSQL && !classPresent("com.mysql.cj.jdbc.Driver")) {
            plugin.getLogger().warning("MySQL driver missing. Falling back to FlatFile.");
            return cached = flat.get();
        }

        plugin.getLogger().info("Using SQL poll storage via JDBI (" + be + ")");
        return cached = sql.get();
    }

    private boolean classPresent(String n) {
        try { Class.forName(n); return true; } catch (ClassNotFoundException e) { return false; }
    }

}

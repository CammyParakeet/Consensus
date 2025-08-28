package com.glance.consensus.platform.paper.polls.persistence.config;

import com.google.inject.Inject;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

/**
 * Thin loader for the "storage" section of Bukkit {@code config.yml}
 *
 * <p>Example:</p>
 * <pre>
 * storage:
 *   backend: FLATFILE  # FLATFILE | SQLITE | MYSQL (nyi)
 *   flatfile:
 *     dir: playerdata/polls
 *   sql:
 *     jdbcUrl: "jdbc:sqlite:${plugin.data}/polls.db"
 *     username: ""
 *     password: ""
 *     pool:
 *       maxSize: 6
 *       minIdle: 2
 * </pre>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>{@code ${plugin.data}} is expanded to {@code plugin.getDataFolder().getAbsolutePath()}</li>
 *   <li>Reasonable defaults applied if keys are missing</li>
 * </ul>
 *
 * @author Cammy
 */
@Getter
public final class PollStorageConfig {

    public enum Backend { FLATFILE, SQLITE, MYSQL }

    private final Backend backend;
    private final String flatFileDir;
    private final String jdbcURL;
    private final String username;
    private final String password;
    private final int maxPool;
    private final int minIdle;

    @Inject
    public PollStorageConfig(@NotNull final Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection s = config.getConfigurationSection("storage");
        if (s == null) {
            this.backend = Backend.FLATFILE;
            this.flatFileDir = "playerdata/polls";
            this.jdbcURL = "";
            this.username = "";
            this.password = "";
            this.maxPool = 6;
            this.minIdle = 2;
        } else {
            this.backend = Backend.valueOf(s.getString("backend", "FLATFILE").toUpperCase(Locale.ROOT));
            this.flatFileDir = s.getString("flatfile.dir", "playerdata/polls");

            String rawUrl = s.getString("sql.jdbcUrl", "jdbc:sqlite:" +
                    new File(plugin.getDataFolder(), "polls.db").getAbsolutePath());
            this.jdbcURL = rawUrl.replace("${plugin.data}", plugin.getDataFolder().getAbsolutePath());

            this.username = s.getString("sql.username", "");
            this.password = s.getString("sql.password", "");

            this.maxPool = s.getInt("sql.pool.maxSize", 6);
            this.minIdle = s.getInt("sql.pool.minIdle", 2);
        }
    }

}

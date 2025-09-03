package com.glance.consensus.platform.paper.polls.persistence.sql;

import com.glance.consensus.platform.paper.polls.persistence.config.PollStorageConfig;
import com.glance.consensus.platform.paper.polls.persistence.config.PollStorageProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.plugin.Plugin;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;

@Singleton
@Getter
@Accessors(fluent = true)
public class SqlBootstrap {

    private final DataSource dataSource;
    private final Jdbi jdbi;
    private final Dialect dialect;

    public enum Dialect { SQLITE, MYSQL, MARIADB }

    @Inject
    public SqlBootstrap(
            @NotNull Plugin plugin,
            @NotNull PollStorageConfig cfg
    ) {
        String url = cfg.getJdbcURL();

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);

        if (!cfg.getUsername().isEmpty()) hc.setUsername(cfg.getUsername());
        if (!cfg.getPassword().isEmpty()) hc.setPassword(cfg.getPassword());

        hc.setMaximumPoolSize(cfg.getMaxPool());
        hc.setMinimumIdle(cfg.getMinIdle());

        this.dataSource = new HikariDataSource(hc);

        this.jdbi = Jdbi.create(this.dataSource)
                .installPlugins()
                .installPlugin(new SqlObjectPlugin());

        this.dialect = url.startsWith("jdbc:sqlite") ? Dialect.SQLITE : Dialect.MYSQL;

        // Create schema once
        if (dialect == Dialect.SQLITE) {
            jdbi.useExtension(SqlitePollDao.class, dao -> {
                // todo
            });
        } else {
            throw new UnsupportedOperationException();
        }
    }

}

package com.glance.consensus.platform.paper.polls.builder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.glance.consensus.platform.paper.module.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages ephemeral {@link PollBuildSession} objects for players
 * <p>
 * Ensures each player has at most one active session at a time
 */
@Singleton
@AutoService(Manager.class)
public final class PollBuilderSessions implements Manager {

    private final Cache<UUID, PollBuildSession> sessions;

    @Inject
    public PollBuilderSessions() {
        this.sessions = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
                .maximumSize(1000)
                .build();
    }

    /**
     * Gets an existing build session for a player, or creates one if absent
     *
     * @param playerId the player id
     * @return the session
     */
    public PollBuildSession getOrCreate(@NotNull UUID playerId) {
        return sessions.get(playerId, PollBuildSession::new);
    }

    /**
     * Gets the current session for a player if present
     *
     * @param playerId the player id
     * @return optional session
     */
    public Optional<PollBuildSession> get(@NotNull UUID playerId) {
        return Optional.ofNullable(sessions.getIfPresent(playerId));
    }

    /**
     * Clears a player's build session immediately
     *
     * @param playerId the player id
     */
    public void clear(@NotNull UUID playerId) {
        sessions.invalidate(playerId);
    }

    /**
     * Checks if a player currently has a build session
     *
     * @param playerId the player id
     * @return true if present
     */
    public boolean has(@NotNull UUID playerId) {
        return sessions.getIfPresent(playerId) != null;
    }

    @Override
    public void onDisable() {
        this.sessions.invalidateAll();
        this.sessions.cleanUp();
    }

}

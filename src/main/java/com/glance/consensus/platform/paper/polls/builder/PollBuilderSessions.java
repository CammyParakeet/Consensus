package com.glance.consensus.platform.paper.polls.builder;

import com.glance.consensus.platform.paper.module.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@AutoService(Manager.class)
public final class PollBuilderSessions implements Manager {

    private final Map<UUID, PollBuildSession> sessions = new ConcurrentHashMap<>();

    public PollBuildSession getOrCreate(@NotNull UUID playerId) {
        return sessions.computeIfAbsent(playerId, PollBuildSession::new);
    }

    public Optional<PollBuildSession> get(@NotNull UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void clear(@NotNull UUID playerId) {
        sessions.remove(playerId);
    }

    public boolean has(@NotNull UUID playerId) {
        return sessions.containsKey(playerId);
    }

    @Override
    public void onDisable() {
        this.sessions.clear();
    }

}

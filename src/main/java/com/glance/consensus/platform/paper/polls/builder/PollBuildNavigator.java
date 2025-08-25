package com.glance.consensus.platform.paper.polls.builder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PollBuildNavigator {

    void open(
        @NotNull Player player,
        @NotNull PollBuildSession.Stage stage
    );

    void clear(@NotNull UUID playerId);

}

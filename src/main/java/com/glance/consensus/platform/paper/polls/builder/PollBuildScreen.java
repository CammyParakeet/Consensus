package com.glance.consensus.platform.paper.polls.builder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PollBuildScreen {
    PollBuildSession.Stage stage();
    void open(@NotNull Player player, @NotNull PollBuildSession session);
}

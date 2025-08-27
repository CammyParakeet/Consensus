package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PollManager extends Manager {

    UUID createFromBuildSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    ) throws IllegalArgumentException;

    Optional<PollRuntime> get(@NotNull UUID pollId);

    Collection<PollRuntime> active();

    boolean close(@NotNull UUID pollId);

}

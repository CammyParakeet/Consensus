package com.glance.consensus.platform.paper.polls.builder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Navigator for the poll-building wizard
 * <p>
 * Handles transitions between different {@link PollBuildSession.Stage}s
 * for a given player and manages the per-player build session lifecycle
 *
 * @author Cammy
 */
public interface PollBuildNavigator {

    /**
     * Opens the given stage of the build UI for the player
     *
     * @param player the player to show the stage to
     * @param stage  the stage to open
     */
    void open(
        @NotNull Player player,
        @NotNull PollBuildSession.Stage stage
    );

    /**
     * Clears (resets) a player's build session state
     *
     * @param playerId the id of the player
     */
    void clear(@NotNull UUID playerId);

}

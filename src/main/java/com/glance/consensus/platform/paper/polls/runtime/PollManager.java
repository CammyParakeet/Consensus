package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Central service for managing the lifecycle of active polls
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Create a new {@link Poll} from a player's in-progress {@link PollBuildSession}</li>
 *   <li>Provide access to active {@link PollRuntime}s</li>
 *   <li>Close polls early if required</li>
 * </ul>
 *
 * <p>Implementations should handle validation (e.g minimum question/options),
 * scheduling automatic closes based on duration, and updating vote tallies</p>
 *
 * @author Cammy
 */
public interface PollManager extends Manager {

    /**
     * Creates a new {@link Poll} from a completed build session
     *
     * @param creator the player who is creating the poll
     * @param session the completed build session state
     * @return the unique id of the created poll
     * @throws IllegalArgumentException if the session is invalid (e.g. missing question or options)
     */
    UUID createFromBuildSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    ) throws IllegalArgumentException;

    /**
     * Gets the runtime wrapper for a poll by its id
     *
     * @param pollId the poll id
     * @return optional runtime if the poll exists
     */
    Optional<PollRuntime> get(@NotNull UUID pollId);

    /**
     * Returns all currently active (open) polls
     *
     * @return collection of active polls
     */
    Collection<PollRuntime> active();

    /**
     * Closes a poll immediately
     *
     * @param pollId the poll id
     * @return true if closed successfully, false if not found
     */
    boolean close(@NotNull UUID pollId);

}

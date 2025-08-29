package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
     * Initialize a poll building session and start the wizard
     *
     * @param player the player who is creating the poll
     * @param suppliedId optional custom user driven identifier for the poll separate to internal ID
     */
    void startBuildSession(@NotNull Player player, @Nullable String suppliedId);

    /**
     * Creates a new {@link Poll} from a completed build session
     *
     * @param creator the player who is creating the poll
     * @param session the completed build session state
     * @return the unique id of the created poll
     * @throws IllegalArgumentException if the session is invalid (e.g. missing question or options)
     */
    CompletableFuture<UUID> createFromBuildSession(
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

    /** All known polls in memory (active + closed) */
    Collection<PollRuntime> all();

    /**
     * Returns all currently active (open) polls
     *
     * @return collection of active polls
     */
    Collection<PollRuntime> active();

    /** Convenience view of closed polls (derived from {@link #all()}) */
    default Collection<PollRuntime> closed() {
        return all().stream().filter(rt -> rt.getPoll().isClosed()).toList();
    }

    /**
     * Closes a poll immediately
     *
     * @param pollId the poll id
     * @return true if closed successfully, false if not found
     */
    boolean close(@NotNull UUID pollId);

}

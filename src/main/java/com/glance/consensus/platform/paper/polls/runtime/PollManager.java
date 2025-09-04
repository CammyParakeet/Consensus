package com.glance.consensus.platform.paper.polls.runtime;

import com.glance.consensus.platform.paper.module.Manager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildSession;
import com.glance.consensus.platform.paper.polls.domain.Poll;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
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
     * Constructs a {@link Poll} object from a build session
     *
     * @param creator The author of this poll
     * @param session The build wizard session
     * @return The built poll instance
     * @throws IllegalArgumentException if session is invalid (e.g. missing question or options)
     */
    @NotNull Poll buildFromSession(
        @NotNull Player creator,
        @NotNull PollBuildSession session
    );

    /**
     * Registers a new {@link Poll}
     *
     * @param creator the author of the poll
     * @param poll the poll instance
     * @return the unique id of the created poll
     */
    CompletableFuture<UUID> registerPoll(
        @NotNull Player creator,
        @NotNull Poll poll
    );

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

    /** Record that a player voted in this poll (runtime-only cache) */
    void markVoted(@NotNull UUID pollId, @NotNull UUID voterId);

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

    /**
     * Returns the set of unique voter UUIDs for a given poll
     */
    Set<UUID> findVoters(@NotNull UUID pollId);

    void clearLocal(@NotNull UUID pollId);

}

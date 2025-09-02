package com.glance.consensus.platform.paper.event;

import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * TODO: implement
 * Fired when a player attempts to vote on a poll, before any state mutation
 */
@Getter
public class PlayerPollVoteAttemptEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull PollRuntime runtime;
    private final int optionIndex;
    private final @NotNull Set<Integer> beforeSelections;
    private final @NotNull Set<Integer> proposedSelections;

    private boolean cancelled;

    @Setter
    private String cancelReason;

    public PlayerPollVoteAttemptEvent(
        @NotNull Player player,
        @NotNull PollRuntime runtime,
        int optionIndex,
        @NotNull Set<Integer> beforeSelections,
        @NotNull Set<Integer> proposedSelections
    ) {
        this.player = player;
        this.runtime = runtime;
        this.optionIndex = optionIndex;
        this.beforeSelections = beforeSelections;
        this.proposedSelections = proposedSelections;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }

}

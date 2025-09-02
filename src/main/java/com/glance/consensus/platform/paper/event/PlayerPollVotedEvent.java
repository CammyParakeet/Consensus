package com.glance.consensus.platform.paper.event;

import com.glance.consensus.platform.paper.polls.runtime.PollRuntime;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * TODO: implement
 * Fired after a player's vote has been successfully persisted
 */
@Getter
public class PlayerPollVotedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @NotNull Player player;
    private final @NotNull PollRuntime runtime;
    private final @NotNull Set<Integer> beforeSelections;
    private final @NotNull Set<Integer> afterSelections;

    public PlayerPollVotedEvent(
            @NotNull Player player,
            @NotNull PollRuntime runtime,
            @NotNull Set<Integer> beforeSelections,
            @NotNull Set<Integer> afterSelections
    ) {
        super();
        this.player = player;
        this.runtime = runtime;
        this.beforeSelections = beforeSelections;
        this.afterSelections = afterSelections;
    }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }

}

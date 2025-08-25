package com.glance.consensus.platform.paper.polls.builder;

import com.glance.consensus.platform.paper.module.Manager;
import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Singleton
@AutoService(Manager.class)
public final class DefaultPollBuilderNavigator implements Manager, PollBuildNavigator {

    private final PollBuilderSessions registry;
    private final Map<PollBuildSession.Stage, PollBuildScreen> screens;

    @Inject
    public DefaultPollBuilderNavigator(
        @NotNull final PollBuilderSessions registry,
        @NotNull Set<PollBuildScreen> screenSet // multi-bind
    ) {
        this.registry = registry;
        this.screens = new EnumMap<>(PollBuildSession.Stage.class);
        for (var s : screenSet) this.screens.put(s.stage(), s);
    }

    public PollBuildSession session(@NotNull UUID playerId) {
        return registry.getOrCreate(playerId);
    }

    @Override
    public void clear(@NotNull UUID playerId) {
        registry.clear(playerId);
    }

    @Override
    public void open(
        @NotNull Player player,
        @NotNull PollBuildSession.Stage stage
    ) {
       var session = registry.getOrCreate(player.getUniqueId());
       var screen = screens.get(stage);
       if (screen == null) {
           player.sendMessage(Component.text("Error | Missing dialog: " + stage).color(NamedTextColor.RED));
           return;
       }

       screen.open(player, session);
    }

    @Override
    public void onDisable() {
        this.screens.clear();
    }
}

package com.glance.consensus.platform.paper.module;

import com.glance.consensus.platform.paper.polls.builder.DefaultPollBuilderNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuildNavigator;
import com.glance.consensus.platform.paper.polls.builder.PollBuilderSessions;
import com.glance.consensus.platform.paper.polls.runtime.DefaultPollManager;
import com.glance.consensus.platform.paper.polls.runtime.PollManager;
import com.google.inject.AbstractModule;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ConsensusModule extends AbstractModule {

    private final Plugin plugin;

    public ConsensusModule(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        this.bind(Plugin.class).toInstance(plugin);
        this.bind(JavaPlugin.class).toInstance((JavaPlugin) plugin);

        this.bind(PollBuilderSessions.class);
        this.bind(PollBuildNavigator.class).to(DefaultPollBuilderNavigator.class).asEagerSingleton();
        this.bind(PollManager.class).to(DefaultPollManager.class).asEagerSingleton();

        install(new PollBuilderWizardModule(plugin));
    }
}

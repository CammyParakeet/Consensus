package com.glance.consensus.platform.paper.module;

import com.glance.consensus.bootstrap.GuiceServiceLoader;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@Slf4j
public final class PollBuilderWizardModule extends AbstractModule {

    private final Plugin plugin;

    public PollBuilderWizardModule(@NotNull final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        var screens = Multibinder.newSetBinder(binder(), PollBuildScreen.class);
        for (Class<? extends PollBuildScreen> screenCls : GuiceServiceLoader.load(
                PollBuildScreen.class, classLoader)
        ) {
            plugin.getLogger().fine("Registering Poll Builder Screen: " + screenCls.getSimpleName());
            screens.addBinding().to(screenCls);
        };
    }

}

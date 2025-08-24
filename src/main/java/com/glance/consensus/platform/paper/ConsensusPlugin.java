package com.glance.consensus.platform.paper;

import com.glance.consensus.platform.paper.module.ConsensusModule;
import com.glance.consensus.platform.paper.module.PaperComponentScanner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class ConsensusPlugin extends JavaPlugin {

    @Getter
    private Injector injector;

    @Override
    public void onLoad() {
        this.injector = Guice.createInjector(new ConsensusModule(this));
        //getLogger().setLevel(Level.FINE);
    }

    @Override
    public void onEnable() {
        // todo config save/load

        PaperComponentScanner.scanAndInitialize(this, this.injector);
    }

    @Override
    public void onDisable() {
        PaperComponentScanner.scanAndInitialize(this, this.injector);
    }

}

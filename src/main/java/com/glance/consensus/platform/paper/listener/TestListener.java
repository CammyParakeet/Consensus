package com.glance.consensus.platform.paper.listener;

import com.google.auto.service.AutoService;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

@Slf4j
@Singleton
@AutoService(Listener.class)
public class TestListener implements Listener {

    @EventHandler
    public void onTest(InventoryCloseEvent e) {
        log.warn("Close inv event {} | {}", e, e.getInventory().getType());
    }

}

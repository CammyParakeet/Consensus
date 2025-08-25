package com.glance.consensus.platform.paper.module;

import com.glance.consensus.bootstrap.GuiceServiceLoader;
import com.glance.consensus.platform.paper.commands.engine.CommandHandler;
import com.glance.consensus.platform.paper.commands.engine.PaperCommandManager;
import com.glance.consensus.platform.paper.polls.builder.PollBuildScreen;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Utility component scanner to discover SPI registered classes </p>
 * <p> Constructs instances via {@link Guice} </p>
 * By default, supports common/known plugin lifecycle types:
 * <li>{@link Listener}</li>
 * <li>{@link Manager}</li>
 * <li>{@link CommandHandler}</li>
 * <li>Anything to come</li>
 * </p>
 * @author Cammy
 */
@Slf4j
@UtilityClass
public class PaperComponentScanner {

    public void scanAndInitialize(@NotNull final Plugin plugin, @NotNull Injector injector) {
        Logger logger = plugin.getLogger();
        logger.fine("[GuiceScan] Starting auto component scan for " + plugin.getName());

        ClassLoader classLoader = plugin.getClass().getClassLoader();

        // Manager Enable
        for (Class<? extends Manager> clazz : GuiceServiceLoader.load(
                Manager.class,
                classLoader)
        ) {
            try {
                Manager manager = injector.getInstance(clazz);
                manager.onEnable();
                logger.fine("[GuiceScan] Enabled Manager: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "enable Manager", clazz, e);
            }
        }

        // Bukkit Event Listeners
        for (Class<? extends Listener> clazz : GuiceServiceLoader.load(
                Listener.class,
                classLoader)
        ) {
            try {
                Listener listener = injector.getInstance(clazz);
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
                logger.fine("[GuiceScan] Registered Listener: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "register Listener", clazz, e);
            }
        }

        // Cloud commands
        PaperCommandManager commandManager;
        try {
            commandManager = injector.getInstance(PaperCommandManager.class);

            for (Class<? extends CommandHandler> clazz : GuiceServiceLoader.load(
                    CommandHandler.class,
                    classLoader)
            ) {
                CommandHandler command;
                try {
                    command = injector.getInstance(clazz);
                    commandManager.registerAnnotated(command);
                    logger.fine("[GuiceScan] Registered Command Handler: " + clazz.getName());
                } catch (Exception e) {
                    logError(logger, "register CommandHandler", clazz, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[GuiceScan] Failed to initialize Paper Command Manager: ", e);
        }
    }

    public void scanAndCleanup(@NotNull final Plugin plugin, @NotNull Injector injector) {
        Logger logger = plugin.getLogger();
        logger.info("Starting component cleanup for " + plugin.getName());

        ClassLoader classLoader = plugin.getClass().getClassLoader();

        // Manager Disable
        for (Class<? extends Manager> clazz : GuiceServiceLoader.load(
                Manager.class,
                classLoader)
        ) {
            try {
                Manager manager = injector.getInstance(clazz);
                manager.onDisable();
                logger.fine("[GuiceScan] Disabling Manager: " + clazz.getName());
            } catch (Exception e) {
                logError(logger, "disabling Manager", clazz, e);
            }
        }

        logger.info("[GuiceScan] Completed component cleanup for " + plugin.getName());
    }

    private void logError(Logger log, String context, Class<?> clazz, Exception e) {
        log.log(Level.SEVERE, "[GuiceScan] Failed to " + context + ": " + clazz.getName(), e);
    }

}

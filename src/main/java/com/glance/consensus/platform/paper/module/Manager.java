package com.glance.consensus.platform.paper.module;

/**
 * Represents a general-purpose lifecycle manager
 * <p>
 * Implementations of this interface are expected to manage the setup
 * and tear-down of a system
 * </p>
 * @author Cammy
 */
public interface Manager {

    /**
     * Called to initialize or enable the manager
     * <p>
     * Implementations should set up core state, register
     * dependencies, or prepare necessary resources
     */
    default void onEnable() {}

    /**
     * Called to shut down or disable the manager
     * <p>
     * Implementations should clean up resources, cancel
     * tasks, or unregister services as needed
     */
    default void onDisable() {}

    /**
     * Called to reload the manager's configuration or core state
     */
    default void reload() {}

    /**
     * Returns the name or identifier of this manager
     * <p>
     * Default implementation returns the simple class name
     *
     * @return the name of the manager
     */
    default String getName() {
        return getClass().getSimpleName();
    }

}

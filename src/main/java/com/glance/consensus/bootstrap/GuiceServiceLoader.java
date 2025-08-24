package com.glance.consensus.bootstrap;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lightweight SPI loader that discovers classes registered under META-INF/services/{interface}
 * <p>
 * Unlike {@link java.util.ServiceLoader}, we only return the classes, not instances,
 * for compatibility with external DI systems or manual construction pipelines
 * <p>
 * Designed for environments where auto-discovery is helpful for service registration/enabling
 *
 * <pre>{@code
 * for (Class<? extends Listener> clazz : ComponentServiceLoader.load(Listener.class)) {
 *     Listener listener = injector.getInstance(clazz);
 *     Bukkit.getPluginManager().registerEvents(listener, plugin);
 * }
 * }</pre>
 *
 * @param <S> The service type being discovered
 * @author Cammy
 */
public class GuiceServiceLoader<S> implements Iterable<Class<? extends S>> {

    private static final String META_INF_PATH = "META-INF/services/";

    private final Class<S> service;
    private final ClassLoader loader;

    private GuiceServiceLoader(Class<S> service, ClassLoader loader) {
        this.service = Objects.requireNonNull(service);
        this.loader = loader == null
                ? ClassLoader.getSystemClassLoader()
                : loader;
    }

    /**
     * Loads implementation classes for the given service using the current threads context classloader
     *
     * @param service the service interface or abstract base
     * @return a new {@link GuiceServiceLoader} for that type
     * @param <S> the type to load
     */
    public static <S> GuiceServiceLoader<S> load(Class<S> service) {
        return new GuiceServiceLoader<>(service, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Loads implementation classes for the given service using a specific classloader
     *
     * @param service the service interface or abstract base
     * @param loader the classloader to use
     * @return a new {@link GuiceServiceLoader} for that type
     * @param <S> the type to load
     */
    public static <S> GuiceServiceLoader<S> load(Class<S> service, ClassLoader loader) {
        return new GuiceServiceLoader<>(service, loader);
    }

    /**
     * Returns an iterator of implementation classes registered under {@code META-INF/services/{iface}}
     * <p>
     * Collects all discovered classes into a concrete list to resolve generic type safety
     *
     * @return an iterator over discovered service implementation classes
     */
    @NotNull
    @Override
    public Iterator<Class<? extends S>> iterator() {
        List<Class<? extends S>> classes = loadClasses(service, loader);
        return classes.iterator();
    }

    /**
     * Returns a sequenced {@link Stream} of discovered implementation classes
     *
     * @return a stream of registered service implementation classes
     */
    public Stream<Class<? extends S>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns the first implementation class discovered (if any)
     *
     * @return an {@link Optional} of the first discovered implementation
     */
    public Optional<Class<? extends S>> findFirst() {
        return stream().findFirst();
    }

    /**
     * Discovers and loads implementation classes of a given service interface using the standard SPI (META-INF/services).
     * This works great with {@link com.google.auto.service.AutoService} or other automatic service libraries
     * <p>
     * Each line in the discovered resource must be the fully qualified class name of a concrete implementation for the
     * provided {@code service} interface/abstraction
     *
     * @param service the base interface/abstract class to scan for
     * @param classLoader used to resolve META-INF/services entries and load the classes
     * @return a list of implementation classes that extend or implement the provided service type
     * @param <S> the service type being discovered
     */
    public static <S> List<Class<? extends S>> loadClasses(Class<S> service,  ClassLoader classLoader) {
        String resource = META_INF_PATH + service.getName();
        Set<Class<? extends S>> discovered = new LinkedHashSet<>();

        try {
            Enumeration<URL> urls = classLoader.getResources(resource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();

                try (
                        var in = url.openStream();
                        var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int commentIndex = line.indexOf('#');
                        if (commentIndex >= 0) line = line.substring(0, commentIndex);

                        line = line.trim();
                        if (line.isEmpty()) continue;

                        try {
                            Class<?> clazz = Class.forName(line, false, classLoader);
                            if (!service.isAssignableFrom(clazz)) {
                                System.err.println("[Class Scan] " + line + " is listed in " +
                                        resource + " but does not implement " + service.getName());
                                continue;
                            }

                            @SuppressWarnings("unchecked")
                            Class<? extends S> cast = (Class<? extends S>) clazz;
                            discovered.add(cast);
                        } catch (ClassNotFoundException e) {
                            System.err.println("[Class Scan] Failed to load class " + line + ": \n" + e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Class Scan] Failed to load SPI entries for " + service.getName() + ": \n" + e);
        }

        return List.copyOf(discovered);
    }

}

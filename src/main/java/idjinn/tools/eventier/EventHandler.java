package idjinn.tools.eventier;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class EventHandler implements IEventHandler {
    private static final Logger log = LogManager.getLogger();
    private final Map<Class<? extends Event>, List<ListenerCallback>> listeners;

    private final boolean TRACE;

    public EventHandler() {
        this.listeners = new HashMap<>();
        this.TRACE = false;
    }

    public EventHandler(final boolean trace) {
        this.listeners = new HashMap<>();
        this.TRACE = trace;
    }

    private List<Class<?>> getClassesFromPackage(final String packageName) throws IOException, ClassNotFoundException {
        final var classLoader = Thread.currentThread().getContextClassLoader();
        final var path = packageName.replace('.', '/');
        final var resources = classLoader.getResources(path);
        final var directories = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            var resource = resources.nextElement();
            directories.add(new File(resource.getFile()));
        }

        final var classes = new ArrayList<Class<?>>();
        for (File directory : directories)
            classes.addAll(this.findClasses(directory, packageName));

        return classes;
    }


    private List<Class<?>> findClasses(final File directory, final String packageName) throws ClassNotFoundException {
        final var classes = new ArrayList<Class<?>>();
        if (!directory.exists()) return classes;

        var files = directory.listFiles();
        if (files == null) return classes;

        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                String subPackage = packageName + "." + file.getName();
                classes.addAll(this.findClasses(file, subPackage));
            } else if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(packageName + '.' + className));
            }
        }
        return classes;
    }

    private List<Method> getEventListenersOf(final Class<?> clazz) {
        final var listenerMethods = new ArrayList<Method>();
        for (final var method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() != 1) continue;

            final var annotations = method.getDeclaredAnnotations();
            for (final var annotation : annotations) {
                if (annotation.annotationType().equals(EventListener.class)) {
                    listenerMethods.add(method);
                    break;
                }
            }
        }
        return listenerMethods;
    }

    private List<Method> getEventListenersOf(final List<Class<?>> classes) {
        final var listenerMethods = new ArrayList<Method>();
        for (final var clazz : classes) {
            listenerMethods.addAll(this.getEventListenersOf(clazz));
        }
        return listenerMethods;
    }

    public void registerEventsFromPackage(final String packageName) throws IOException, ClassNotFoundException {
        this.registerEvents(this.getClassesFromPackage(packageName));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerEvents(final List<Class<?>> classes) {
        for (final var listenerMethod : this.getEventListenersOf(classes)) {
            var eventType = Arrays.stream(listenerMethod.getParameterTypes()).findFirst();
            if (eventType.isPresent() && Event.class.isAssignableFrom(eventType.get())) {
                final var listenerAnnotation = ((EventListener) Arrays.stream(listenerMethod.getDeclaredAnnotations()).filter(a -> a.annotationType().equals(EventListener.class)).findFirst().get());
                final var priority = listenerAnnotation.priority();
                final var listenCancelled = listenerAnnotation.listenCancelled();
                this.listeners.computeIfAbsent((Class<? extends Event>) eventType.get(), k -> new ArrayList<>()).add(new ListenerCallback(null, listenerMethod, priority, listenCancelled));
            }
        }
        for (var listenerMethods : this.listeners.values()) {
            Collections.sort(listenerMethods);
        }
    }

    @Override
    public void unregisterEvents(final List<Class<?>> classes) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TListener> boolean subscribe(final TListener listenerInstance) {
        final var listenerMethods = this.getEventListenersOf(listenerInstance.getClass());
        for (final var listenerMethod : listenerMethods) {
            var eventType = Arrays.stream(listenerMethod.getParameterTypes()).findFirst();
            if (eventType.isPresent() && Event.class.isAssignableFrom(eventType.get())) {
                final var listenerAnnotation = (EventListener) Arrays.stream(listenerMethod.getDeclaredAnnotations()).filter(a -> a.annotationType().equals(EventListener.class)).findFirst().get();
                final var priority = listenerAnnotation.priority();
                final var listenCancelled = listenerAnnotation.listenCancelled();
                this.listeners.computeIfAbsent((Class<? extends Event>) eventType.get(), k -> new ArrayList<>()).add(new ListenerCallback(listenerInstance, listenerMethod, priority, listenCancelled));
            }
        }

        for (var methods : this.listeners.values()) {
            Collections.sort(methods);
        }
        return true;
    }

    @Override
    public <TListener> boolean unsubscribe(final TListener listenerInstance) {
        final var listenerMethods = this.getEventListenersOf(listenerInstance.getClass());

        for (final var listenerMethod : listenerMethods) {
            var eventType = Arrays.stream(listenerMethod.getParameterTypes()).findFirst();
            if (eventType.isPresent() && Event.class.isAssignableFrom(eventType.get())) {
                Class<? extends Event> eventClass = (Class<? extends Event>) eventType.get();

                var callbacks = this.listeners.get(eventClass);
                if (callbacks != null) {
                    callbacks.removeIf(callback -> callback.instance() != null && callback.instance().equals(listenerInstance));
                    if (callbacks.isEmpty()) this.listeners.remove(eventClass);
                }
            }
        }
        return true;
    }

    @Override
    public <TEvent extends Event> TEvent onEvent(final TEvent event) {
        final var listeners = this.listeners.get(event.getClass());
        if (listeners == null) return event;

        if (TRACE) log.trace("onEvent {} with value {}", event.getClass().getSimpleName(), event.toString());
        for (final var listener : listeners) {
            final var ignored = event instanceof Cancellable cancellableEvent && cancellableEvent.isCancelled();
            if (ignored) {
                if (TRACE)
                    log.trace("event listener {} was ignored because event {} was cancelled", listener.method.getClass().getName(), event.getClass().getSimpleName());
                continue;
            }

            try {
                final var oldHashCode = event.hashCode();
                final var oldValue = event.toString();
                listener.method.invoke(listener.instance, event);
                if (event.hashCode() != oldHashCode) {
                    if (TRACE)
                        log.trace("event listener {} changed value of event {} from {} to {}", listener.method.getClass().getName(), event.getClass().getSimpleName(), oldValue.toString(), event.toString());
                    continue;
                }

                if (TRACE)
                    log.trace("event listener {} listened event {}", listener.method.getClass().getName(), event.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("failed to invoke event listener {}: {}", listener.method.getClass().getName(), e.getMessage(), e);
            }
        }

        log.debug("event {} triggered total of {} listeners", event.getClass().getSimpleName(), listeners.size());
        return event;
    }

    public Map<Class<? extends Event>, List<ListenerCallback>> getListeners() {
        return listeners;
    }

    public record ListenerCallback(@Nullable Object instance, Method method, EventListenerPriority priority,
                                   boolean listenCancelled) implements Comparable<ListenerCallback> {
        @Override
        public int compareTo(@NotNull final EventHandler.ListenerCallback o) {
            return Integer.compare(o.priority.ordinal(), this.priority.ordinal());
        }
    }
}

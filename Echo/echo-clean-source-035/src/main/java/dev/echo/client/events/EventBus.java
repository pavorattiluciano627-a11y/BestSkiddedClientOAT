package dev.echo.client.events;

import dev.echo.client.Echo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Exact-event-class dispatch, with priority-ordered methods followed by callbacks.
 * Uses the shared volatile Echo.eventsDisabled lifecycle gate on every register
 * and post entry. Method handles here are ordinary subscriber invocation.
 */
public final class EventBus {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final MethodType subscriberType = MethodType.methodType(void.class, Event.class);
    private static final MethodHandle[] emptySubscribers = new MethodHandle[0];
    private static final Map<Class<?>, SortedMap<EventPriority, LinkedHashMap<Object, MethodHandle[]>>>
            subscribersByPriority = new LinkedHashMap<>();
    private static final Map<Object, Boolean> registeredOwners = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle[]> dispatchCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, ConcurrentHashMap<Object, Consumer<Event>>>
            functionalSubscribers = new ConcurrentHashMap<>();

    public static void register(Object owner) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (owner == null || registeredOwners.putIfAbsent(owner, Boolean.TRUE) != null) {
            return;
        }
        synchronized (EventBus.class) {
            // Deliberately excludes inherited methods and retains reflection order.
            for (Method method : owner.getClass().getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])
                        || !method.isAnnotationPresent(Subscribe.class)) {
                    continue;
                }
                method.setAccessible(true);
                MethodHandle subscriber;
                try {
                    subscriber = lookup.unreflect(method).bindTo(owner).asType(subscriberType);
                } catch (IllegalAccessException failure) {
                    failure.printStackTrace();
                    continue;
                }
                EventPriority priority = method.getAnnotation(Subscribe.class).priority();
                Map<Object, MethodHandle[]> owners = subscribersByPriority
                        .computeIfAbsent(parameters[0], eventType -> new TreeMap<>())
                        .computeIfAbsent(priority, eventPriority -> new LinkedHashMap<>());
                MethodHandle[] previous = owners.get(owner);
                if (previous == null) {
                    owners.put(owner, new MethodHandle[] {subscriber});
                } else {
                    MethodHandle[] next = new MethodHandle[previous.length + 1];
                    System.arraycopy(previous, 0, next, 0, previous.length);
                    next[previous.length] = subscriber;
                    owners.put(owner, next);
                }
                rebuildDispatchCache(parameters[0]);
            }
        }
    }

    public static void unregister(Object owner) {
        if (registeredOwners.remove(owner) == null) {
            return;
        }
        synchronized (EventBus.class) {
            for (var entry : subscribersByPriority.entrySet()) {
                boolean changed = false;
                for (var owners : entry.getValue().values()) {
                    changed |= owners.remove(owner) != null;
                }
                if (changed) {
                    rebuildDispatchCache(entry.getKey());
                }
            }
        }
    }

    private static void rebuildDispatchCache(Class<?> eventType) {
        var priorities = subscribersByPriority.get(eventType);
        if (priorities == null) {
            dispatchCache.remove(eventType);
            return;
        }
        ArrayList<MethodHandle> subscribers = new ArrayList<>();
        for (var owners : priorities.values()) {
            for (MethodHandle[] methods : owners.values()) {
                for (MethodHandle method : methods) {
                    subscribers.add(method);
                }
            }
        }
        if (subscribers.isEmpty()) {
            dispatchCache.remove(eventType);
        } else {
            dispatchCache.put(eventType, subscribers.toArray(emptySubscribers));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> void registerCallback(Class<T> eventType, Object owner,
                                                         Consumer<T> callback) {
        // Keep the consumer itself: no wrapper, eager null check, or exception handler.
        Consumer<Event> erased = (Consumer<Event>) (Consumer<?>) callback;
        functionalSubscribers.computeIfAbsent(eventType, type -> new ConcurrentHashMap<>())
                .put(owner, erased);
    }

    public static void unregisterCallbacks(Object owner) {
        for (var callbacks : functionalSubscribers.values()) {
            callbacks.remove(owner);
        }
    }

    public static boolean hasSubscribers(Class<? extends Event> eventType) {
        if (dispatchCache.containsKey(eventType)) {
            return true;
        }
        var callbacks = functionalSubscribers.get(eventType);
        return callbacks != null && !callbacks.isEmpty();
    }

    public static boolean post(Event event) {
        if (Echo.eventsDisabled) {
            return false;
        }
        dev.echo.client.diag.StartupDiagnostics.countDispatch();
        MethodHandle[] methods = dispatchCache.get(event.getClass());
        if (methods != null) {
            for (MethodHandle method : methods) {
                try {
                    method.invokeExact(event);
                } catch (Throwable failure) {
                    failure.printStackTrace();
                }
            }
        }
        // Fetch after method dispatch: callbacks registered by a method run this post.
        var callbacks = functionalSubscribers.get(event.getClass());
        if (callbacks != null) {
            for (Consumer<Event> callback : callbacks.values()) {
                callback.accept(event);
            }
        }
        return event.cancelled;
    }

    public static void clear() {
        synchronized (EventBus.class) {
            subscribersByPriority.clear();
            dispatchCache.clear();
        }
        registeredOwners.clear();
        functionalSubscribers.clear();
    }
}

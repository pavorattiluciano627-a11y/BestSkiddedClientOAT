package dev.echo.client.features.impl.internals;

import dev.echo.client.feature.internals.ClientCleanupAction;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/** Deferred graph cleanup with original lazy root order and named JDK memory operations. */
public final class ClientStringCleanup {
    private final Class<?> applicationClass;

    public ClientStringCleanup(Class<?> applicationClass) {
        this.applicationClass = applicationClass;
    }

    /** The memory-access failure path does not read or initialize the dashboard/HUD singletons. */
    public List<ClientCleanupAction> collectActions(Class<?> nameAssignmentsClass,
            Supplier<?> dashboard, Supplier<?> hud) {
        StringMemoryAccess memory = StringMemoryAccess.create();
        if (memory == null) {
            return new ArrayList<>();
        }
        Set<Object> seenObjects = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Class<?>> seenClasses = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ClientCleanupAction> actions = new ArrayList<>();
        Deque<Object> queue = new ArrayDeque<>();
        collectFields(applicationClass, null, memory, seenClasses, actions, queue);
        collectFields(nameAssignmentsClass, null, memory, seenClasses, actions, queue);
        enqueue(queue, dashboard.get());
        enqueue(queue, hud.get());
        while (!queue.isEmpty()) {
            Object object = queue.removeFirst();
            if (object instanceof String || !seenObjects.add(object)) {
                continue;
            }
            Class<?> type = object.getClass();
            if (type.isArray()) {
                collectArray(object, actions, queue);
                continue;
            }
            if (object instanceof Map<?, ?> map) {
                collectMap(map, actions, queue);
                continue;
            }
            if (object instanceof Collection<?> collection) {
                collectCollection(collection, actions, queue);
                continue;
            }
            if (object instanceof Optional<?> optional) {
                optional.ifPresent(value -> enqueue(queue, value));
                continue;
            }
            if (!belongsToApplication(type)) {
                continue;
            }
            for (Class<?> current = type; current != null && belongsToApplication(current);
                    current = current.getSuperclass()) {
                collectFields(current, object, memory, seenClasses, actions, queue);
            }
        }
        return actions;
    }

    public static void clearStrings(Collection<String> strings) {
        StringMemoryAccess memory = StringMemoryAccess.create();
        if (memory == null) {
            return;
        }
        for (String string : strings) {
            memory.clearString(string);
        }
    }

    private static void collectFields(Class<?> type, Object object, StringMemoryAccess memory,
            Set<Class<?>> seenClasses, List<ClientCleanupAction> actions, Deque<Object> queue) {
        boolean firstClassVisit = seenClasses.add(type);
        for (Field field : type.getDeclaredFields()) {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            if (isStatic && !firstClassVisit) {
                continue;
            }
            if (!isStatic && object == null) {
                continue;
            }
            if (!field.trySetAccessible()) {
                continue;
            }
            Object owner = isStatic ? null : object;
            try {
                Object value = field.get(owner);
                if (value instanceof String) {
                    actions.add(() -> memory.clearReference(field, owner));
                } else {
                    enqueue(queue, value);
                }
            } catch (IllegalAccessException inaccessible) {
                // Only the failed field is skipped; other reflective failures propagate.
            }
        }
    }

    private static void collectArray(Object array, List<ClientCleanupAction> actions, Deque<Object> queue) {
        if (array.getClass().getComponentType().isPrimitive()) {
            return;
        }
        boolean containsStrings = false;
        for (int index = 0, length = Array.getLength(array); index < length; index++) {
            Object value = Array.get(array, index);
            if (value instanceof String) {
                containsStrings = true;
            } else {
                enqueue(queue, value);
            }
        }
        if (containsStrings) {
            actions.add(() -> {
                for (int index = 0; index < Array.getLength(array); index++) {
                    if (Array.get(array, index) instanceof String) {
                        Array.set(array, index, null);
                    }
                }
            });
        }
    }

    private static void collectCollection(Collection<?> collection, List<ClientCleanupAction> actions,
            Deque<Object> queue) {
        boolean containsStrings = false;
        for (Object value : collection) {
            if (value instanceof String) {
                containsStrings = true;
            } else {
                enqueue(queue, value);
            }
        }
        if (containsStrings) {
            actions.add(() -> {
                try {
                    collection.removeIf(String.class::isInstance);
                } catch (RuntimeException unsupportedMutation) {
                    // Immutable/rejecting collections do not abort terminal cleanup.
                }
            });
        }
    }

    private static void collectMap(Map<?, ?> map, List<ClientCleanupAction> actions, Deque<Object> queue) {
        boolean containsStrings = false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            containsStrings |= key instanceof String || value instanceof String;
            if (!(key instanceof String)) {
                enqueue(queue, key);
            }
            if (!(value instanceof String)) {
                enqueue(queue, value);
            }
        }
        if (containsStrings) {
            actions.add(() -> {
                try {
                    map.entrySet().removeIf(entry -> entry.getKey() instanceof String || entry.getValue() instanceof String);
                } catch (RuntimeException unsupportedMutation) {
                    // The original tolerates per-map mutation failures.
                }
            });
        }
    }

    private boolean belongsToApplication(Class<?> type) {
        if (type.getClassLoader() != applicationClass.getClassLoader()) {
            return false;
        }
        try {
            ProtectionDomain applicationDomain = applicationClass.getProtectionDomain();
            ProtectionDomain candidateDomain = type.getProtectionDomain();
            if (candidateDomain == applicationDomain) {
                return true;
            }
            if (applicationDomain == null || candidateDomain == null || applicationDomain.getCodeSource() == null) {
                return false;
            }
            return applicationDomain.getCodeSource().equals(candidateDomain.getCodeSource());
        } catch (SecurityException denied) {
            return false;
        }
    }

    private static void enqueue(Deque<Object> queue, Object object) {
        if (object != null) {
            queue.addLast(object);
        }
    }
}

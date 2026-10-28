package dev.echo.client.features.impl.internals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * The original platform-memory API used by terminal client cleanup.
 * Reflection is confined to the named JDK Unsafe API and String storage field.
 * @param unsafe platform Unsafe instance
 * @param objectFieldOffset object-field offset query
 * @param staticFieldOffset static-field offset query
 * @param staticFieldBase static-field base query
 * @param getObject platform reference read
 * @param putObject platform reference write
 */
public record StringMemoryAccess(Object unsafe, Method objectFieldOffset, Method staticFieldOffset,
                                 Method staticFieldBase, Method getObject, Method putObject) {
    /** Resolves the supported platform API, returning null when access is unavailable. */
    public static StringMemoryAccess create() {
        try {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field singleton = type.getDeclaredField("theUnsafe");
            if (!singleton.trySetAccessible()) {
                return null;
            }
            Object instance = singleton.get(null);
            return new StringMemoryAccess(instance,
                    type.getMethod("objectFieldOffset", Field.class),
                    type.getMethod("staticFieldOffset", Field.class),
                    type.getMethod("staticFieldBase", Field.class),
                    type.getMethod("getObject", Object.class, long.class),
                    type.getMethod("putObject", Object.class, long.class, Object.class));
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }

    /** Clears a reference field, retaining the original best-effort failure handling. */
    public void clearReference(Field field, Object owner) {
        try {
            boolean isStatic = Modifier.isStatic(field.getModifiers());
            Object base = isStatic ? staticFieldBase.invoke(unsafe, field) : owner;
            Method offsetQuery = isStatic ? staticFieldOffset : objectFieldOffset;
            long offset = (Long) offsetQuery.invoke(unsafe, field);
            putObject.invoke(unsafe, base, offset, null);
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // Terminal cleanup continues when this individual field cannot be cleared.
        }
    }

    /** Zeros byte- or char-backed String storage without changing cached hash fields. */
    public void clearString(String string) {
        if (string == null || string.isEmpty()) {
            return;
        }
        try {
            Field value = String.class.getDeclaredField("value");
            long offset = (Long) objectFieldOffset.invoke(unsafe, value);
            Object storage = getObject.invoke(unsafe, string, offset);
            if (storage instanceof byte[] bytes) {
                Arrays.fill(bytes, (byte) 0);
            } else if (storage instanceof char[] characters) {
                Arrays.fill(characters, (char) 0);
            }
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            // The original leaves unsupported String layouts untouched.
        }
    }
}

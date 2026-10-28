package dev.echo.client.setting.list;

import java.util.function.Predicate;

/** A named registry-entry predicate. Null predicates retain their original failure behavior. */
public record RegistryEntryGroup<T>(CharSequence label, Predicate<T> predicate) {
    public boolean matches(T entry) {
        return predicate.test(entry);
    }
}

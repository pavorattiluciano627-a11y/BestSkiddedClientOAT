package dev.echo.client.utils;

import java.util.HashSet;
import java.util.Set;

/** Shared ownership claims; add/remove use equality, exclusion uses identity. */
public final class OwnerSet {
    private static final Set<Object> owners = new HashSet<>();

    private OwnerSet() {
    }

    public static void add(Object owner) {
        owners.add(owner);
    }

    public static void remove(Object owner) {
        owners.remove(owner);
    }

    public static boolean hasOwners() {
        return !owners.isEmpty();
    }

    public static boolean hasOtherOwner(Object excludedOwner) {
        for (Object owner : owners) {
            if (owner != excludedOwner) {
                return true;
            }
        }
        return false;
    }
}

package dev.echo.client.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maintains case-insensitive friend names in insertion order. */
public final class FriendManager {
    private final Map<String, String> friendsByName = new LinkedHashMap<>();

    /** Creates an empty friend list. */
    public FriendManager() { }

    /**
     * Inserts a trimmed name or updates the spelling of an existing entry.
     *
     * @param name the friend name
     * @return true only when a new entry was inserted
     */
    public synchronized boolean add(String name) {
        String displayName = trimName(name);
        if (displayName == null) {
            return false;
        }
        return friendsByName.put(normalizeName(displayName), displayName) == null;
    }

    /**
     * Removes a name case-insensitively.
     *
     * @param name the friend name
     * @return whether an entry was removed
     */
    public synchronized boolean remove(String name) {
        String normalized = normalizeName(name);
        return normalized != null && friendsByName.remove(normalized) != null;
    }

    /**
     * Tests a trimmed name case-insensitively.
     *
     * @param name the friend name
     * @return whether the name is present
     */
    public synchronized boolean contains(String name) {
        String normalized = normalizeName(name);
        return normalized != null && friendsByName.containsKey(normalized);
    }

    /**
     * Copies the names in insertion order.
     *
     * @return an independently mutable list of display names
     */
    public synchronized List<String> names() {
        return new ArrayList<>(friendsByName.values());
    }

    /**
     * Replaces all friends, treating null as an empty collection.
     *
     * @param names the new friend names
     */
    public synchronized void replaceAll(Collection<String> names) {
        friendsByName.clear();
        if (names == null) {
            return;
        }
        for (String name : names) {
            add(name);
        }
    }

    /** Removes all friends. */
    public synchronized void clear() {
        friendsByName.clear();
    }

    /**
     * Counts the names.
     *
     * @return the number of distinct case-insensitive names
     */
    public synchronized int size() {
        return friendsByName.size();
    }

    private static String trimName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeName(String name) {
        String trimmed = trimName(name);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}

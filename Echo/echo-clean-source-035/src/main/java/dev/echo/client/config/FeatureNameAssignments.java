package dev.echo.client.config;

import dev.echo.client.Echo;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.HudFeature;
import dev.echo.client.features.internals.ClickGUIFeature;
import dev.echo.client.input.BindMode;
import dev.echo.client.input.InputKey;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns cached feature IDs, binding operations, display snapshots and change history.
 *
 * <p>The static collections deliberately belong to this class: callers which
 * inspect the original provider's class identity must encounter its actual data.
 * Cached IDs use ordinary HashMap key equality and retain their String identity.
 */
public final class FeatureNameAssignments {
    private static final Map<Feature, String> assignedNames = new HashMap<>();
    private static final Map<Feature, Boolean> previousStates = new HashMap<>();
    private static final ArrayDeque<NameAssignmentHistory> history = new ArrayDeque<>();
    private static final int HISTORY_LIMIT = 64;
    private static long nextSequence = 1L;

    private FeatureNameAssignments() { }

    /** Returns nonempty non-HUD categories in the enum's declaration order. */
    public static List<Category> categories() {
        List<Category> result = new ArrayList<>();
        for (Category category : Category.values()) {
            if (!features(category).isEmpty()) {
                result.add(category);
            }
        }
        return result;
    }

    /** Returns visible non-HUD features in the manager's category-list order. */
    public static List<Feature> features(Category category) {
        return features(category, false);
    }

    /**
     * Copies a category's non-HUD features, optionally including invisible ones.
     *
     * @param category the manager category
     * @param includeInvisible whether visibility should be ignored
     * @return a fresh mutable list, or the original empty-list fallback if no manager exists
     */
    public static List<Feature> features(Category category, boolean includeInvisible) {
        if (Echo.featureManager == null) {
            return List.of();
        }
        List<Feature> result = new ArrayList<>();
        for (Feature feature : Echo.featureManager.featuresInCategory(category)) {
            if ((includeInvisible || feature.isVisible()) && !(feature instanceof HudFeature)) {
                result.add(feature);
            }
        }
        return result;
    }

    /** Returns visible, enableable HUD features sorted by lowercase label code units. */
    public static List<Feature> overlays() {
        List<Feature> result = new ArrayList<>();
        for (Feature feature : FeatureManager.allFeatures()) {
            if (feature.isVisible() && feature.canEnable() && feature instanceof HudFeature) {
                result.add(feature);
            }
        }
        result.sort(FeatureNameAssignments::compareOverlayLabels);
        return result;
    }

    /**
     * Returns a memoized short content hash of the feature's name.
     *
     * <p>Renaming or mutating a label does not replace an existing cache value.
     * A null hash is not installed by HashMap.computeIfAbsent. Like the original,
     * this method does not add synchronization around the cache operation.
     *
     * @param feature the feature used as the ordinary HashMap key
     * @return the original cached String object, or the computed value
     */
    public static String id(Feature feature) {
        return assignedNames.computeIfAbsent(feature, FeatureNameAssignments::createId);
    }

    /** Returns the live name object without flattening or copying it. */
    public static CharSequence label(Feature feature) {
        return feature.getName();
    }

    /** Returns the feature's description object. */
    public static CharSequence description(Feature feature) {
        return feature.getDescription();
    }

    /** Queries the feature's actual enabled-state accessor. */
    public static boolean isEnabled(Feature feature) {
        return feature.isEnabled();
    }

    /** Changes feature state through its actual lifecycle operation. */
    public static void setEnabled(Feature feature, boolean enabled) {
        feature.setEnabled(enabled);
    }

    /** Toggles through the feature's actual lifecycle operation. */
    public static void toggle(Feature feature) {
        feature.toggle();
    }

    /** Returns the current binding, retaining the feature accessor's null behavior. */
    public static InputKey binding(Feature feature) {
        return feature.getKey();
    }

    /** Queries input state for the feature's current binding. */
    public static boolean isBindingPressed(Feature feature) {
        return feature.getKey().isPressed();
    }

    /** Returns HOLD or TOGGLE from the feature's actual hold-mode flag. */
    public static BindMode bindMode(Feature feature) {
        return feature.isHoldMode() ? BindMode.HOLD : BindMode.TOGGLE;
    }

    /**
     * Changes activation mode, disabling an active old hold binding before the write.
     * A null mode has the original TOGGLE interpretation.
     */
    public static void setBindMode(Feature feature, BindMode mode) {
        boolean hold = mode == BindMode.HOLD;
        if (feature.isHoldMode() == hold) {
            return;
        }
        if (feature.isHoldMode() && feature.isEnabled()) {
            feature.setEnabled(false);
        }
        feature.setHoldMode(hold);
    }

    /**
     * Tests whether a keyboard binding belongs to the registered Click GUI feature.
     * Null, NONE and mouse bindings are not reserved by this rule.
     */
    public static boolean isReserved(InputKey key) {
        if (key == null || key == InputKey.NONE || key.isMouse()) {
            return false;
        }
        ClickGUIFeature clickGui = Echo.featureManager == null ? null
                : (ClickGUIFeature) Echo.featureManager.findFeature(ClickGUIFeature.class);
        return clickGui != null && clickGui.getKey() == key;
    }

    /**
     * Assigns a binding with the original reservation, equality and disable order.
     *
     * <p>Reservation is checked before equality, including for the Click GUI
     * feature itself. Null is not rewritten to NONE: the original setter accepts
     * it and the assignment result is ASSIGNED when it differs from the old key.
     */
    public static NameAssignmentResult assignBinding(Feature feature, InputKey key) {
        if (isReserved(key)) {
            return NameAssignmentResult.RESERVED;
        }
        if (feature.getKey() == key) {
            return NameAssignmentResult.UNCHANGED;
        }
        if (key == InputKey.NONE && feature instanceof ClickGUIFeature) {
            return NameAssignmentResult.RESERVED;
        }
        if (feature.isHoldMode() && feature.isEnabled()) {
            feature.setEnabled(false);
        }
        feature.setKey(key);
        return key == InputKey.NONE ? NameAssignmentResult.CLEARED : NameAssignmentResult.ASSIGNED;
    }

    /**
     * Captures visible non-HUD features and records their observed state changes.
     *
     * @return an immutable snapshot list, retaining feature/name/key identities
     */
    public static List<FeatureBindSnapshot> capture() {
        List<Feature> features = FeatureManager.allFeatures();
        List<FeatureBindSnapshot> result = new ArrayList<>(features.size());
        for (Feature feature : features) {
            if (!feature.isVisible() || feature instanceof HudFeature) {
                continue;
            }
            result.add(new FeatureBindSnapshot(feature, id(feature), label(feature),
                    feature.isEnabled(), binding(feature), bindMode(feature), isBindingPressed(feature)));
        }
        recordStateChanges(result);
        return List.copyOf(result);
    }

    /** Returns immutable history entries strictly after the supplied sequence. */
    public static synchronized List<NameAssignmentHistory> changesAfter(long sequence) {
        List<NameAssignmentHistory> result = new ArrayList<>();
        for (NameAssignmentHistory entry : history) {
            if (entry.sequence() > sequence) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    /** Removes history entries through and including an acknowledged sequence. */
    public static synchronized void acknowledgeChanges(long sequence) {
        while (!history.isEmpty() && history.peekFirst().sequence() <= sequence) {
            history.removeFirst();
        }
    }

    /** Returns the most recently assigned sequence, including acknowledged entries. */
    public static synchronized long latestChangeSequence() {
        return nextSequence - 1L;
    }

    /**
     * Clears this provider's ID cache, prior-state map and history in that order.
     * Resets numbering to one without changing feature state or binding objects.
     */
    public static synchronized void clearAssignments() {
        assignedNames.clear();
        previousStates.clear();
        history.clear();
        nextSequence = 1L;
    }

    /**
     * Appends the existing cached String objects to the supplied collection.
     *
     * <p>The argument passed to addAll is the live HashMap values view. No sort,
     * copying, name recomputation or deduplication is added. The destination owns
     * its order/duplicate policy and exceptions, including partial additions.
     *
     * @param destination the caller's string collection
     */
    public static synchronized void collectAssignedNames(Collection<String> destination) {
        destination.addAll(assignedNames.values());
    }

    /** Adds an explicit feature notification with a monotonic nanosecond timestamp. */
    public static synchronized void postNotification(Feature feature, CharSequence suffix) {
        history.addLast(new NameAssignmentHistory(nextSequence++, feature, label(feature),
                feature.isEnabled(), System.nanoTime(), suffix));
        trimHistory();
    }

    private static synchronized void recordStateChanges(List<FeatureBindSnapshot> snapshots) {
        for (FeatureBindSnapshot snapshot : snapshots) {
            Boolean previous = previousStates.put(snapshot.feature(), snapshot.enabled());
            if (previous == null || previous == snapshot.enabled()) {
                continue;
            }
            if (!snapshot.feature().recordsStateChanges()) {
                continue;
            }
            history.addLast(new NameAssignmentHistory(nextSequence++, snapshot.feature(),
                    snapshot.label(), snapshot.enabled(), System.nanoTime()));
        }
        trimHistory();
    }

    private static void trimHistory() {
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    private static String createId(Feature feature) {
        return Concat.shortHash(feature.getName());
    }

    private static int compareOverlayLabels(Feature first, Feature second) {
        return Concat.compareLowercase(label(first), label(second));
    }
}

package dev.echo.client.features;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.integration.RuntimeEntitlementQuery;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered feature registration, class/category indexes, and shutdown.
 *
 * <p>This source is a reconstruction candidate. Its original-key, shared-cache,
 * and control-state applicability conditions are recorded in the adjacent
 * proof manifest. It is not a replacement bootstrap or a feature-definition list.
 */
public class FeatureManager {
    private final CopyOnWriteArrayList<Feature> features;
    private final Map<Class<? extends Feature>, Feature> featuresByClass;
    private final Map<Category, List<Feature>> featuresByCategory;
    private volatile boolean shuttingDown;

    public FeatureManager() {
        features = new CopyOnWriteArrayList<>();
        featuresByClass = new ConcurrentHashMap<>();
        featuresByCategory = new ConcurrentHashMap<>();
        shuttingDown = false;
    }

    /** Appends the complete input before installing its class-index entries. */
    public void addFeatures(CopyOnWriteArrayList<Feature> additions) {
        features.addAll(additions);
        for (Feature feature : additions) {
            featuresByClass.put(feature.getClass(), feature);
        }
    }

    /** Filters and sorts the caller's list, returning that same list. */
    public CopyOnWriteArrayList<Feature> sort(CopyOnWriteArrayList<Feature> input) {
        if (!RuntimeEntitlementQuery.isBeta()) {
            input.removeIf(Feature::isFilteredFromDefaultList);
        }
        input.sort((first, second) -> compareNamesIgnoreCase(first.getName(), second.getName()));
        return input;
    }

    /** Retains the supplied category lists, including their mutability and identity. */
    public void addCategoryCache(Map<Category, List<Feature>> categories) {
        featuresByCategory.putAll(categories);
    }

    /** Adds to the ordered and category lists; leaves the class index untouched. */
    public void addFeatureToLists(Feature feature) {
        features.add(feature);
        featuresByCategory.get(feature.getCategory()).add(feature);
    }

    /** Removes the first list occurrences; leaves the class index untouched. */
    public void removeFeatureFromLists(Feature feature) {
        features.remove(feature);
        featuresByCategory.get(feature.getCategory()).remove(feature);
    }

    public Feature findFeature(Class<?> type) {
        if (shuttingDown) {
            return null;
        }
        return featuresByClass.get(type);
    }

    public List<Feature> featuresInCategory(Category category) {
        if (shuttingDown) {
            return Collections.emptyList();
        }
        return featuresByCategory.getOrDefault(category, Collections.emptyList());
    }

    /** Returns an unmodifiable live view of the currently bound manager's list. */
    public static List<Feature> allFeatures() {
        FeatureManager manager = Echo.featureManager;
        if (manager == null || manager.shuttingDown) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(manager.features);
    }

    public static Optional<Feature> find(Class<?> type) {
        FeatureManager manager = Echo.featureManager;
        if (manager == null || manager.shuttingDown) {
            return Optional.empty();
        }
        return Optional.ofNullable(manager.findFeature(type));
    }

    /**
     * Disables each non-exempt feature, unregisters it, and clears its settings.
     * Only an ordinary exception from the disable call is ignored.
     */
    public void shutdownExcept(Feature exempt) {
        shuttingDown = true;
        for (Feature feature : features) {
            if (feature == null || feature == exempt) {
                continue;
            }
            try {
                feature.setEnabled(false);
            } catch (Exception ignored) {
                // The original continues with unregistration after this failure.
            }
            EventBus.unregister(feature);
            feature.settings.clear();
        }
        features.clear();
        featuresByClass.clear();
        featuresByCategory.clear();
    }

    public CopyOnWriteArrayList<Feature> getFeatures() {
        return features;
    }

    public Map<Class<? extends Feature>, Feature> getFeaturesByClass() {
        return featuresByClass;
    }

    public Map<Category, List<Feature>> getFeaturesByCategory() {
        return featuresByCategory;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    private static int compareNamesIgnoreCase(CharSequence first, CharSequence second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        int firstLength = first.length();
        int secondLength = second.length();
        int sharedLength = Math.min(firstLength, secondLength);
        for (int index = 0; index < sharedLength; index++) {
            char left = Character.toLowerCase(first.charAt(index));
            char right = Character.toLowerCase(second.charAt(index));
            if (left != right) {
                return left - right;
            }
        }
        return firstLength - secondLength;
    }
}

package dev.echo.client.config;

import java.util.List;
import java.util.Objects;

/**
 * The array-list and keybind rows derived from a common feature snapshot.
 *
 * @param arrayRows the enabled-feature rows
 * @param keybindRows the feature and setting keybind rows
 */
public record SnapshotDiff(List<FeatureSnapshotDiff> arrayRows, List<FeatureSnapshotEntry> keybindRows) {
    /** Makes independent immutable lists, rejecting null lists and elements. */
    public SnapshotDiff {
        arrayRows = List.copyOf(Objects.requireNonNull(arrayRows, "arrayRows"));
        keybindRows = List.copyOf(Objects.requireNonNull(keybindRows, "keybindRows"));
    }
}

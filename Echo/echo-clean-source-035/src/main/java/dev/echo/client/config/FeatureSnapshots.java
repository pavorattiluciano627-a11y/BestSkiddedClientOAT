package dev.echo.client.config;

import dev.echo.client.features.Category;
import dev.echo.client.input.InputKey;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.utils.strings.Concat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Projects supplied feature snapshots into enabled-feature and keybind display rows. */
public final class FeatureSnapshots {
    private FeatureSnapshots() { }

    /**
     * Builds both displays in supplied snapshot and live setting iteration order.
     *
     * <p>Feature enabled/key/pressed values come from the snapshot. Category and
     * setting bindings are read from the referenced live feature. Result lists
     * are immutable shallow copies made by {@link SnapshotDiff}.
     *
     * @param snapshots the required snapshots, in display order
     * @return both row lists
     */
    public static SnapshotDiff displayRows(List<FeatureBindSnapshot> snapshots) {
        Objects.requireNonNull(snapshots, "snapshots");
        List<FeatureSnapshotDiff> arrayRows = new ArrayList<>();
        List<FeatureSnapshotEntry> keybindRows = new ArrayList<>();
        for (FeatureBindSnapshot snapshot : snapshots) {
            Category category = snapshot.feature().getCategory();
            if (snapshot.enabled()) {
                Optional<String> keyLabel = snapshot.binding() == InputKey.NONE
                        ? Optional.empty() : Optional.of(snapshot.binding().displayLabel());
                arrayRows.add(new FeatureSnapshotDiff(snapshot.id(), snapshot.label(), category,
                        keyLabel, snapshot.pressed()));
            }
            List<FeatureSnapshotEntry> settings = snapshot.enabled()
                    ? settingRows(snapshot, category) : List.of();
            if (snapshot.binding() == InputKey.NONE && settings.isEmpty()) {
                continue;
            }
            keybindRows.add(new FeatureSnapshotEntry(snapshot.id(), snapshot.label(), category,
                    snapshot.binding() == InputKey.NONE ? "-" : snapshot.binding().displayLabel(),
                    snapshot.enabled(), snapshot.pressed()));
            keybindRows.addAll(settings);
        }
        return new SnapshotDiff(arrayRows, keybindRows);
    }

    /** Builds bound setting rows while retaining the original repeated getter order. */
    private static List<FeatureSnapshotEntry> settingRows(FeatureBindSnapshot snapshot, Category category) {
        List<FeatureSnapshotEntry> result = new ArrayList<>();
        for (Setting setting : snapshot.feature().settings) {
            if (!(setting instanceof KeybindSetting binding) || binding.getKey() == InputKey.NONE) {
                continue;
            }
            result.add(new FeatureSnapshotEntry(snapshot.id() + "/" + Concat.shortHash(binding.getLabel()),
                    binding.getLabel(), category, binding.getKey().displayLabel(), true,
                    binding.getKey().isPressed()));
        }
        return result;
    }

    /**
     * Builds the complete projection and returns its enabled-feature rows.
     *
     * @param snapshots the required snapshots
     * @return the immutable enabled-feature rows
     */
    public static List<FeatureSnapshotDiff> arrayRows(List<FeatureBindSnapshot> snapshots) {
        return displayRows(snapshots).arrayRows();
    }

    /**
     * Builds the complete projection and returns its feature/setting keybind rows.
     *
     * @param snapshots the required snapshots
     * @return the immutable keybind rows
     */
    public static List<FeatureSnapshotEntry> keybindRows(List<FeatureBindSnapshot> snapshots) {
        return displayRows(snapshots).keybindRows();
    }
}

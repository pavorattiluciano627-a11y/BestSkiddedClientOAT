package dev.echo.client.config;

import dev.echo.client.features.Feature;
import java.util.Objects;

/**
 * A numbered feature-state change with its occurrence time and optional suffix.
 *
 * @param sequence the monotonically assigned history sequence
 * @param feature the feature instance
 * @param label the feature label
 * @param enabled the recorded enabled state
 * @param timestampNanos the occurrence time from {@link System#nanoTime()}
 * @param suffix the optional extra label
 */
public record NameAssignmentHistory(long sequence, Feature feature, CharSequence label,
                                     boolean enabled, long timestampNanos, CharSequence suffix) {
    /** Validates the required feature and label while allowing a null suffix. */
    public NameAssignmentHistory {
        Objects.requireNonNull(feature, "module");
        Objects.requireNonNull(label, "label");
    }

    /**
     * Creates a history entry with no suffix.
     *
     * @param sequence the history sequence
     * @param feature the feature instance
     * @param label the feature label
     * @param enabled the recorded enabled state
     * @param timestampNanos the occurrence time from {@link System#nanoTime()}
     */
    public NameAssignmentHistory(long sequence, Feature feature, CharSequence label,
                                 boolean enabled, long timestampNanos) {
        this(sequence, feature, label, enabled, timestampNanos, null);
    }
}

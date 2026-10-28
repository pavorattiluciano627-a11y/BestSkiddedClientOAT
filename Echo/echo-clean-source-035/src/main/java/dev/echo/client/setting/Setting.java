package dev.echo.client.setting;

import dev.echo.client.integration.RuntimeEntitlementQuery;
import java.util.function.Predicate;

/**
 * Setting metadata, availability, visibility and change notification.
 * The external entitlement query is owned by the client-services shard.
 */
public class Setting {
    private final CharSequence label;
    private Predicate<Object> visibilityPredicate;
    private Runnable changeCallback;
    private CharSequence description = "";
    private CharSequence tooltip = "";
    private boolean restricted;

    public Setting(CharSequence label) {
        this.label = label;
    }

    public Setting(CharSequence label, Predicate<Object> visibilityPredicate) {
        this(label);
        this.visibilityPredicate = visibilityPredicate;
    }

    public Setting(String label) {
        this((CharSequence) label);
    }

    public String getName() { return label.toString(); }
    public CharSequence getLabel() { return label; }
    public CharSequence getUnit() { return ""; }
    public String getTypeId() { return ""; }

    public boolean isAvailable() {
        return !restricted || RuntimeEntitlementQuery.isBeta();
    }

    public boolean isVisible() {
        if (!isAvailable()) return false;
        return visibilityPredicate == null || visibilityPredicate.test(null);
    }

    public Setting restrict() {
        restricted = true;
        return this;
    }

    public Setting onChange(Runnable callback) {
        changeCallback = callback;
        return this;
    }

    public Setting tooltip(CharSequence text) {
        tooltip = text == null ? "" : text;
        return this;
    }

    public Setting description(CharSequence text) {
        description = text == null ? "" : text;
        return this;
    }

    public void notifyChanged() {
        if (changeCallback != null) changeCallback.run();
    }

    public String getTooltipText() { return tooltip.toString(); }
    public Predicate<Object> getVisibilityPredicate() { return visibilityPredicate; }
    public Runnable getChangeCallback() { return changeCallback; }
    public CharSequence getDescription() { return description; }
    public CharSequence getTooltip() { return tooltip; }
    public boolean isRestricted() { return restricted; }
    public void setVisibilityPredicate(Predicate<Object> predicate) { visibilityPredicate = predicate; }
    public void setChangeCallback(Runnable callback) { changeCallback = callback; }
}

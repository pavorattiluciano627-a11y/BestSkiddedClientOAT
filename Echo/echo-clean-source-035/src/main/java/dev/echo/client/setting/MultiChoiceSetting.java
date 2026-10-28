package dev.echo.client.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Insertion-ordered options with independently stored selections. */
public class MultiChoiceSetting extends Setting {
    private final LinkedHashMap<CharSequence, Boolean> selectedOptions = new LinkedHashMap<>();
    private boolean selectionMode;

    public MultiChoiceSetting(CharSequence label, CharSequence... options) {
        super(label);
        for (CharSequence option : options) selectedOptions.put(option, false);
    }

    public MultiChoiceSetting(CharSequence label, Predicate<Object> visibleWhen, CharSequence... options) {
        this(label, options);
        setVisibilityPredicate(visibleWhen);
    }

    public MultiChoiceSetting selectionMode() {
        selectionMode = true;
        return this;
    }

    private static boolean sameLabel(CharSequence first, CharSequence second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        int length = first.length();
        if (length != second.length()) return false;
        for (int index = 0; index < length; index++) {
            if (first.charAt(index) != second.charAt(index)) return false;
        }
        return true;
    }

    public void toggle(CharSequence option) {
        for (CharSequence key : selectedOptions.keySet()) {
            if (sameLabel(key, option)) {
                selectedOptions.put(key, !selectedOptions.get(key));
                notifyChanged();
                return;
            }
        }
    }

    public boolean isSelected(CharSequence option) {
        for (CharSequence key : selectedOptions.keySet()) {
            if (sameLabel(key, option)) return Boolean.TRUE.equals(selectedOptions.get(key));
        }
        return false;
    }

    public void replaceOptions(Map<? extends CharSequence, Boolean> options) {
        selectedOptions.clear();
        if (options == null) return;
        for (Map.Entry<? extends CharSequence, Boolean> entry : options.entrySet()) {
            selectedOptions.put(entry.getKey(), Boolean.TRUE.equals(entry.getValue()));
        }
        notifyChanged();
    }

    public void setSelected(CharSequence option, boolean selected) {
        for (CharSequence key : selectedOptions.keySet()) {
            if (sameLabel(key, option)) {
                selectedOptions.put(key, selected);
                notifyChanged();
                return;
            }
        }
        selectedOptions.put(option, selected);
        notifyChanged();
    }

    public Map<CharSequence, Boolean> getOptions() { return Collections.unmodifiableMap(selectedOptions); }

    public Map<String, Boolean> toStringMap() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Map.Entry<CharSequence, Boolean> entry : selectedOptions.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    @Override public String getTypeId() { return "mmd"; }
    public LinkedHashMap<CharSequence, Boolean> getMutableOptions() { return selectedOptions; }
    public boolean isSelectionMode() { return selectionMode; }
    public void setSelectionMode(boolean enabled) { selectionMode = enabled; }
}

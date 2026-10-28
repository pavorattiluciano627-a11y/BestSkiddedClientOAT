package dev.echo.client.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Editable text and a separately managed, ordered list of normalized entries. */
public class TextSetting extends Setting {
    private String text = "";
    private String placeholder;
    private final List<String> entries = new ArrayList<>();
    private Consumer<String> entryAdded;
    private Consumer<String> entryRemoved;
    private boolean listMode;
    private int maximumLength = 32;

    public TextSetting(CharSequence label, String placeholder) {
        this(label, placeholder, false);
    }

    public TextSetting(CharSequence label, String placeholder, boolean listMode) {
        super(label);
        this.placeholder = placeholder;
        this.listMode = listMode;
    }

    public TextSetting(CharSequence label, String placeholder, Predicate<Object> visibleWhen) {
        this(label, placeholder, false, visibleWhen);
    }

    public TextSetting(CharSequence label, String placeholder, boolean listMode, Predicate<Object> visibleWhen) {
        this(label, placeholder, listMode);
        setVisibilityPredicate(visibleWhen);
    }

    public TextSetting(String label, String placeholder) { this((CharSequence) label, placeholder); }

    public TextSetting(String label, String placeholder, boolean listMode) {
        this((CharSequence) label, placeholder, listMode);
    }

    public TextSetting(String label, String placeholder, Predicate<Object> visibleWhen) {
        this((CharSequence) label, placeholder, visibleWhen);
    }

    public TextSetting(String label, String placeholder, boolean listMode, Predicate<Object> visibleWhen) {
        this((CharSequence) label, placeholder, listMode, visibleWhen);
    }

    public void addEntry(String entry) {
        String normalized = normalize(entry);
        if (!normalized.isEmpty() && !entries.contains(normalized)) {
            entries.add(normalized);
            if (entryAdded != null) entryAdded.accept(normalized);
        }
    }

    public void removeEntry(String entry) {
        String normalized = normalize(entry);
        if (entries.remove(normalized) && entryRemoved != null) entryRemoved.accept(normalized);
    }

    public void clearEntries() { entries.clear(); }
    public boolean containsEntry(String entry) { return entries.contains(normalize(entry)); }
    public String getText() { return text; }
    public String getPlaceholder() { return placeholder; }
    public void setText(String text) { this.text = normalize(text); }

    public void setMaximumLength(int maximumLength) {
        this.maximumLength = Math.max(1, maximumLength);
        text = normalize(text);
        List<String> normalized = new ArrayList<>();
        for (String entry : entries) {
            String value = normalize(entry);
            if (!value.isEmpty() && !normalized.contains(value)) normalized.add(value);
        }
        entries.clear();
        entries.addAll(normalized);
    }

    private String normalize(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > maximumLength ? trimmed.substring(0, maximumLength) : trimmed;
    }

    @Override public String getTypeId() { return "str"; }
    public List<String> getEntries() { return entries; }
    public Consumer<String> getEntryAddedCallback() { return entryAdded; }
    public Consumer<String> getEntryRemovedCallback() { return entryRemoved; }
    public boolean isListMode() { return listMode; }
    public int getMaximumLength() { return maximumLength; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public void setEntryAddedCallback(Consumer<String> callback) { entryAdded = callback; }
    public void setEntryRemovedCallback(Consumer<String> callback) { entryRemoved = callback; }
    public void setListMode(boolean listMode) { this.listMode = listMode; }
}

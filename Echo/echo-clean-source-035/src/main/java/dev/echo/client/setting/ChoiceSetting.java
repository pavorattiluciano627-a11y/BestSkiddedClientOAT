package dev.echo.client.setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import dev.echo.client.utils.strings.Concat;

/** Ordered single selection with representation-aware Concat comparison. */
public class ChoiceSetting extends Setting {
    private CharSequence selectedOption;
    private List<CharSequence> options;

    public ChoiceSetting(CharSequence label, CharSequence initial, CharSequence... options) {
        this(label, initial, Arrays.asList(options));
    }

    public ChoiceSetting(CharSequence label, CharSequence initial,
                         Predicate<Object> visibleWhen, CharSequence... options) {
        this(label, initial, options);
        setVisibilityPredicate(visibleWhen);
    }

    public ChoiceSetting(CharSequence label, CharSequence initial, List<CharSequence> options) {
        super(label);
        this.options = options;
        selectedOption = initial;
    }

    private static boolean sameOption(CharSequence first, CharSequence second) {
        if (first instanceof Concat left && second instanceof Concat right) {
            return left.sameRepresentation(right);
        }
        return sameOrdinaryOption(first, second);
    }

    public static boolean sameOrdinaryOption(CharSequence first, CharSequence second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        int length = first.length();
        if (length != second.length()) return false;
        for (int index = 0; index < length; index++) {
            if (Character.toLowerCase(first.charAt(index)) != Character.toLowerCase(second.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    public void cycle() {
        int index = -1;
        for (int candidate = 0; candidate < options.size(); candidate++) {
            if (sameOption(options.get(candidate), selectedOption)) {
                index = candidate;
                break;
            }
        }
        if (index == -1) index = 0;
        index = (index + 1) % options.size();
        selectedOption = options.get(index);
        notifyChanged();
    }

    public boolean isSelected(CharSequence option) { return sameOption(selectedOption, option); }
    public CharSequence getValue() { return selectedOption; }
    public void setValue(CharSequence option) { selectedOption = option; notifyChanged(); }
    public void setValueSilently(CharSequence option) { selectedOption = option; }
    public List<CharSequence> getOptions() { return options; }
    public void setOptions(List<CharSequence> options) { this.options = options; }
    @Override public String getTypeId() { return "mod"; }
}

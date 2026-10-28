package dev.echo.client.setting;

import dev.echo.client.setting.range.NamedRange;
import dev.echo.client.setting.range.RangeBand;
import dev.echo.client.setting.range.RangeBounds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.echo.client.utils.strings.Concat;

/** Ordered, optionally bounded named ranges and their display bands. */
public class NamedRangeGroupSetting extends Setting {
    private final List<NamedRange> ranges = new ArrayList<>();
    private final List<RangeBand> bands = new ArrayList<>();
    private RangeBounds bounds;

    public NamedRangeGroupSetting(CharSequence label, NamedRange... ranges) {
        super(label);
        Collections.addAll(this.ranges, ranges);
    }

    public NamedRangeGroupSetting bounds(CharSequence label, float minimum, float maximum,
                                         float step, CharSequence unit) {
        bounds = new RangeBounds(label, minimum, maximum, step, unit);
        for (NamedRange range : ranges) range.constrain(range.getLower(), range.getUpper(), bounds);
        return this;
    }

    public NamedRangeGroupSetting band(CharSequence label, float lower, float upper) {
        bands.add(new RangeBand(label, lower, upper));
        return this;
    }

    public boolean hasBounds() { return bounds != null; }
    public List<RangeBand> getBands() { return Collections.unmodifiableList(bands); }
    public List<NamedRange> getRanges() { return Collections.unmodifiableList(ranges); }

    private static boolean sameRangeLabel(CharSequence first, CharSequence second) {
        return Concat.contentEquals(first, second);
    }

    public NamedRange findRange(CharSequence label) {
        for (NamedRange range : ranges) {
            if (sameRangeLabel(range.getLabel(), label)) return range;
        }
        return null;
    }

    public boolean isEnabled(CharSequence label) {
        NamedRange range = findRange(label);
        return range != null && range.isEnabled();
    }

    /** The upper bound is exclusive, except that the configured maximum means unbounded. */
    public boolean contains(NamedRange range, float value) {
        if (bounds == null) return true;
        if (value < range.getLower()) return false;
        return !(range.getUpper() < bounds.maximum()) || value < range.getUpper();
    }

    public boolean coversAll(NamedRange range) {
        return bounds == null || range.getLower() <= bounds.minimum()
                && !(range.getUpper() < bounds.maximum());
    }

    public void moveRange(int from, int to) {
        if (from < 0 || from >= ranges.size()) return;
        to = Math.max(0, Math.min(ranges.size() - 1, to));
        if (from == to) return;
        ranges.add(to, ranges.remove(from));
        notifyChanged();
    }

    public void toggleRange(int index) {
        NamedRange range = ranges.get(index);
        range.setEnabled(!range.isEnabled());
        notifyChanged();
    }

    public void setRange(NamedRange range, float first, float second) {
        if (bounds == null) return;
        range.constrain(first, second, bounds);
        notifyChanged();
    }

    public RangeBounds getBounds() { return bounds; }
    @Override public String getTypeId() { return "pri"; }
}

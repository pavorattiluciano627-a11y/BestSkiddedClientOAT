package dev.echo.client.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Clean immutable String-segment text for literal feature labels and defaults.
 * It preserves a non-String CharSequence and explicit segment comparison instead
 * of hiding strings behind hashes, runtime decryption, or dynamic member dispatch.
 * Additional legacy Concat subtype operations are not implemented by this class.
 */
public final class SegmentedText implements CharSequence {
    private final String[] segments;
    private final int[] offsets;
    private final int length;

    private SegmentedText(String[] segments) {
        this.segments = segments.clone();
        this.offsets = new int[segments.length];
        int total = 0;
        for (int i = 0; i < segments.length; i++) {
            offsets[i] = total;
            total = Math.addExact(total, Objects.requireNonNull(segments[i], "segment").length());
        }
        this.length = total;
    }

    /**
     * Creates a fresh text object with one segment per Unicode code point.
     * This is the established native string factory's shape, expressed with a
     * readable literal rather than a 64-bit lookup key.
     * @param text original Unicode text
     * @return a new non-String text value
     */
    public static SegmentedText literal(String text) {
        Objects.requireNonNull(text, "text");
        return new SegmentedText(text.codePoints().mapToObj(c -> new String(Character.toChars(c))).toArray(String[]::new));
    }

    /**
     * Copies an explicit String segment array, retaining its segment boundaries.
     * @param segments literal String parts in order
     * @return a new text object
     */
    public static SegmentedText ofParts(String... segments) {
        return new SegmentedText(Objects.requireNonNull(segments, "segments"));
    }

    /** @return total UTF-16 code-unit length, as required by CharSequence */
    @Override public int length() { return length; }

    /**
     * Reads a UTF-16 code unit without flattening the text.
     * @param index code-unit index
     * @return code unit at index
     */
    @Override public char charAt(int index) {
        Objects.checkIndex(index, length);
        for (int i = 0; i < segments.length; i++) {
            int end = offsets[i] + segments[i].length();
            if (index < end) return segments[i].charAt(index - offsets[i]);
        }
        throw new AssertionError("Validated index not covered by immutable segments");
    }

    /**
     * Slices overlapping segments, retaining a non-String result.
     * @param start inclusive UTF-16 index
     * @param end exclusive UTF-16 index
     * @return fresh segmented slice
     */
    @Override public SegmentedText subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            int segmentStart = offsets[i];
            int segmentEnd = segmentStart + segments[i].length();
            if (segmentEnd <= start || segmentStart >= end) continue;
            parts.add(segments[i].substring(Math.max(0, start - segmentStart), Math.min(segments[i].length(), end - segmentStart)));
        }
        return new SegmentedText(parts.toArray(String[]::new));
    }

    /** @return a flattened String only when a caller explicitly requests one */
    @Override public String toString() { return String.join("", segments); }

    /**
     * Compares String segments, matching the string-part Concat comparator's
     * Arrays.equals behavior. Equal flattened text need not have equal segments.
     * Object.equals/hashCode retain reference identity.
     * @param other another segmented value
     * @return whether both have the same segment contents and boundaries
     */
    public boolean hasSameSegments(SegmentedText other) {
        return other != null && Arrays.equals(segments, other.segments);
    }
}

package dev.echo.client.utils.strings;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Nonencrypted character sequences with content operations and representation-
 * specific equality. Segment objects remain live; offsets are fixed at construction.
 */
public abstract class Concat implements CharSequence {
    /** Joins string segments, retaining their segmentation for representation equality. */
    public static Concat strings(String... segments) { return new Strings(segments); }

    /** Joins arbitrary sequences, retaining references to the segment objects. */
    public static Concat sequences(CharSequence... segments) { return new Sequences(segments); }

    /** Copies a character array into a concatenation value. */
    public static Concat characters(char[] characters) { return new Chars(characters); }

    /** Uses Integer.toString's decimal conversion. */
    public static Concat integer(int value) { return new Chars(Integer.toString(value).toCharArray()); }

    /** Uses Float.toString's conversion, distinct from TextBuffers' fixed precision. */
    public static Concat floatingPoint(float value) { return new Chars(Float.toString(value).toCharArray()); }

    /** Uses Locale.ROOT's fixed-decimal formatter and its original precision exceptions. */
    public static Concat formattedFloat(float value, int decimalPlaces) {
        return new Chars(String.format(Locale.ROOT, "%." + decimalPlaces + "f", value).toCharArray());
    }

    /** Tests representation-specific equality, which is not Object.equals. */
    public abstract boolean sameRepresentation(Concat other);

    /** Returns a live, UTF-16-code-unit lowercase view. */
    public CharSequence lowercaseView() { return new Sequence(this); }

    /** Checks one index, preserving the original exception's index message. */
    protected void checkIndex(int index, int length) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException(index);
    }

    /** Checks a half-open range without adding an exception message. */
    protected void checkRange(int start, int end, int length) {
        if (start < 0 || end > length || start > end) throw new IndexOutOfBoundsException();
    }

    /** Compares content without a null or identity shortcut. */
    protected boolean sameCharacters(Concat other) {
        if (length() != other.length()) return false;
        for (int index = 0; index < length(); index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    /** Compares nullable sequences by UTF-16 content. */
    public static boolean contentEquals(CharSequence first, CharSequence second) {
        if (first == second) return true;
        if (first == null || second == null || first.length() != second.length()) return false;
        for (int index = 0; index < first.length(); index++) {
            if (first.charAt(index) != second.charAt(index)) return false;
        }
        return true;
    }

    /** Compares lowercase code units, then lengths; this is not String.compareToIgnoreCase. */
    public static int compareLowercase(CharSequence first, CharSequence second) {
        int limit = Math.min(first.length(), second.length());
        for (int index = 0; index < limit; index++) {
            int comparison = Character.compare(Character.toLowerCase(first.charAt(index)),
                                                Character.toLowerCase(second.charAt(index)));
            if (comparison != 0) return comparison;
        }
        return Integer.compare(first.length(), second.length());
    }

    /** Searches using per-code-unit lowercase conversion and the original null behavior. */
    public static boolean containsLowercase(CharSequence text, CharSequence query) {
        if (text == null || query == null) return false;
        if (query.isEmpty()) return true;
        int lastStart = text.length() - query.length();
        for (int start = 0; start <= lastStart; start++) {
            boolean matches = true;
            for (int offset = 0; offset < query.length(); offset++) {
                if (Character.toLowerCase(text.charAt(start + offset))
                        != Character.toLowerCase(query.charAt(offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }

    /**
     * Returns the first eight SHA-256 bytes as sixteen lowercase hex digits.
     * Unpaired surrogates are encoded as three bytes, rather than replaced with '?'.
     * Exceptions from the input sequence are swallowed, as in the original method.
     */
    public static String shortHash(CharSequence text) {
        if (text == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int index = 0, length = text.length(); index < length; index++) {
                char character = text.charAt(index);
                if (character < 128) {
                    digest.update((byte) character);
                } else if (character < 2048) {
                    digest.update((byte) (192 | character >> 6));
                    digest.update((byte) (128 | character & 63));
                } else if (Character.isHighSurrogate(character) && index + 1 < length
                        && Character.isLowSurrogate(text.charAt(index + 1))) {
                    int codePoint = Character.toCodePoint(character, text.charAt(++index));
                    digest.update((byte) (240 | codePoint >> 18));
                    digest.update((byte) (128 | codePoint >> 12 & 63));
                    digest.update((byte) (128 | codePoint >> 6 & 63));
                    digest.update((byte) (128 | codePoint & 63));
                } else {
                    digest.update((byte) (224 | character >> 12));
                    digest.update((byte) (128 | character >> 6 & 63));
                    digest.update((byte) (128 | character & 63));
                }
            }
            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder(16);
            for (int index = 0; index < 8; index++) {
                result.append(String.format("%02x", hash[index] & 255));
            }
            return result.toString();
        } catch (Exception exception) {
            return null;
        }
    }

    /** Lowercase view; record equality remains equality of the backing sequence. */
    record Sequence(CharSequence sequence) implements CharSequence {
        @Override public int length() { return sequence.length(); }
        @Override public char charAt(int index) { return Character.toLowerCase(sequence.charAt(index)); }
        @Override public CharSequence subSequence(int start, int end) {
            return new Sequence(sequence.subSequence(start, end));
        }
        @Override public String toString() {
            StringBuilder result = new StringBuilder(length());
            for (int index = 0; index < length(); index++) result.append(charAt(index));
            return result.toString();
        }
    }

    /** A copied character array with content-based representation comparison. */
    static class Chars extends Concat {
        private final char[] characters;
        Chars(char[] characters) { this.characters = characters.clone(); }
        @Override public int length() { return characters.length; }
        @Override public char charAt(int index) {
            checkIndex(index, characters.length);
            return characters[index];
        }
        @Override public String toString() { return new String(characters); }
        @Override public CharSequence subSequence(int start, int end) {
            checkRange(start, end, characters.length);
            return new Chars(Arrays.copyOfRange(characters, start, end));
        }
        @Override public boolean sameRepresentation(Concat other) {
            return other instanceof Chars chars
                    ? Arrays.equals(characters, chars.characters) : sameCharacters(other);
        }
    }

    /** A shallow copy of sequence segments, with offsets fixed at construction. */
    static class Sequences extends Concat {
        private final CharSequence[] segments;
        private final int[] offsets;
        private final int length;
        Sequences(CharSequence[] segments) {
            this.segments = segments.clone();
            offsets = new int[segments.length];
            int total = 0;
            for (int index = 0; index < segments.length; index++) {
                offsets[index] = total;
                total += segments[index].length();
            }
            length = total;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            checkIndex(index, length);
            for (int segment = 0; segment < segments.length; segment++) {
                if (index < offsets[segment] + segments[segment].length()) {
                    return segments[segment].charAt(index - offsets[segment]);
                }
            }
            throw new IndexOutOfBoundsException(index);
        }
        @Override public String toString() {
            StringBuilder result = new StringBuilder(length);
            for (CharSequence segment : segments) result.append(segment);
            return result.toString();
        }
        @Override public CharSequence subSequence(int start, int end) {
            checkRange(start, end, length);
            ArrayList<CharSequence> result = new ArrayList<>();
            for (int segment = 0; segment < segments.length; segment++) {
                int segmentStart = offsets[segment];
                int segmentEnd = segmentStart + segments[segment].length();
                if (segmentEnd <= start || segmentStart >= end) continue;
                result.add(segments[segment].subSequence(Math.max(0, start - segmentStart),
                        Math.min(segments[segment].length(), end - segmentStart)));
            }
            return new Sequences(result.toArray(CharSequence[]::new));
        }
        @Override public boolean sameRepresentation(Concat other) { return sameCharacters(other); }
    }

    /** String segments use binary-search offsets and segment-array equality. */
    static class Strings extends Concat {
        private final String[] segments;
        private final int[] offsets;
        private final int length;
        Strings(String[] segments) {
            this.segments = segments.clone();
            offsets = new int[segments.length];
            int total = 0;
            for (int index = 0; index < segments.length; index++) {
                offsets[index] = total;
                total += segments[index].length();
            }
            length = total;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            checkIndex(index, length);
            int segment = Arrays.binarySearch(offsets, index);
            if (segment < 0) segment = -segment - 2;
            while (segments[segment].isEmpty()) segment++;
            return segments[segment].charAt(index - offsets[segment]);
        }
        @Override public String toString() { return String.join("", segments); }
        @Override public CharSequence subSequence(int start, int end) {
            checkRange(start, end, length);
            ArrayList<String> result = new ArrayList<>();
            for (int segment = 0; segment < segments.length; segment++) {
                int segmentStart = offsets[segment];
                int segmentEnd = segmentStart + segments[segment].length();
                if (segmentEnd <= start || segmentStart >= end) continue;
                result.add(segments[segment].substring(Math.max(0, start - segmentStart),
                        Math.min(segments[segment].length(), end - segmentStart)));
            }
            return new Strings(result.toArray(String[]::new));
        }
        @Override public boolean sameRepresentation(Concat other) {
            return other instanceof Strings strings && Arrays.equals(segments, strings.segments);
        }
    }
}

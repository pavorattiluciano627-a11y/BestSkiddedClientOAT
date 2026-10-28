package dev.echo.client.utils.strings;

/** Integer, fixed-decimal, range, and ellipsis character-sequence buffers. */
public final class TextBuffers {
    // The original class has an implicit public constructor.

    /** Keeps up to limit characters and then appends three dots. */
    @Deprecated
    public static String abbreviateString(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit) + "..." : text;
    }

    /** Returns the input itself when it fits, otherwise a live abbreviated view. */
    public static CharSequence abbreviate(CharSequence text, int limit) {
        return text.length() <= limit ? text : new Slice(text, limit);
    }

    /** Formats an integer using the original fixed character buffer algorithm. */
    public static CharSequence integer(int value) { return new CharArrayView(value); }

    /** Formats a float at fixed precision, including a decimal point at precision zero. */
    public static CharSequence fixed(float value, int decimalPlaces) {
        return new MutableChars(value, decimalPlaces);
    }

    /** Formats two fixed-precision floats separated by one hyphen. */
    public static CharSequence range(float first, float second, int decimalPlaces) {
        return new JoinedBuffers(first, second, decimalPlaces);
    }

    /** Ellipsis view, not a substring: indices at or beyond the cutoff return '.'. */
    static final class Slice implements CharSequence {
        private final CharSequence text;
        private final int cutoff;
        private final int length;
        Slice(CharSequence text, int cutoff) {
            this.text = text;
            this.cutoff = cutoff;
            length = cutoff + 3;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { return index < cutoff ? text.charAt(index) : '.'; }
        @Override public CharSequence subSequence(int start, int end) {
            StringBuilder result = new StringBuilder(end - start);
            for (int index = start; index < end; index++) result.append(charAt(index));
            return result;
        }
        // No toString override: adding one would change the original API behavior.
    }

    /** Integer text buffer; the historical Integer.MIN_VALUE overflow is retained. */
    static final class CharArrayView implements CharSequence {
        private final char[] characters;
        private final int length;
        CharArrayView(int value) {
            boolean negative = value < 0;
            int magnitude = negative ? -value : value;
            int digits = negative ? 1 : 0;
            int remaining = magnitude;
            do {
                digits++;
                remaining /= 10;
            } while (remaining > 0);
            characters = new char[digits];
            int index = digits - 1;
            do {
                characters[index--] = (char) ('0' + magnitude % 10);
                magnitude /= 10;
            } while (magnitude > 0);
            if (negative) characters[0] = '-';
            length = digits;
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { return characters[index]; }
        @Override public CharSequence subSequence(int start, int end) {
            return new String(characters, start, end - start);
        }
    }

    /**
     * Fixed decimal buffer, despite the inherited taxonomy name MutableChars.
     * Uses float rounding and integer scaling, including the historical overflow
     * and zero-precision behavior. It is immutable despite the inherited type name.
     */
    static final class MutableChars implements CharSequence {
        private final char[] characters;
        private final int length;
        MutableChars(float value, int decimalPlaces) {
            int scale = 1;
            for (int digit = 0; digit < decimalPlaces; digit++) scale *= 10;
            long rounded = Math.round(value * scale);
            boolean negative = rounded < 0;
            if (negative) rounded = -rounded;
            long integral = rounded / scale;
            long fractional = rounded % scale;
            int integralDigits = 1;
            for (long remaining = integral; remaining >= 10; remaining /= 10) integralDigits++;
            length = (negative ? 1 : 0) + integralDigits + 1 + decimalPlaces;
            characters = new char[length];
            int index = length - 1;
            for (int digit = decimalPlaces - 1; digit >= 0; digit--) {
                characters[index--] = (char) ('0' + fractional % 10);
                fractional /= 10;
            }
            characters[index--] = '.';
            do {
                characters[index--] = (char) ('0' + integral % 10);
                integral /= 10;
            } while (integral > 0);
            if (negative) characters[index] = '-';
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { return characters[index]; }
        @Override public CharSequence subSequence(int start, int end) {
            return new String(characters, start, end - start);
        }
    }

    /** A pair of decimal buffers joined by a hyphen, with no toString override. */
    static final class JoinedBuffers implements CharSequence {
        private final MutableChars first;
        private final MutableChars second;
        private final int length;
        JoinedBuffers(float first, float second, int decimalPlaces) {
            this.first = new MutableChars(first, decimalPlaces);
            this.second = new MutableChars(second, decimalPlaces);
            length = this.first.length() + 1 + this.second.length();
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            if (index < first.length()) return first.charAt(index);
            if (index == first.length()) return '-';
            return second.charAt(index - first.length() - 1);
        }
        @Override public CharSequence subSequence(int start, int end) {
            StringBuilder result = new StringBuilder(end - start);
            for (int index = start; index < end; index++) result.append(charAt(index));
            return result;
        }
    }
}

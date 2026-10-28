package dev.echo.client.ui;

/**
 * Hierarchical UI identity with UTF-8 FNV-1a hashing and the original mutable
 * empty-check compatibility state. See the recovery receipt for binding and
 * class-initialization applicability limits.
 */
public record UiId(long value) {
    private static final long FNV_OFFSET_BASIS = -3750763034362895579L;
    private static final long FNV_PRIME = 1099511628211L;

    public static final UiId NONE;
    public static final UiId ROOT;
    private static int emptyCheckState;

    static {
        if (emptyCheckGuard() != 0) {
            setEmptyCheckState(57);
        }
        NONE = new UiId(0L);
        ROOT = new UiId(FNV_OFFSET_BASIS);
    }

    /** Hashes a child label as UTF-8, followed by a 0xff segment delimiter. */
    public UiId child(CharSequence label) {
        long hash = value == 0L ? FNV_OFFSET_BASIS : value;
        for (int index = 0; index < label.length(); index++) {
            char character = label.charAt(index);
            if (character < 0x80) {
                hash = appendByte(hash, character);
            } else if (character < 0x800) {
                hash = appendByte(hash, 0xc0 | character >> 6);
                hash = appendByte(hash, 0x80 | character & 0x3f);
            } else if (Character.isHighSurrogate(character) && index + 1 < label.length()
                    && Character.isLowSurrogate(label.charAt(index + 1))) {
                int codePoint = Character.toCodePoint(character, label.charAt(++index));
                hash = appendByte(hash, 0xf0 | codePoint >> 18);
                hash = appendByte(hash, 0x80 | codePoint >> 12 & 0x3f);
                hash = appendByte(hash, 0x80 | codePoint >> 6 & 0x3f);
                hash = appendByte(hash, 0x80 | codePoint & 0x3f);
            } else if (Character.isSurrogate(character)) {
                hash = appendByte(hash, '?');
            } else {
                hash = appendByte(hash, 0xe0 | character >> 12);
                hash = appendByte(hash, 0x80 | character >> 6 & 0x3f);
                hash = appendByte(hash, 0x80 | character & 0x3f);
            }
        }
        hash = (hash ^ 0xffL) * FNV_PRIME;
        return new UiId(hash == 0L ? 1L : hash);
    }

    private static long appendByte(long hash, int value) {
        return (hash ^ (value & 0xffL)) * FNV_PRIME;
    }

    public UiId child(int index) {
        return child(Integer.toString(index));
    }

    /** The original zero state reverses this predicate; it is not assumed absent. */
    public boolean isEmpty() {
        int guard = emptyCheckGuard();
        int comparison = Long.compare(value, 0L);
        return guard != 0 ? comparison != 0 : comparison == 0;
    }

    /** Retains the original public state write, including zero and negative values. */
    public static void setEmptyCheckState(int state) {
        emptyCheckState = state;
    }

    public static int getEmptyCheckState() {
        return emptyCheckState;
    }

    /** Exact marker shared by the original UI-state consumers. */
    public static int emptyCheckGuard() {
        return getEmptyCheckState() == 0 ? 55 : 0;
    }
}

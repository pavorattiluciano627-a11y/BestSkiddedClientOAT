package dev.echo.client.features;

import dev.echo.client.utils.strings.Concat;

/** Creates the original fresh, code-point-segmented literal representation. */
public final class FeatureLiterals {
    private FeatureLiterals() {
    }

    public static Concat literal(String text) {
        return Concat.strings(text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toArray(String[]::new));
    }
}

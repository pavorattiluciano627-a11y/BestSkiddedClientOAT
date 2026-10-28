package dev.echo.client.bootstrap;

import dev.echo.client.utils.strings.Concat;

/** Creates fresh code-point-segmented Concat values from explicit source literals. */
public final class CatalogText {
    private CatalogText() {
    }

    /**
     * Preserves String-segment representation and fresh wrapper identity.
     * @param text the plaintext source literal
     * @return a fresh Concat containing one String segment per Unicode code point
     */
    public static Concat literal(String text) {
        return Concat.strings(text.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toArray(String[]::new));
    }
}

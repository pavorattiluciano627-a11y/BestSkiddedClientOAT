package dev.echo.client.screens.panels;

/** Direct string operations recovered from the friend-name editor. */
public final class PanelTextEditing {
    private PanelTextEditing() {
    }

    /**
     * Inserts the original UTF-16 character at a bounded cursor position.
     *
     * @param text the text being edited
     * @param cursor the proposed UTF-16 insertion index
     * @param character the character to insert
     * @return the updated text
     */
    public static String insertCharacter(String text, int cursor, char character) {
        int insertionIndex = Math.clamp(cursor, 0, text.length());
        return text.substring(0, insertionIndex) + character + text.substring(insertionIndex);
    }
}

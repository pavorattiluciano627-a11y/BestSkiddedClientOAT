package dev.echo.client.screens.panels;

/** Shared, source-recovered geometry calculations used by the original panels. */
public final class PanelGeometry {
    private static final float SCRIPT_ROW_HEIGHT = 44.0F;
    private static final float ROW_GAP = 6.0F;
    private static final float CONFIG_CARD_HEIGHT = 72.0F;
    private static final float CONFIG_COLUMNS = 3.0F;

    private PanelGeometry() {
    }

    /**
     * Constrains a panel's top or left coordinate to the available viewport.
     *
     * @param position the proposed coordinate
     * @param panelExtent the panel width or height
     * @param viewportExtent the corresponding viewport size
     * @return the constrained coordinate, retaining {@link Math#clamp(float, float, float)} semantics
     */
    public static float clampPosition(float position, float panelExtent, int viewportExtent) {
        return Math.clamp(position, 0.0F, Math.max(0.0F, viewportExtent - panelExtent));
    }

    /**
     * Computes the script-list height, with gaps between rows only.
     *
     * @param rowCount the row count supplied by the panel
     * @return the original floating-point layout result
     */
    public static float scriptListHeight(int rowCount) {
        return rowCount * SCRIPT_ROW_HEIGHT + Math.max(0, rowCount - 1) * ROW_GAP;
    }

    /**
     * Computes the height of the three-column configuration grid.
     *
     * @param configCount the number of configurations
     * @return the grid height, retaining the original float division before ceiling
     */
    public static float configGridHeight(int configCount) {
        int rowCount = (int) Math.ceil(configCount / CONFIG_COLUMNS);
        float cardsHeight = rowCount * CONFIG_CARD_HEIGHT;
        float gapsHeight = Math.max(0, rowCount - 1) * ROW_GAP;
        return cardsHeight + gapsHeight;
    }

    /**
     * Computes the squared distance used to compare panel positions.
     *
     * @param x the first point's x coordinate
     * @param y the first point's y coordinate
     * @param otherX the second point's x coordinate
     * @param otherY the second point's y coordinate
     * @return the squared Euclidean distance without a square root
     */
    public static float squaredDistance(float x, float y, float otherX, float otherY) {
        float deltaX = x - otherX;
        float deltaY = y - otherY;
        return deltaX * deltaX + deltaY * deltaY;
    }
}

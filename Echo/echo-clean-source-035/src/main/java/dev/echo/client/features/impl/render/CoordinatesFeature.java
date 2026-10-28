package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudIcon;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.render.FontAsset;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.ui.UiSize;

/** The original three-axis coordinate panel. */
public class CoordinatesFeature extends EchoHudElement {
    public CoordinatesFeature() {
        super(new FeatureInfo(CatalogText.literal("Coordinates"),
                CatalogText.literal("Current X, Y and Z position"), Category.RENDER),
                HudElementType.COORDINATES);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.COORDINATES.defaultX();
        float y = HudElementType.COORDINATES.defaultY();
        hud.drawPanel(layout, x, y, layout.getWidth(), 41.0f, 10.0f, 1.0f, 14.0f);
        layout.drawIcon(HudIcon.GLOBAL, x + 12.0f, y + 13.5f, 14.0f, 14.0f, -1, 1.0f);
        float nextX = x + 38.0f;
        nextX = drawCoordinate(layout, hud.getCoordinateXText(), "X", nextX, y) + 8.0f;
        nextX = drawCoordinate(layout, hud.getCoordinateYText(), "Y", nextX, y) + 8.0f;
        drawCoordinate(layout, hud.getCoordinateZText(), "Z", nextX, y);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float width = 38.0f
                + measureCoordinate(layout, hud.getCoordinateXText(), "X") + 8.0f
                + measureCoordinate(layout, hud.getCoordinateYText(), "Y") + 8.0f
                + measureCoordinate(layout, hud.getCoordinateZText(), "Z") + 14.0f;
        return new UiSize(HudLayout.roundWidth(Math.max(HudElementType.COORDINATES.defaultWidth(), width)), 41.0f);
    }

    private static float drawCoordinate(HudElementLayout layout, String value, String axis,
                                         float x, float y) {
        layout.drawText(FontAsset.INTER, value, x, y + 13.0f, 13.0f, -1, 1.0f);
        x += layout.textWidth(FontAsset.INTER, value, 13.0f) + 4.0f;
        layout.drawText(FontAsset.INTER, axis, x, y + 13.5f, 11.5f, -1929379841, 1.0f);
        return x + layout.textWidth(FontAsset.INTER, axis, 11.5f);
    }

    private static float measureCoordinate(HudElementLayout layout, String value, String axis) {
        return layout.textWidth(FontAsset.INTER, value, 13.0f) + 4.0f
                + layout.textWidth(FontAsset.INTER, axis, 11.5f);
    }
}

package dev.echo.client.setting;

import dev.echo.client.setting.ui.SettingWidgetRenderer;
import java.util.function.IntSupplier;
import dev.echo.client.hud.ItemHudLayout;

/** A live rendering callback with dynamic layout columns. */
public class CustomWidgetSetting extends Setting {
    private final SettingWidgetRenderer renderer;
    private final float height;
    private final IntSupplier columnCount;
    private int layoutIndex;

    public CustomWidgetSetting(CharSequence label, float height, SettingWidgetRenderer renderer,
                               IntSupplier columnCount) {
        super(label);
        this.height = height;
        this.renderer = renderer;
        this.columnCount = columnCount;
    }

    public CustomWidgetSetting(CharSequence label, float height, SettingWidgetRenderer renderer) {
        this(label, height, renderer, null);
    }

    public int getColumnCount() { return columnCount == null ? 1 : Math.max(1, columnCount.getAsInt()); }
    public void render(ItemHudLayout context) { renderer.render(context); }
    public float getHeight() { return height; }
    public int getLayoutIndex() { return layoutIndex; }
    public void setLayoutIndex(int index) { layoutIndex = index; }
    @Override public String getTypeId() { return "preview"; }
}

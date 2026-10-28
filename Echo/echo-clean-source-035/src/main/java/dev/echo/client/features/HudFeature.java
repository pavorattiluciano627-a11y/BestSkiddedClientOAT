package dev.echo.client.features;

import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import dev.echo.client.bootstrap.CatalogText;

/** Screen-relative HUD positioning with legacy edge anchors and nine-point anchors. */
public abstract class HudFeature extends Feature {
    public static final CharSequence LEFT = CatalogText.literal("Left");
    public static final CharSequence RIGHT = CatalogText.literal("Right");
    public static final CharSequence TOP = CatalogText.literal("Top");
    public static final CharSequence BOTTOM = CatalogText.literal("Bottom");
    public static final CharSequence CENTER = CatalogText.literal("Center");
    public static final CharSequence NONE = CatalogText.literal("None");
    public static final CharSequence TOP_LEFT = CatalogText.literal("Top Left");
    public static final CharSequence TOP_CENTER = CatalogText.literal("Top Center");
    public static final CharSequence TOP_RIGHT = CatalogText.literal("Top Right");
    public static final CharSequence MIDDLE_LEFT = CatalogText.literal("Middle Left");
    public static final CharSequence MIDDLE = CatalogText.literal("Middle");
    public static final CharSequence MIDDLE_RIGHT = CatalogText.literal("Middle Right");
    public static final CharSequence BOTTOM_LEFT = CatalogText.literal("Bottom Left");
    public static final CharSequence BOTTOM_CENTER = CatalogText.literal("Bottom Center");
    public static final CharSequence BOTTOM_RIGHT = CatalogText.literal("Bottom Right");

    private final FloatSetting relativeX;
    private final FloatSetting relativeY;
    private final ChoiceSetting anchor;

    public HudFeature(FeatureInfo info) {
        super(info);
        relativeX = new FloatSetting(CatalogText.literal("Hud Module X Position"),
                0.01f, 0.0f, 1.0f, 0.01f);
        relativeY = new FloatSetting(CatalogText.literal("Hud Module Y Position"),
                0.01f, 0.0f, 1.0f, 0.01f);
        anchor = new ChoiceSetting(CatalogText.literal("Hud Module Anchor"), NONE,
                LEFT, RIGHT, TOP, BOTTOM, CENTER, NONE,
                TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE, MIDDLE_RIGHT,
                BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT);
    }

    public abstract float getWidth();
    public abstract float getHeight();

    @Override
    public void postInitialize() {
        relativeX.setVisibilityPredicate(context -> false);
        relativeY.setVisibilityPredicate(context -> false);
        anchor.setVisibilityPredicate(context -> false);
    }

    public float getX() {
        float screenWidth = viewportWidth();
        float width = getWidth();
        if (screenWidth <= width) {
            return 0.0f;
        }
        float x;
        if (!hasLegacyAnchor()) {
            x = relativeX.getValue() * screenWidth - getHorizontalAnchor() * width;
        } else if (anchor.isSelected(LEFT)) {
            x = 0.0f;
        } else if (anchor.isSelected(RIGHT)) {
            x = screenWidth - width;
        } else if (anchor.isSelected(CENTER)) {
            x = (screenWidth - width) * 0.5f;
        } else {
            x = relativeX.getValue() * screenWidth;
        }
        return Mth.clamp(x, 0.0f, screenWidth - width);
    }

    public float getY() {
        float screenHeight = viewportHeight();
        float height = getHeight();
        if (screenHeight <= height) {
            return 0.0f;
        }
        float y;
        if (!hasLegacyAnchor()) {
            y = relativeY.getValue() * screenHeight - getVerticalAnchor() * height;
        } else if (anchor.isSelected(TOP)) {
            y = 0.0f;
        } else if (anchor.isSelected(BOTTOM)) {
            y = screenHeight - height;
        } else if (anchor.isSelected(CENTER)) {
            y = (screenHeight - height) * 0.5f;
        } else {
            y = relativeY.getValue() * screenHeight;
        }
        return Mth.clamp(y, 0.0f, screenHeight - height);
    }

    public float getHorizontalAnchor() {
        if (anchor.isSelected(RIGHT) || anchor.isSelected(TOP_RIGHT)
                || anchor.isSelected(MIDDLE_RIGHT) || anchor.isSelected(BOTTOM_RIGHT)) {
            return 1.0f;
        }
        if (anchor.isSelected(CENTER) || anchor.isSelected(TOP_CENTER)
                || anchor.isSelected(MIDDLE) || anchor.isSelected(BOTTOM_CENTER)) {
            return 0.5f;
        }
        return 0.0f;
    }

    public float getVerticalAnchor() {
        if (anchor.isSelected(BOTTOM) || anchor.isSelected(BOTTOM_LEFT)
                || anchor.isSelected(BOTTOM_CENTER) || anchor.isSelected(BOTTOM_RIGHT)) {
            return 1.0f;
        }
        if (anchor.isSelected(CENTER) || anchor.isSelected(MIDDLE_LEFT)
                || anchor.isSelected(MIDDLE) || anchor.isSelected(MIDDLE_RIGHT)) {
            return 0.5f;
        }
        return 0.0f;
    }

    private boolean hasLegacyAnchor() {
        return anchor.isSelected(NONE) || anchor.isSelected(LEFT) || anchor.isSelected(RIGHT)
                || anchor.isSelected(TOP) || anchor.isSelected(BOTTOM) || anchor.isSelected(CENTER);
    }

    public void setPosition(float x, float y) {
        configurePosition(x, y, getWidth(), getHeight(), viewportWidth(), viewportHeight(), true);
    }

    protected void configurePosition(float x, float y, float width, float height,
                                     float screenWidth, float screenHeight, boolean notifyChanges) {
        int column = classifyScreenThird(x + width * 0.5f, screenWidth);
        int row = classifyScreenThird(y + height * 0.5f, screenHeight);
        CharSequence selectedAnchor;
        if (row == 0) {
            selectedAnchor = column == 0 ? TOP_LEFT : column == 1 ? TOP_CENTER : TOP_RIGHT;
        } else if (row == 1) {
            selectedAnchor = column == 0 ? MIDDLE_LEFT : column == 1 ? MIDDLE : MIDDLE_RIGHT;
        } else {
            selectedAnchor = column == 0 ? BOTTOM_LEFT : column == 1 ? BOTTOM_CENTER : BOTTOM_RIGHT;
        }
        // Anchor changes notify even when the position values are installed silently.
        anchor.setValue(selectedAnchor);
        setPositionValue(relativeX, (x + column * 0.5f * width) / screenWidth, notifyChanges);
        setPositionValue(relativeY, (y + row * 0.5f * height) / screenHeight, notifyChanges);
    }

    private static int classifyScreenThird(float coordinate, float extent) {
        if (coordinate < extent / 3.0f) {
            return 0;
        }
        if (coordinate > extent * 2.0f / 3.0f) {
            return 2;
        }
        return 1;
    }

    private static void setPositionValue(FloatSetting setting, float value, boolean notifyChanges) {
        if (notifyChanges) {
            setting.setValue(value);
        } else {
            setting.setValueSilently(value);
        }
    }

    public FloatSetting getRelativeX() { return relativeX; }
    public FloatSetting getRelativeY() { return relativeY; }
    public ChoiceSetting getAnchor() { return anchor; }

    private static int viewportWidth() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getWidth() / 2.0);
    }

    private static int viewportHeight() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getHeight() / 2.0);
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(HudFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.relativeX);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.relativeY);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.anchor);
    }
}

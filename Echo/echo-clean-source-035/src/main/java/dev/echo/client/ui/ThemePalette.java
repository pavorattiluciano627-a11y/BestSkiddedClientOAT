package dev.echo.client.ui;

import java.awt.Color;

/**
 * Shared mutable UI style state and the original color/hit-test operations.
 * Bootstrap reads return the current shared objects; live drawing reads these
 * same fields and supplies its own interpolation/opacity factors.
 * Original binding and control-state applicability is recorded separately.
 */
public class ThemePalette {
    public static float PANEL_WIDTH;
    public static float PANEL_HEADER_HEIGHT;
    public static float PANEL_SPACING;
    public static float SCREEN_PADDING;
    public static float PREFERRED_PANEL_HEIGHT;
    public static float PANEL_BORDER_WIDTH;
    public static float PANEL_CORNER_RADIUS;
    public static Color primaryColor;
    public static Color secondaryColor;
    public static float LABEL_TEXT_SIZE;
    public static float FEATURE_ROW_HEIGHT;
    public static float CATEGORY_ROW_GAP;
    public static float ROW_HEIGHT;
    public static float SLIDER_ROW_HEIGHT;
    public static float SEARCH_GAP;
    public static float CONTENT_PADDING;
    public static float SLOT_RADIUS;
    public static float ROW_RADIUS;
    public static float CATEGORY_BODY_RADIUS;
    public static float TOGGLE_WIDTH;
    public static float TOGGLE_HEIGHT;
    public static float TOGGLE_KNOB_SIZE;
    public static float COLOR_PICKER_HEIGHT;
    public static float CONTROL_SPACING;
    public static float SHORT_TRANSITION_SECONDS;
    public static float LONG_TRANSITION_SECONDS;
    public static Color PANEL_SHADOW;
    private static final Color BASE_SOFT_WHITE_170;
    private static final Color BASE_WHITE_120;
    private static final Color BASE_WHITE_180;
    private static final Color BASE_WHITE_220;
    private static final Color BASE_COOL_GRAY_200;
    private static final Color BASE_SOFT_WHITE_140;
    private static final Color BASE_WHITE_200;
    private static final Color BASE_NEUTRAL_GRAY_100;
    private static final Color BASE_WHITE_250;
    private static final Color BASE_WHITE_100;
    private static final Color BASE_NEUTRAL_GRAY_160;
    private static final Color BASE_WHITE_40;
    private static final Color BASE_WHITE_70;
    private static final Color BASE_WHITE_30;
    private static final Color BASE_INK_240;
    private static final Color BASE_INK_176;
    private static final Color BASE_INK_112;
    public static Color PANEL_FOREGROUND;
    public static Color TOOLTIP_BACKGROUND;
    public static Color DIM_FOREGROUND;
    public static Color MEDIUM_FOREGROUND;
    public static Color BRIGHT_FOREGROUND;
    public static Color TOOLTIP_BORDER;
    public static Color SLIDER_FILL;
    public static Color SLOT_BACKGROUND;
    public static Color TOGGLE_KNOB;
    public static Color FAINT_FOREGROUND;
    public static Color TOGGLE_OFF;
    public static Color TOGGLE_ON;
    public static Color KNOB_SHADOW;
    public static Color ROW_BACKGROUND;
    public static Color ROW_HOVER;
    public static Color ROW_ACCENT;
    public static Color REGISTRY_BACKGROUND;
    public static Color REGISTRY_SURFACE;
    public static Color FEATURE_CARD_BACKGROUND;
    public static float ROW_INDENT;
    public static float MAX_PANEL_HEIGHT;
    public static float SCROLL_SPEED;
    public static float HALF_PIXEL;
    public static float VISIBLE_ANIMATION_THRESHOLD;
    public static float ANIMATION_EPSILON;
    public static float INPUT_ANIMATION_THRESHOLD;
    public static Color SELECTED_OPTION_TEXT;
    public static Color PRIMARY_TEXT;
    public static Color SECONDARY_TEXT;
    public static Color TAB_ACTIVE_TEXT;
    public static Color foregroundColor;
    public static Object activeInteractionOwner;

    // Preserve the original functional allocation, field-read and publication order.
    static {
        PANEL_WIDTH = 120.0f;
        PANEL_HEADER_HEIGHT = 18.0f;
        PANEL_SPACING = 8.0f;
        SCREEN_PADDING = 15.0f;
        PREFERRED_PANEL_HEIGHT = 100.0f;
        PANEL_BORDER_WIDTH = 3.0f;
        PANEL_CORNER_RADIUS = 6.0f;
        primaryColor = new Color(0, 150, 255, 255);
        secondaryColor = new Color(10, 40, 120, 255);
        LABEL_TEXT_SIZE = 12.0f;
        FEATURE_ROW_HEIGHT = 16.0f;
        CATEGORY_ROW_GAP = 2.0f;
        ROW_HEIGHT = 16.0f;
        SLIDER_ROW_HEIGHT = 24.0f;
        SEARCH_GAP = 2.0f;
        CONTENT_PADDING = 5.0f;
        SLOT_RADIUS = 2.0f;
        ROW_RADIUS = 4.0f;
        CATEGORY_BODY_RADIUS = 6.0f;
        TOGGLE_WIDTH = 18.0f;
        TOGGLE_HEIGHT = 9.0f;
        TOGGLE_KNOB_SIZE = 6.0f;
        COLOR_PICKER_HEIGHT = 80.0f;
        CONTROL_SPACING = 5.0f;
        SHORT_TRANSITION_SECONDS = 0.15f;
        LONG_TRANSITION_SECONDS = 0.2f;
        PANEL_SHADOW = new Color(0, 0, 0, 20);
        BASE_SOFT_WHITE_170 = new Color(248, 248, 248, 170);
        BASE_WHITE_120 = new Color(255, 255, 255, 120);
        BASE_WHITE_180 = new Color(255, 255, 255, 180);
        BASE_WHITE_220 = new Color(255, 255, 255, 220);
        BASE_COOL_GRAY_200 = new Color(180, 180, 190, 200);
        BASE_SOFT_WHITE_140 = new Color(248, 248, 248, 140);
        BASE_WHITE_200 = new Color(255, 255, 255, 200);
        BASE_NEUTRAL_GRAY_100 = new Color(174, 174, 178, 100);
        BASE_WHITE_250 = new Color(255, 255, 255, 250);
        BASE_WHITE_100 = new Color(255, 255, 255, 100);
        BASE_NEUTRAL_GRAY_160 = new Color(174, 174, 178, 160);
        BASE_WHITE_40 = new Color(255, 255, 255, 40);
        BASE_WHITE_70 = new Color(255, 255, 255, 70);
        BASE_WHITE_30 = new Color(255, 255, 255, 30);
        BASE_INK_240 = new Color(32, 32, 48, 240);
        BASE_INK_176 = new Color(48, 48, 64, 176);
        BASE_INK_112 = new Color(80, 80, 96, 112);
        PANEL_FOREGROUND = BASE_SOFT_WHITE_170;
        TOOLTIP_BACKGROUND = BASE_SOFT_WHITE_140;
        DIM_FOREGROUND = BASE_WHITE_120;
        MEDIUM_FOREGROUND = BASE_WHITE_180;
        BRIGHT_FOREGROUND = BASE_WHITE_220;
        TOOLTIP_BORDER = BASE_COOL_GRAY_200;
        SLIDER_FILL = BASE_WHITE_200;
        SLOT_BACKGROUND = BASE_NEUTRAL_GRAY_100;
        TOGGLE_KNOB = BASE_WHITE_250;
        FAINT_FOREGROUND = BASE_WHITE_100;
        TOGGLE_OFF = BASE_NEUTRAL_GRAY_160;
        TOGGLE_ON = new Color(0, 122, 255, 180);
        KNOB_SHADOW = new Color(0, 0, 0, 40);
        ROW_BACKGROUND = BASE_WHITE_40;
        ROW_HOVER = BASE_WHITE_70;
        ROW_ACCENT = new Color(0, 122, 255, 120);
        REGISTRY_BACKGROUND = BASE_WHITE_30;
        REGISTRY_SURFACE = BASE_WHITE_30;
        FEATURE_CARD_BACKGROUND = BASE_WHITE_30;
        ROW_INDENT = 4.0f;
        MAX_PANEL_HEIGHT = 400.0f;
        SCROLL_SPEED = 6.7f;
        HALF_PIXEL = 0.5f;
        VISIBLE_ANIMATION_THRESHOLD = 0.01f;
        ANIMATION_EPSILON = 0.001f;
        INPUT_ANIMATION_THRESHOLD = 0.9f;
        SELECTED_OPTION_TEXT = BASE_INK_240;
        PRIMARY_TEXT = BASE_INK_176;
        SECONDARY_TEXT = BASE_INK_112;
        TAB_ACTIVE_TEXT = new Color(255, 255, 255, 240);
        foregroundColor = new Color(0, 122, 255, 40);
        activeInteractionOwner = null;
    }

    /** Fresh Color interpolation with the original component reads and truncation. */
    public static Color blend(Color from, Color to, float amount) {
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * amount),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * amount),
                (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * amount));
    }

    public static int replaceAlpha(int argb, int alpha) {
        return argb & 0x00ffffff | alpha << 24;
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static boolean contains(float x, float y, float width, float height, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /** Original truncating alpha multiplication: deliberately no clamping or rounding. */
    public static int multiplyAlpha(int argb, float opacity) {
        return (int) ((argb >>> 24) * opacity) << 24 | argb & 0x00ffffff;
    }

    public static int toArgb(Color color) {
        return color.getRGB();
    }

    public static int multiplyAlpha(Color color, float opacity) {
        return multiplyAlpha(color.getRGB(), opacity);
    }

    /** Reads the shared foregroundColor default without copying its value. */
    public static java.awt.Color bootstrapForegroundColor() {
        return foregroundColor;
    }

    /** Reads the shared primaryColor default without copying its value. */
    public static java.awt.Color bootstrapPrimaryColor() {
        return primaryColor;
    }

    /** Reads the shared secondaryColor default without copying its value. */
    public static java.awt.Color bootstrapSecondaryColor() {
        return secondaryColor;
    }
}

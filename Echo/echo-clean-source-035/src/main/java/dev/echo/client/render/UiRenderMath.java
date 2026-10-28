package dev.echo.client.render;

import net.minecraft.util.math.ColorHelper;
import org.joml.Vector4i;

/** Exact color and scissor operations extracted from the UI renderer. */
public final class UiRenderMath {
    private UiRenderMath() {
    }

    /** Keeps alpha and rounds each quarter-brightness RGB channel independently. */
    public static int textShadowColor(int argb) {
        return ColorHelper.getArgb(
                ColorHelper.getAlpha(argb),
                Math.round(ColorHelper.getRed(argb) * 0.25F),
                Math.round(ColorHelper.getGreen(argb) * 0.25F),
                Math.round(ColorHelper.getBlue(argb) * 0.25F));
    }

    /** The original UI uses a fixed two-pixel scale, independent of window scale. */
    public static Vector4i scissorPixels(float x, float y, float width, float height) {
        float scale = 2.0F;
        int left = (int) Math.floor(x * scale);
        int top = (int) Math.floor(y * scale);
        int right = (int) Math.ceil((x + width) * scale);
        int bottom = (int) Math.ceil((y + height) * scale);
        return new Vector4i(left, top, right - left, bottom - top);
    }

    /** Returns a new intersection, leaving both input rectangles untouched. */
    public static Vector4i intersectScissors(Vector4i first, Vector4i second) {
        int firstRight = first.x + first.z;
        int firstBottom = first.y + first.w;
        int secondRight = second.x + second.z;
        int secondBottom = second.y + second.w;
        int left = Math.max(first.x, second.x);
        int top = Math.max(first.y, second.y);
        int right = Math.min(firstRight, secondRight);
        int bottom = Math.min(firstBottom, secondBottom);
        return new Vector4i(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}

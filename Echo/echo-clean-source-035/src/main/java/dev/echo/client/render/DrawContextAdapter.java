package dev.echo.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Candidate: direct field targets are conditional; see needs.json. */
public final class DrawContextAdapter {
    private final DrawContext context;

    private DrawContextAdapter(DrawContext context) {
        this.context = context;
    }

    public static DrawContextAdapter wrap(DrawContext context) {
        return context == null ? null : new DrawContextAdapter(context);
    }

    public int width() { return context.getScaledWindowWidth(); }
    public int height() { return context.getScaledWindowHeight(); }
    public void pushMatrix() { context.getMatrices().pushMatrix(); }
    public void popMatrix() { context.getMatrices().popMatrix(); }

    public void translate(float x, float y) {
        context.getMatrices().translate(x, y);
    }

    public void drawItem(ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
    }

    public void drawStackOverlay(TextRenderer textRenderer, ItemStack stack, int x, int y) {
        context.drawStackOverlay(textRenderer, stack, x, y);
    }

    public void drawText(TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
        context.drawText(textRenderer, text, x, y, color, shadow);
    }

    public void addEntity(EntityRenderState state, float scale, Vector3f translation,
                          Quaternionf rotation, Quaternionf overrideCameraAngle,
                          int x1, int y1, int x2, int y2) {
        context.addEntity(state, scale, translation, rotation, overrideCameraAngle, x1, y1, x2, y2);
    }
}

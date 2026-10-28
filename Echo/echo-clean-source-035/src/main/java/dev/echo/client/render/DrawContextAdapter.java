package dev.echo.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Candidate: direct field targets are conditional; see needs.json. */
public final class DrawContextAdapter {
    private final GuiGraphicsExtractor context;

    private DrawContextAdapter(GuiGraphicsExtractor context) {
        this.context = context;
    }

    public static DrawContextAdapter wrap(GuiGraphicsExtractor context) {
        return context == null ? null : new DrawContextAdapter(context);
    }

    public int width() { return context.guiWidth(); }
    public int height() { return context.guiHeight(); }
    public void pushMatrix() { context.pose().pushMatrix(); }
    public void popMatrix() { context.pose().popMatrix(); }

    public void translate(float x, float y) {
        context.pose().translate(x, y);
    }

    public void drawItem(ItemStack stack, int x, int y) {
        context.item(stack, x, y);
    }

    public void drawStackOverlay(Font textRenderer, ItemStack stack, int x, int y) {
        context.itemDecorations(textRenderer, stack, x, y);
    }

    public void drawText(Font textRenderer, Component text, int x, int y, int color, boolean shadow) {
        context.text(textRenderer, text, x, y, color, shadow);
    }

    public void addEntity(EntityRenderState state, float scale, Vector3f translation,
                          Quaternionf rotation, Quaternionf overrideCameraAngle,
                          int x1, int y1, int x2, int y2) {
        context.entity(state, scale, translation, rotation, overrideCameraAngle, x1, y1, x2, y2);
    }
}

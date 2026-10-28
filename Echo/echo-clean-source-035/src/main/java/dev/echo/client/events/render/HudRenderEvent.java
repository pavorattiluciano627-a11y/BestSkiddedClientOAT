package dev.echo.client.events.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.echo.client.events.Event;
import dev.echo.client.render.DrawContextAdapter;
import dev.echo.client.render.UiRenderer;
import dev.echo.render.texture.TextureView;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/** HUD render payload. Analysis candidate: linkage and shared renderer names await merge. */
public class HudRenderEvent extends Event {
    private PoseStack matrices;
    private float tickDelta;
    private DrawContextAdapter drawContext;
    private UiRenderer renderer;
    private TextureView texture;

    /** Captures the render frame's original references and interpolation value. */
    public HudRenderEvent(PoseStack matrices, float tickDelta, DrawContextAdapter drawContext,
                          UiRenderer renderer, TextureView texture) {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
        this.drawContext = drawContext;
        this.renderer = renderer;
        this.texture = texture;
    }

    /** Returns the GUI width using the original fixed factor of two and upward rounding. */
    public int getWidth() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getWidth() / 2.0);
    }

    /** Returns a fresh identity matrix, independently of the supplied matrix stack. */
    public Matrix4f createIdentityMatrix() { return new Matrix4f(); }

    /** Returns the GUI height using the original fixed factor of two and upward rounding. */
    public int getHeight() {
        return (int) Math.ceil(Minecraft.getInstance().getWindow().getHeight() / 2.0);
    }

    /** Returns the supplied matrix stack. */
    public PoseStack getMatrices() { return matrices; }

    /** Returns the supplied interpolation value. */
    public float getTickDelta() { return tickDelta; }

    /** Returns the draw-context adapter. */
    public DrawContextAdapter getDrawContext() { return drawContext; }

    /** Returns the UI renderer. */
    public UiRenderer getRenderer() { return renderer; }

    /** Returns the render texture view. */
    public TextureView getTexture() { return texture; }
}

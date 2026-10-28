package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import dev.echo.client.events.input.KeybindingEvent;
import dev.echo.client.render.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * World-render notification retaining all nine original payload references/values.
 * The original shared sentinel read and counter-controlled write are retained in
 * this functional analysis candidate; their production-removal proofs are pending.
 */
public class WorldRenderEvent extends Event {
    private final MatrixStack matrices;
    private final float tickProgress;
    private final float dynamicDeltaTicks;
    private final Camera camera;
    private final GameRenderer gameRenderer;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final WorldRenderer worldRenderer;
    private final WorldRenderContext renderContext;

    /**
     * Records the original shared render references without defensive copies.
     * @param matrices world matrix stack
     * @param tickProgress render interpolation progress
     * @param dynamicDeltaTicks render tick counter's dynamic delta ticks
     * @param camera render camera
     * @param gameRenderer active game renderer
     * @param projectionMatrix projection matrix captured by the render hook
     * @param viewMatrix view matrix captured by the render hook
     * @param worldRenderer active world renderer
     * @param renderContext client's world drawing context
     */
    public WorldRenderEvent(MatrixStack matrices, float tickProgress, float dynamicDeltaTicks,
                            Camera camera, GameRenderer gameRenderer, Matrix4f projectionMatrix,
                            Matrix4f viewMatrix, WorldRenderer worldRenderer,
                            WorldRenderContext renderContext) {
        this.matrices = matrices;
        this.tickProgress = tickProgress;
        KeybindingEvent.getInputSentinel();
        this.dynamicDeltaTicks = dynamicDeltaTicks;
        this.camera = camera;
        this.gameRenderer = gameRenderer;
        this.projectionMatrix = projectionMatrix;
        this.viewMatrix = viewMatrix;
        this.worldRenderer = worldRenderer;
        this.renderContext = renderContext;
        if (Event.getEventCounter() != 0) {
            KeybindingEvent.setInputSentinel(new String[1]);
        }
    }

    /** @return the shared matrix stack */
    public MatrixStack getMatrices() { return matrices; }
    /** @return render interpolation progress */
    public float getTickProgress() { return tickProgress; }
    /** @return the dynamic delta ticks */
    public float getDynamicDeltaTicks() { return dynamicDeltaTicks; }
    /** @return the camera */
    public Camera getCamera() { return camera; }
    /** @return the game renderer */
    public GameRenderer getGameRenderer() { return gameRenderer; }
    /** @return the shared projection matrix */
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    /** @return the shared view matrix */
    public Matrix4f getViewMatrix() { return viewMatrix; }
    /** @return the world renderer */
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    /** @return the world drawing context */
    public WorldRenderContext getRenderContext() { return renderContext; }
}

package dev.echo.client.render;

import dev.echo.render.device.RenderDevice;
import org.joml.Matrix4f;

/** Candidate explicit-device version of the original shared-device projection helper. */
public final class GuiProjection {
    private GuiProjection() {}

    public static Matrix4f create(RenderDevice device) {
        return new Matrix4f().ortho(0.0f, GuiDimensions.width(), GuiDimensions.height(), 0.0f,
                -1000.0f, 1000.0f, device.isVulkan());
    }
}

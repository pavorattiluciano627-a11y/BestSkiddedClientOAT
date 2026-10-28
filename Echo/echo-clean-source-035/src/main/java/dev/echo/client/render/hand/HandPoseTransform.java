package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

/** Shared body of the thirteen identical, directly encoded hand-position helpers. */
public final class HandPoseTransform {
    private HandPoseTransform() {
    }

    public static void translate(MatrixStack matrices, Arm arm, float equipProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}

package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Smooth's functional transform; original opaque counter writes remain an applicability gate. */
public class SmoothHandAnimation extends HandAnimation {
    public SmoothHandAnimation() {
        super("Smooth");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float offset = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, offset);
        int side = arm == Arm.RIGHT ? 1 : -1;
        float squaredWave = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * -20.0F));
        // Retain the original multiply and rotation call, including signed-zero behavior.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * 0.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }
}

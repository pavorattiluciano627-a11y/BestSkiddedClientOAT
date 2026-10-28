package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

/** Alternating spin direction retained on the strategy instance across render calls. */
public class Spin360HandAnimation extends HandAnimation {
    private boolean positiveRotation;

    public Spin360HandAnimation() {
        super("Spin360");
        positiveRotation = false;
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * (positiveRotation ? 360.0F : -360.0F)));
        // Toggle after drawing on every call in the interval, not just its rising edge.
        if (swingProgress < 0.01F && swingProgress > 0.0F) {
            positiveRotation = !positiveRotation;
        }
    }
}

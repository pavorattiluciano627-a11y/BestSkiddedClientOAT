package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public class SpinHandAnimation extends HandAnimation {
    public SpinHandAnimation() {
        super("Spin");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * -360.0F));
    }
}

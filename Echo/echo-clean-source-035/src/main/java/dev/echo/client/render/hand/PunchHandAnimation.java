package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class PunchHandAnimation extends HandAnimation {
    public PunchHandAnimation() {
        super("Punch");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        // FCMPL sends NaN through the sine branch, just as the original bytecode does.
        float angle = swingProgress <= 0.0F ? 0.0F : -MathHelper.sin(swingProgress * 13.0F) * 37.0F;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
    }
}

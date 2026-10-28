package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SigmaHandAnimation extends HandAnimation {
    public SigmaHandAnimation() {
        super("Sigma");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60.0F * wave - 50.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F));
    }
}

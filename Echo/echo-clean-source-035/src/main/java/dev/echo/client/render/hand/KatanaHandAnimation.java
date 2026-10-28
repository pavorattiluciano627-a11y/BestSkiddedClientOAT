package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class KatanaHandAnimation extends HandAnimation {
    public KatanaHandAnimation() {
        super("Katana");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30.0F * (1.0F - wave) - 30.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F));
    }
}

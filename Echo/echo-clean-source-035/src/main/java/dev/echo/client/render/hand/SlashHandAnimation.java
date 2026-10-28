package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SlashHandAnimation extends HandAnimation {
    public SlashHandAnimation() {
        super("Slash");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float offset = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, offset);
        int side = arm == Arm.RIGHT ? 1 : -1;
        float squaredWave = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * -70.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }
}

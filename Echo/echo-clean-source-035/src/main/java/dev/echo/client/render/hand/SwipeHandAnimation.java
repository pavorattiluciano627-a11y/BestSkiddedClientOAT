package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwipeHandAnimation extends HandAnimation {
    public SwipeHandAnimation() {
        super("Swipe");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        int side = arm == Arm.RIGHT ? 1 : -1;
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * -20.0F));
    }
}

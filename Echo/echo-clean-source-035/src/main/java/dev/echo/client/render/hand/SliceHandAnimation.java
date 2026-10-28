package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SliceHandAnimation extends HandAnimation {
    public SliceHandAnimation() {
        super("Slice");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-MathHelper.sin(swingProgress * 3.0F) * 60.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F * wave));
    }
}

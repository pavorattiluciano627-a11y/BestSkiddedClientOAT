package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class StabHandAnimation extends HandAnimation {
    public StabHandAnimation() {
        super("Stab");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.translate(0.0F, 0.0F, -wave / 4.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-120.0F));
    }
}

package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class DefaultHandAnimation extends HandAnimation {
    public DefaultHandAnimation() {
        super("Default");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        float x = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float y = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
        float z = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate(direction * x, y, z);
        rotate(matrices, arm, swingProgress);
    }

    private void rotate(MatrixStack matrices, Arm arm, float swingProgress) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        float squaredWave = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }
}

package dev.echo.client.render.hand;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class ExhibitionHandAnimation extends HandAnimation {
    public ExhibitionHandAnimation() {
        super("Exhibition");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        if (arm != MinecraftClient.getInstance().player.getMainArm()) {
            applyConfiguredSwing(matrices, arm, swingProgress);
            return;
        }
        int side = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(side * -0.1F, 0.2F, 0.0F);
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * -35.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
        matrices.multiply((arm == Arm.RIGHT ? RotationAxis.POSITIVE_Y : RotationAxis.NEGATIVE_Y).rotationDegrees(13.365F));
        matrices.multiply((arm == Arm.RIGHT ? RotationAxis.POSITIVE_Z : RotationAxis.NEGATIVE_Z).rotationDegrees(78.05F));
    }
}

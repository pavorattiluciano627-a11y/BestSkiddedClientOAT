package dev.echo.client.render.hand;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Configured base swing with the original main-hand pose and double offhand swing. */
public class CustomHandAnimation extends HandAnimation {
    public CustomHandAnimation() {
        super("Custom");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        applyConfiguredSwing(matrices, arm, swingProgress);
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        if (arm != MinecraftClient.getInstance().player.getMainArm()) {
            // Do not combine these two samples: settings are read again for the second call.
            applyConfiguredSwing(matrices, arm, swingProgress);
            return;
        }
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30.0F * (1.0F - wave) - 30.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0F));
    }
}

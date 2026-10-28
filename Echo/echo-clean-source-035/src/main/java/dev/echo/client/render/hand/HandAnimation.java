package dev.echo.client.render.hand;

import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.ViewModelFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Named first-person hand transform and the original live-strength swing operation. */
public abstract class HandAnimation {
    private final String displayName;

    public HandAnimation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public abstract void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm);

    /** Samples the live feature and selected hand strength on every invocation. */
    public static void applyConfiguredSwing(MatrixStack matrices, Arm arm, float swingProgress) {
        ViewModelFeature feature = (ViewModelFeature) Echo.featureManager.findFeature(ViewModelFeature.class);
        float amplitude = 1.0F;
        if (feature != null && feature.isEnabled()) {
            if (MinecraftClient.getInstance().player != null) {
                amplitude = arm == MinecraftClient.getInstance().player.getMainArm()
                        ? ViewModelFeature.strengthMain.getValue()
                        : ViewModelFeature.strengthOff.getValue();
            } else {
                amplitude = ViewModelFeature.strengthMain.getValue();
            }
        }
        int side = arm == Arm.RIGHT ? 1 : -1;
        float squaredWave = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * (-2.0F * amplitude)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wave * (-8.0F * amplitude)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
    }
}

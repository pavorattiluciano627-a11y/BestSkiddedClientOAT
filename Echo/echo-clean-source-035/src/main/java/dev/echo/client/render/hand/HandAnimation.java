package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.echo.client.Echo;
import dev.echo.client.features.impl.render.ViewModelFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Named first-person hand transform and the original live-strength swing operation. */
public abstract class HandAnimation {
    private final String displayName;

    public HandAnimation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public abstract void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm);

    /** Samples the live feature and selected hand strength on every invocation. */
    public static void applyConfiguredSwing(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        ViewModelFeature feature = (ViewModelFeature) Echo.featureManager.findFeature(ViewModelFeature.class);
        float amplitude = 1.0F;
        if (feature != null && feature.isEnabled()) {
            if (Minecraft.getInstance().player != null) {
                amplitude = arm == Minecraft.getInstance().player.getMainArm()
                        ? ViewModelFeature.strengthMain.getValue()
                        : ViewModelFeature.strengthOff.getValue();
            } else {
                amplitude = ViewModelFeature.strengthMain.getValue();
            }
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float squaredWave = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(side * wave * (-2.0F * amplitude)));
        matrices.mulPose(Axis.XP.rotationDegrees(wave * (-8.0F * amplitude)));
        matrices.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }
}

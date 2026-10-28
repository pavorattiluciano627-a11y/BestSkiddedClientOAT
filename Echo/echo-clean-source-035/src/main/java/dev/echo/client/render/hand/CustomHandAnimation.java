package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Configured base swing with the original main-hand pose and double offhand swing. */
public class CustomHandAnimation extends HandAnimation {
    public CustomHandAnimation() {
        super("Custom");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        applyConfiguredSwing(matrices, arm, swingProgress);
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        if (arm != Minecraft.getInstance().player.getMainArm()) {
            // Do not combine these two samples: settings are read again for the second call.
            applyConfiguredSwing(matrices, arm, swingProgress);
            return;
        }
        matrices.mulPose(Axis.XP.rotationDegrees(70.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(-30.0F * (1.0F - wave) - 30.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(110.0F));
    }
}

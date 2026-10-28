package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class SigmaHandAnimation extends HandAnimation {
    public SigmaHandAnimation() {
        super("Sigma");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(50.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(-60.0F * wave - 50.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(110.0F));
    }
}

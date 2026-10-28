package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class SlashHandAnimation extends HandAnimation {
    public SlashHandAnimation() {
        super("Slash");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        float offset = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, offset);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float squaredWave = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        matrices.mulPose(Axis.ZP.rotationDegrees(side * wave * -70.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(-70.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }
}

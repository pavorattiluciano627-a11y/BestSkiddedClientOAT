package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.entity.HumanoidArm;

public class SpinHandAnimation extends HandAnimation {
    public SpinHandAnimation() {
        super("Spin");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(swingProgress * -360.0F));
    }
}

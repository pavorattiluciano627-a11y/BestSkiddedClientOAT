package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class PunchHandAnimation extends HandAnimation {
    public PunchHandAnimation() {
        super("Punch");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        // FCMPL sends NaN through the sine branch, just as the original bytecode does.
        float angle = swingProgress <= 0.0F ? 0.0F : -Mth.sin(swingProgress * 13.0F) * 37.0F;
        matrices.mulPose(Axis.XP.rotationDegrees(angle));
    }
}

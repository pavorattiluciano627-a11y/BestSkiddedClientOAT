package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class SwipeHandAnimation extends HandAnimation {
    public SwipeHandAnimation() {
        super("Swipe");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(side * wave * -20.0F));
    }
}

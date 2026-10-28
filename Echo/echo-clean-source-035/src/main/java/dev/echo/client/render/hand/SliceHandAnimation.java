package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class SliceHandAnimation extends HandAnimation {
    public SliceHandAnimation() {
        super("Slice");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(-Mth.sin(swingProgress * 3.0F) * 60.0F));
        matrices.mulPose(Axis.ZP.rotationDegrees(-60.0F * wave));
    }
}

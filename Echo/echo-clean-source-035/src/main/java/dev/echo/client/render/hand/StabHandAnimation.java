package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class StabHandAnimation extends HandAnimation {
    public StabHandAnimation() {
        super("Stab");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        HandPoseTransform.translate(matrices, arm, 0.0F);
        matrices.translate(0.0F, 0.0F, -wave / 4.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(-120.0F));
    }
}

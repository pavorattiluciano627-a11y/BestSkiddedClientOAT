package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class DefaultHandAnimation extends HandAnimation {
    public DefaultHandAnimation() {
        super("Default");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        float x = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float y = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
        float z = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        matrices.translate(direction * x, y, z);
        rotate(matrices, arm, swingProgress);
    }

    private void rotate(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float squaredWave = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + squaredWave * -20.0F)));
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(side * wave * -20.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(wave * -80.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
    }
}

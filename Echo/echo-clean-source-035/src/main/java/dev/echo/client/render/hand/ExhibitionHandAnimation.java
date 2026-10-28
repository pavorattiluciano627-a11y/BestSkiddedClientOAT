package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class ExhibitionHandAnimation extends HandAnimation {
    public ExhibitionHandAnimation() {
        super("Exhibition");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        if (arm != Minecraft.getInstance().player.getMainArm()) {
            applyConfiguredSwing(matrices, arm, swingProgress);
            return;
        }
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(side * -0.1F, 0.2F, 0.0F);
        float wave = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        matrices.mulPose(Axis.ZP.rotationDegrees(side * wave * 10.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(wave * -35.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(-102.25F));
        matrices.mulPose((arm == HumanoidArm.RIGHT ? Axis.YP : Axis.YN).rotationDegrees(13.365F));
        matrices.mulPose((arm == HumanoidArm.RIGHT ? Axis.ZP : Axis.ZN).rotationDegrees(78.05F));
    }
}

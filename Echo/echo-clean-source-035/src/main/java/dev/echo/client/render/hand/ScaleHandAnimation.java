package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class ScaleHandAnimation extends HandAnimation {
    public ScaleHandAnimation() {
        super("Scale");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        float scale = -Mth.sin(swingProgress * 3.0F) / 2.0F + 1.0F;
        matrices.scale(scale, scale, scale);
    }
}

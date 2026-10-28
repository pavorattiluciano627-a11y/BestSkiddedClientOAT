package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class BounceHandAnimation extends HandAnimation {
    public BounceHandAnimation() {
        super("Bounce");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        HandPoseTransform.translate(matrices, arm,
                0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2)));
    }
}

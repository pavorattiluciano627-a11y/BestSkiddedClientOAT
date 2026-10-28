package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

public class BounceHandAnimation extends HandAnimation {
    public BounceHandAnimation() {
        super("Bounce");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm,
                0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2)));
    }
}

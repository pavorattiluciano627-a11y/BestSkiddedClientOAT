package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

public class ScaleHandAnimation extends HandAnimation {
    public ScaleHandAnimation() {
        super("Scale");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        HandPoseTransform.translate(matrices, arm, 0.0F);
        float scale = -MathHelper.sin(swingProgress * 3.0F) / 2.0F + 1.0F;
        matrices.scale(scale, scale, scale);
    }
}

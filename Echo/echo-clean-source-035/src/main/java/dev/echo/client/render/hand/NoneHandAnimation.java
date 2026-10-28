package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

/** Selects the original explicit no-animation strategy. */
public class NoneHandAnimation extends HandAnimation {
    public NoneHandAnimation() {
        super("None");
    }

    @Override
    public void apply(float swingProgress, PoseStack matrices, int direction, HumanoidArm arm) {
        // The original method is exactly RETURN: no base translation is applied.
        return;
    }
}

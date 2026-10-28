package dev.echo.client.render.hand;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

/** Selects the original explicit no-animation strategy. */
public class NoneHandAnimation extends HandAnimation {
    public NoneHandAnimation() {
        super("None");
    }

    @Override
    public void apply(float swingProgress, MatrixStack matrices, int direction, Arm arm) {
        // The original method is exactly RETURN: no base translation is applied.
        return;
    }
}

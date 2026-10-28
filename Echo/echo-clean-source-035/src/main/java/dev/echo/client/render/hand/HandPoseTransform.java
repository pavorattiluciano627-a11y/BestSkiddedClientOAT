package dev.echo.client.render.hand;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;

/** Shared body of the thirteen identical, directly encoded hand-position helpers. */
public final class HandPoseTransform {
    private HandPoseTransform() {
    }

    public static void translate(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}

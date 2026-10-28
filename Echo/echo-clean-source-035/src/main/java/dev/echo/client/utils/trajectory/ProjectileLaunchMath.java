package dev.echo.client.utils.trajectory;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Exact float-angle construction used by the bow simulation. */
public final class ProjectileLaunchMath {
    private ProjectileLaunchMath() {
    }

    public static Vec3d bowVelocity(float pitch, float yaw, float speed) {
        float x = -MathHelper.sin((double) (yaw * ((float) Math.PI / 180)))
                * MathHelper.cos((double) (pitch * ((float) Math.PI / 180)));
        float y = -MathHelper.sin((double) (pitch * ((float) Math.PI / 180)));
        float z = MathHelper.cos((double) (yaw * ((float) Math.PI / 180)))
                * MathHelper.cos((double) (pitch * ((float) Math.PI / 180)));
        return new Vec3d((double) x, (double) y, (double) z).normalize().multiply((double) speed);
    }
}

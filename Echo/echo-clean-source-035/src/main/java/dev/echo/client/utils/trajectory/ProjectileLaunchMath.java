package dev.echo.client.utils.trajectory;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Exact float-angle construction used by the bow simulation. */
public final class ProjectileLaunchMath {
    private ProjectileLaunchMath() {
    }

    public static Vec3 bowVelocity(float pitch, float yaw, float speed) {
        float x = -Mth.sin((double) (yaw * ((float) Math.PI / 180)))
                * Mth.cos((double) (pitch * ((float) Math.PI / 180)));
        float y = -Mth.sin((double) (pitch * ((float) Math.PI / 180)));
        float z = Mth.cos((double) (yaw * ((float) Math.PI / 180)))
                * Mth.cos((double) (pitch * ((float) Math.PI / 180)));
        return new Vec3((double) x, (double) y, (double) z).normalize().scale((double) speed);
    }
}

package dev.echo.client.scripting.api;

import net.minecraft.util.math.Vec3i;

/** Integer vector; Java overflow and division-by-zero behavior are intentional. */
public class Vector3iView {
    public final int x;
    public final int y;
    public final int z;

    public Vector3iView(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3iView(Vec3i vector) {
        this(vector.getX(), vector.getY(), vector.getZ());
    }

    public Vector3iView add(Vector3iView other) {
        return new Vector3iView(x + other.x, y + other.y, z + other.z);
    }

    public Vector3iView subtract(Vector3iView other) {
        return new Vector3iView(x - other.x, y - other.y, z - other.z);
    }

    public Vector3iView multiply(Vector3iView other) {
        return new Vector3iView(x * other.x, y * other.y, z * other.z);
    }

    public Vector3iView divide(Vector3iView other) {
        return new Vector3iView(x / other.x, y / other.y, z / other.z);
    }

    @Override
    public String toString() {
        return "vec3i(" + x + ", " + y + ", " + z + ")";
    }
}

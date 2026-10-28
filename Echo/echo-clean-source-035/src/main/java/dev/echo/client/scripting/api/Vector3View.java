package dev.echo.client.scripting.api;

import net.minecraft.util.math.Vec3d;

/** Immutable script vector with componentwise arithmetic and identity equality. */
public class Vector3View {
    public final double x;
    public final double y;
    public final double z;

    public Vector3View(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3View(Vec3d vector) {
        this(vector.getX(), vector.getY(), vector.getZ());
    }

    public Vector3View add(Vector3View other) {
        return new Vector3View(x + other.x, y + other.y, z + other.z);
    }

    public Vector3View subtract(Vector3View other) {
        return new Vector3View(x - other.x, y - other.y, z - other.z);
    }

    public Vector3View multiply(Vector3View other) {
        return new Vector3View(x * other.x, y * other.y, z * other.z);
    }

    public Vector3View divide(Vector3View other) {
        return new Vector3View(x / other.x, y / other.y, z / other.z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3View normalize() {
        double length = length();
        return new Vector3View(x / length, y / length, z / length);
    }

    @Override
    public String toString() {
        return "vec3(" + x + ", " + y + ", " + z + ")";
    }
}

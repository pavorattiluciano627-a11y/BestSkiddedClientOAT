package dev.echo.client.particle;

/** Shared particle state; the manager owns age advancement and update scheduling. */
public abstract class Particle {
    public int age;
    public final int maxAge;
    public double x;
    public double y;
    public double z;
    public int color;
    public float rotation;

    public Particle(int maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isDead() {
        String marker = ParticleManager.getUpdateMarker();
        int currentAge = age;
        if (marker == null) {
            // The original IRETURN of an int from a boolean method narrows to bit zero.
            return (currentAge & 1) != 0;
        }
        return currentAge >= maxAge;
    }

    public boolean isThroughWalls() {
        return false;
    }

    public abstract float getSize();

    public abstract void tick(float tickProgress);
}

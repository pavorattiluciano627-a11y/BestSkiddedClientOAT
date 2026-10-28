package dev.echo.client.render;

/** Mutable screen-space bounds returned by the original box projection. */
public final class ProjectedBounds {
    public double minX;
    public double minY;
    public double maxX;
    public double maxY;
    public boolean visible;

    public ProjectedBounds(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
}

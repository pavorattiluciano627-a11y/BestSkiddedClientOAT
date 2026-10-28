package dev.echo.client.render;

import dev.echo.client.particle.ParticleUv;

/**
 * Original world-geometry virtual slots with their functional argument order.
 * Opaque static state and key-fragment removal remain in the analysis ledger.
 */
public abstract class WorldGeometry {
    public abstract void beginFrame();

    public abstract void destroy();

    public abstract void drawBox(double x, double y, double z,
                                 double width, double height, double depth, int color);

    public abstract void drawTracer(double x, double y, double z, int color);

    public abstract void drawLine(double x1, double y1, double z1,
                                  double x2, double y2, double z2, int color);

    public abstract void drawTriangle(double x1, double y1, double z1, int color1,
                                      double x2, double y2, double z2, int color2,
                                      double x3, double y3, double z3, int color3);

    public abstract void drawParticle(double x, double y, double z, float size,
                                      int color, ParticleUv uv, float rotation);
}

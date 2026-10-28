package dev.echo.client.render;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import dev.echo.client.events.render.WorldRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** World projection and ordered outline primitives, retaining the original shared-state operations. */
public class WorldProjection implements MinecraftAccess {
    private static final double FULL_CIRCLE = Math.PI * 2;

    public static ProjectedBounds projectBox(WorldRenderEvent event, Box bounds) {
        MinecraftClient client = MinecraftClient.getInstance();
        String[] sentinel = MatrixBufferWriter.getMemorySentinel();
        Vec3d camera = MinecraftRenderState.getCamera(client).getCameraPos();
        Matrix4f view = event.getViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        int width = GuiDimensions.width();
        int height = GuiDimensions.height();
        Vec3d[] corners = {
                new Vec3d(bounds.minX, bounds.minY, bounds.minZ),
                new Vec3d(bounds.maxX, bounds.minY, bounds.minZ),
                new Vec3d(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vec3d(bounds.minX, bounds.minY, bounds.maxZ),
                new Vec3d(bounds.minX, bounds.maxY, bounds.minZ),
                new Vec3d(bounds.maxX, bounds.maxY, bounds.minZ),
                new Vec3d(bounds.maxX, bounds.maxY, bounds.maxZ),
                new Vec3d(bounds.minX, bounds.maxY, bounds.maxZ)
        };
        Vector4f[] clipCorners = new Vector4f[8];
        for (int index = 0; index < 8; index++) {
            Vec3d corner = corners[index];
            Vector4f clip = new Vector4f((float) (corner.x - camera.x),
                    (float) (corner.y - camera.y), (float) (corner.z - camera.z), 1.0F);
            clip.mul(view);
            clip.mul(projection);
            clipCorners[index] = clip;
            if (sentinel == null) {
                int counter = Event.getEventCounterFallbackValue();
                Event.setEventCounter(counter + 1);
                // The original builds the edge array before its saved-null-sentinel
                // branch returns the still-null bounds, after transforming one corner.
                BoxProjection.orderedEdges();
                return null;
            }
        }
        return BoxProjection.projectClipCorners(clipCorners, width, height);
    }

    public static void drawBoxOutline(WorldGeometry geometry, Box bounds, int color) {
        double minX = bounds.minX;
        double minY = bounds.minY;
        double minZ = bounds.minZ;
        MatrixBufferWriter.getMemorySentinel();
        double maxX = bounds.maxX;
        double maxY = bounds.maxY;
        double maxZ = bounds.maxZ;
        geometry.drawLine(minX, minY, minZ, maxX, minY, minZ, color);
        geometry.drawLine(maxX, minY, minZ, maxX, minY, maxZ, color);
        geometry.drawLine(maxX, minY, maxZ, minX, minY, maxZ, color);
        geometry.drawLine(minX, minY, maxZ, minX, minY, minZ, color);
        geometry.drawLine(minX, maxY, minZ, maxX, maxY, minZ, color);
        geometry.drawLine(maxX, maxY, minZ, maxX, maxY, maxZ, color);
        geometry.drawLine(maxX, maxY, maxZ, minX, maxY, maxZ, color);
        geometry.drawLine(minX, maxY, maxZ, minX, maxY, minZ, color);
        geometry.drawLine(minX, minY, minZ, minX, maxY, minZ, color);
        geometry.drawLine(maxX, minY, minZ, maxX, maxY, minZ, color);
        geometry.drawLine(maxX, minY, maxZ, maxX, maxY, maxZ, color);
        geometry.drawLine(minX, minY, maxZ, minX, maxY, maxZ, color);
        if (Event.getEventCounterFallbackValue() == 0) {
            MatrixBufferWriter.setMemorySentinel(new String[1]);
        }
    }

    public static void drawRectangleBorder(UiRenderer renderer, float x, float y,
                                            float width, float height, float thickness, int color) {
        renderer.drawRoundedRect(x, y, width, thickness, 0.0F, color);
        renderer.drawRoundedRect(x, y + height - thickness, width, thickness, 0.0F, color);
        renderer.drawRoundedRect(x, y + thickness, thickness, height - thickness * 2.0F, 0.0F, color);
        renderer.drawRoundedRect(x + width - thickness, y + thickness, thickness,
                height - thickness * 2.0F, 0.0F, color);
    }

    public static void drawCircle(WorldGeometry geometry, Vec3d center, Vec3d normal,
                                   double radius, int segments, int color) {
        String[] sentinel = MatrixBufferWriter.getMemorySentinel();
        if (geometry == null || center == null || normal == null || segments < 3 || radius <= 0.0) {
            return;
        }
        Vec3d axis = normal.normalize();
        if (axis.lengthSquared() < 1.0E-6) {
            axis = new Vec3d(0.0, 1.0, 0.0);
        }
        Vec3d reference = Math.abs(axis.y) < 0.99 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
        Vec3d tangent = axis.crossProduct(reference).normalize();
        if (tangent.lengthSquared() < 1.0E-6) {
            tangent = new Vec3d(1.0, 0.0, 0.0);
        }
        Vec3d bitangent = axis.crossProduct(tangent).normalize();
        for (int segment = 0; segment < segments; segment++) {
            double angle = FULL_CIRCLE * segment / segments;
            double nextAngle = FULL_CIRCLE * (segment + 1) / segments;
            Vec3d from = center.add(tangent.multiply(Math.cos(angle) * radius))
                    .add(bitangent.multiply(Math.sin(angle) * radius));
            Vec3d to = center.add(tangent.multiply(Math.cos(nextAngle) * radius))
                    .add(bitangent.multiply(Math.sin(nextAngle) * radius));
            geometry.drawLine(from.x, from.y, from.z, to.x, to.y, to.z, color);
            if (sentinel == null) {
                break;
            }
        }
    }
}

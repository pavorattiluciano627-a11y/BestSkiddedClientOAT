package dev.echo.client.render;

import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.Event;
import dev.echo.client.events.render.WorldRenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** World projection and ordered outline primitives, retaining the original shared-state operations. */
public class WorldProjection implements MinecraftAccess {
    private static final double FULL_CIRCLE = Math.PI * 2;

    public static ProjectedBounds projectBox(WorldRenderEvent event, AABB bounds) {
        Minecraft client = Minecraft.getInstance();
        String[] sentinel = MatrixBufferWriter.getMemorySentinel();
        Vec3 camera = MinecraftRenderState.getCamera(client).position();
        Matrix4f view = event.getViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        int width = GuiDimensions.width();
        int height = GuiDimensions.height();
        Vec3[] corners = {
                new Vec3(bounds.minX, bounds.minY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.minY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vec3(bounds.minX, bounds.minY, bounds.maxZ),
                new Vec3(bounds.minX, bounds.maxY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.maxY, bounds.minZ),
                new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ),
                new Vec3(bounds.minX, bounds.maxY, bounds.maxZ)
        };
        Vector4f[] clipCorners = new Vector4f[8];
        for (int index = 0; index < 8; index++) {
            Vec3 corner = corners[index];
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

    public static void drawBoxOutline(WorldGeometry geometry, AABB bounds, int color) {
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

    public static void drawCircle(WorldGeometry geometry, Vec3 center, Vec3 normal,
                                   double radius, int segments, int color) {
        String[] sentinel = MatrixBufferWriter.getMemorySentinel();
        if (geometry == null || center == null || normal == null || segments < 3 || radius <= 0.0) {
            return;
        }
        Vec3 axis = normal.normalize();
        if (axis.lengthSqr() < 1.0E-6) {
            axis = new Vec3(0.0, 1.0, 0.0);
        }
        Vec3 reference = Math.abs(axis.y) < 0.99 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = axis.cross(reference).normalize();
        if (tangent.lengthSqr() < 1.0E-6) {
            tangent = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 bitangent = axis.cross(tangent).normalize();
        for (int segment = 0; segment < segments; segment++) {
            double angle = FULL_CIRCLE * segment / segments;
            double nextAngle = FULL_CIRCLE * (segment + 1) / segments;
            Vec3 from = center.add(tangent.scale(Math.cos(angle) * radius))
                    .add(bitangent.scale(Math.sin(angle) * radius));
            Vec3 to = center.add(tangent.scale(Math.cos(nextAngle) * radius))
                    .add(bitangent.scale(Math.sin(nextAngle) * radius));
            geometry.drawLine(from.x, from.y, from.z, to.x, to.y, to.z, color);
            if (sentinel == null) {
                break;
            }
        }
    }
}

package dev.echo.client.render;

import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferAccess;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.CommandEncoder;
import dev.echo.render.command.RenderPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.pipeline.GraphicsPipeline;
import dev.echo.render.pipeline.GraphicsPipelineInfo;
import dev.echo.render.pipeline.PrimitiveTopology;
import dev.echo.render.vertex.VertexFormat;
import dev.echo.render.vertex.VertexStepMode;
import java.util.ArrayList;
import java.util.List;

/** Adaptive quadratic strokes with solid strips, round joins and alpha-feathered edges. */
public final class ImmediateColorBatch implements RenderBatch {
    private static final int VERTEX_BYTES = 20;
    private final RenderDevice device;
    private final Buffer cameraUniforms;
    private final GraphicsPipeline pipeline;
    private Buffer vertices;
    private int capacity = 4096;
    private int submittedVertices;
    private int pendingVertices;

    public ImmediateColorBatch(RenderDevice device, Buffer cameraUniforms) {
        this.device = device;
        this.cameraUniforms = cameraUniforms;
        GraphicsPipelineInfo info = new GraphicsPipelineInfo(device, "shaders/immediate/position_color.wgsl")
                .vertexAttribute(0, VertexFormat.Float4).vertexAttribute(1, VertexFormat.Int)
                .vertexStepMode(VertexStepMode.VERTEX);
        info.topology = PrimitiveTopology.TRIANGLE_LIST;
        info.cullEnabled = false;
        info.blendEnabled = true;
        pipeline = device.createGraphicsPipeline(info);
        vertices = device.createBuffer(BufferInfo.builder().usage(1).size((long) capacity * VERTEX_BYTES)
                .dynamic(true).mapped(true).build());
    }

    public void quadraticStroke(float startX, float startY, float controlX, float controlY,
                                 float endX, float endY, float width, float feather, float tolerance,
                                 int color, float depth) {
        List<ImmediatePoint> points = new ArrayList<>(32);
        ImmediatePoint start = new ImmediatePoint(startX, startY);
        ImmediatePoint control = new ImmediatePoint(controlX, controlY);
        ImmediatePoint end = new ImmediatePoint(endX, endY);
        points.add(start);
        flattenQuadratic(start, control, end, Math.max(0.001F, tolerance), 0, points);
        float radius = width * 0.5F;
        float fringe = Math.max(0.35F, feather);
        int transparentColor = color & 0x00FFFFFF;
        for (int index = 1; index < points.size(); index++) {
            segment(points.get(index - 1), points.get(index), radius, fringe, color, transparentColor, depth);
        }
        int segments = Math.max(12, Math.min(32,
                (int) Math.ceil(Math.PI * Math.sqrt(Math.max(1.0F, radius / Math.max(tolerance, 0.01F))))));
        for (ImmediatePoint point : points) {
            roundJoin(point, radius, fringe, segments, color, transparentColor, depth);
        }
    }

    private static void flattenQuadratic(ImmediatePoint start, ImmediatePoint control, ImmediatePoint end,
                                          float tolerance, int recursionDepth, List<ImmediatePoint> points) {
        float dx = end.x() - start.x();
        float dy = end.y() - start.y();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float deviation = length < 1.0E-5F
                ? distance(control, start)
                : Math.abs(dy * control.x() - dx * control.y()
                        + end.x() * start.y() - end.y() * start.x()) / length;
        if (deviation <= tolerance || recursionDepth >= 12) {
            points.add(end);
            return;
        }
        ImmediatePoint leftControl = midpoint(start, control);
        ImmediatePoint rightControl = midpoint(control, end);
        ImmediatePoint split = midpoint(leftControl, rightControl);
        flattenQuadratic(start, leftControl, split, tolerance, recursionDepth + 1, points);
        flattenQuadratic(split, rightControl, end, tolerance, recursionDepth + 1, points);
    }

    private void segment(ImmediatePoint start, ImmediatePoint end, float radius, float fringe,
                         int color, int transparentColor, float depth) {
        float dx = end.x() - start.x();
        float dy = end.y() - start.y();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5F) {
            return;
        }
        float normalX = -dy / length;
        float normalY = dx / length;
        ImmediatePoint startLeft = new ImmediatePoint(start.x() + normalX * radius, start.y() + normalY * radius);
        ImmediatePoint startRight = new ImmediatePoint(start.x() - normalX * radius, start.y() - normalY * radius);
        ImmediatePoint endLeft = new ImmediatePoint(end.x() + normalX * radius, end.y() + normalY * radius);
        ImmediatePoint endRight = new ImmediatePoint(end.x() - normalX * radius, end.y() - normalY * radius);
        quad(startLeft, startRight, endLeft, endRight, color, color, depth);

        ImmediatePoint outerStartLeft = new ImmediatePoint(start.x() + normalX * (radius + fringe),
                start.y() + normalY * (radius + fringe));
        ImmediatePoint outerEndLeft = new ImmediatePoint(end.x() + normalX * (radius + fringe),
                end.y() + normalY * (radius + fringe));
        quad(outerStartLeft, startLeft, outerEndLeft, endLeft, transparentColor, color, depth);

        ImmediatePoint outerStartRight = new ImmediatePoint(start.x() - normalX * (radius + fringe),
                start.y() - normalY * (radius + fringe));
        ImmediatePoint outerEndRight = new ImmediatePoint(end.x() - normalX * (radius + fringe),
                end.y() - normalY * (radius + fringe));
        quad(startRight, outerStartRight, endRight, outerEndRight, color, transparentColor, depth);
    }

    private void roundJoin(ImmediatePoint center, float radius, float fringe, int segments,
                           int color, int transparentColor, float depth) {
        for (int index = 0; index < segments; index++) {
            double angle = (Math.PI * 2) * index / segments;
            double nextAngle = (Math.PI * 2) * (index + 1) / segments;
            ImmediatePoint inner = pointOnCircle(center, radius, angle);
            ImmediatePoint nextInner = pointOnCircle(center, radius, nextAngle);
            triangle(center, inner, nextInner, color, color, color, depth);
            ImmediatePoint outer = pointOnCircle(center, radius + fringe, angle);
            ImmediatePoint nextOuter = pointOnCircle(center, radius + fringe, nextAngle);
            quad(inner, outer, nextInner, nextOuter, color, transparentColor, depth);
        }
    }

    private void quad(ImmediatePoint a, ImmediatePoint b, ImmediatePoint c, ImmediatePoint d,
                      int firstColor, int secondColor, float depth) {
        triangle(a, b, c, firstColor, secondColor, firstColor, depth);
        triangle(c, b, d, firstColor, secondColor, secondColor, depth);
    }

    private void triangle(ImmediatePoint a, ImmediatePoint b, ImmediatePoint c,
                          int colorA, int colorB, int colorC, float depth) {
        vertex(a, colorA, depth);
        vertex(b, colorB, depth);
        vertex(c, colorC, depth);
    }

    private void vertex(ImmediatePoint point, int color, float depth) {
        ensureCapacity(1);
        long address = vertices.mappedAddress() + (submittedVertices + pendingVertices) * (long) VERTEX_BYTES;
        NativeMemoryWrites.putFloat4(address, point.x(), point.y(), depth, 1.0F);
        NativeMemoryWrites.putInt(address + 16L, color);
        pendingVertices++;
    }

    private void ensureCapacity(int additionalVertices) {
        if (submittedVertices + pendingVertices + additionalVertices <= capacity) {
            return;
        }
        while (submittedVertices + pendingVertices + additionalVertices > capacity) {
            capacity *= 2;
        }
        vertices = device.reallocateMappedBuffer(vertices, (long) capacity * VERTEX_BYTES,
                (long) (submittedVertices + pendingVertices) * VERTEX_BYTES);
    }

    @Override
    public void prepare(CommandEncoder commands) {
        if (pendingVertices > 0) {
            commands.transitionBuffer(vertices.wholeSlice(), 1, BufferAccess.VERTEX_READ);
        }
    }

    @Override
    public void reset() {
        submittedVertices = 0;
        pendingVertices = 0;
    }

    @Override
    public void draw(CommandEncoder commands, RenderPass pass) {
        if (pendingVertices == 0) {
            return;
        }
        pass.setPipeline(pipeline);
        pass.setVertexBuffer(vertices.slice((long) submittedVertices * VERTEX_BYTES,
                (long) pendingVertices * VERTEX_BYTES));
        pass.setBuffer(0, cameraUniforms.wholeSlice());
        pass.draw(pendingVertices, 1, 0);
        submittedVertices += pendingVertices;
        pendingVertices = 0;
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        device.release(vertices);
    }

    private static ImmediatePoint midpoint(ImmediatePoint first, ImmediatePoint second) {
        return new ImmediatePoint((first.x() + second.x()) * 0.5F, (first.y() + second.y()) * 0.5F);
    }

    private static float distance(ImmediatePoint first, ImmediatePoint second) {
        float dx = first.x() - second.x();
        float dy = first.y() - second.y();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static ImmediatePoint pointOnCircle(ImmediatePoint center, float radius, double angle) {
        return new ImmediatePoint(center.x() + (float) Math.cos(angle) * radius,
                center.y() + (float) Math.sin(angle) * radius);
    }
}

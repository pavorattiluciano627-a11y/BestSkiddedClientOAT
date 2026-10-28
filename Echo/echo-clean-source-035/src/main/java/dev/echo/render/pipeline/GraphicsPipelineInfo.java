package dev.echo.render.pipeline;

import dev.echo.render.device.RenderDevice;
import dev.echo.render.shader.naga.Module;
import dev.echo.render.shader.wgsl.WgslParser;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.vertex.VertexAttribute;
import dev.echo.render.vertex.VertexFormat;
import dev.echo.render.vertex.VertexStepMode;
import java.util.ArrayList;
import java.util.List;

/** Mutable pipeline descriptor, including defaults used by both native backends. */
public final class GraphicsPipelineInfo {
    public final Module module;
    public final List<VertexAttribute> vertexAttributes = new ArrayList<>();
    public int vertexStride;
    public int pushConstantBytes;
    public boolean cullEnabled;
    public boolean depthTestEnabled = true;
    public boolean depthWriteEnabled = true;
    public boolean blendEnabled = true;
    public float lineWidth = 1.0f;
    public TextureFormat colorFormat = TextureFormat.BGRA8Unorm;
    public TextureFormat depthFormat;
    public PrimitiveTopology topology = PrimitiveTopology.TRIANGLE_STRIP;
    public DepthCompare depthCompare;
    public BlendMode blendMode = BlendMode.SRC_ALPHA;
    public CullFace cullFace = CullFace.BACK;
    public FrontFace frontFace;
    public VertexStepMode vertexStepMode = VertexStepMode.INSTANCE;

    public GraphicsPipelineInfo(RenderDevice device, String shaderResource) {
        module = WgslParser.parseSource(device.resourceLoader.loadText(shaderResource));
        depthFormat = device.depthFormat();
        depthCompare = device.capabilities().reversedDepth()
                ? DepthCompare.GREATER_OR_EQUAL : DepthCompare.LESS_OR_EQUAL;
        frontFace = FrontFace.COUNTER_CLOCKWISE;
    }

    public GraphicsPipelineInfo vertexAttribute(int location, VertexFormat format) {
        switch (format) {
            case Mat2 -> appendVertexAttributes(location, VertexFormat.Float2, 2);
            case Mat3 -> appendVertexAttributes(location, VertexFormat.Float3, 3);
            case Mat4 -> appendVertexAttributes(location, VertexFormat.Float4, 4);
            default -> {
                vertexAttributes.add(new VertexAttribute(location, format, vertexStride));
                vertexStride += format.bytes;
            }
        }
        return this;
    }

    private void appendVertexAttributes(int location, VertexFormat format, int columns) {
        for (int column = 0; column < columns; column++) {
            vertexAttributes.add(new VertexAttribute(location + column, format, vertexStride));
            vertexStride += format.bytes;
        }
    }

    public GraphicsPipelineInfo vertexStepMode(VertexStepMode mode) {
        vertexStepMode = mode;
        return this;
    }
}

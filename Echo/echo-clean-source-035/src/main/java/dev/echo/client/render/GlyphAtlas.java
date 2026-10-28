package dev.echo.client.render;

import dev.echo.client.resources.ClientResources;
import dev.echo.client.utils.IntLists;
import dev.echo.render.FontAsset;
import dev.echo.render.GlyphCoverageGrid;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Font metrics, quadratic contours and the three shader-readable curve/band textures. */
public class GlyphAtlas {
    private static final int WIDTH = 2048;
    private static final float PADDING = 0.05f;
    private static final float DOUBLE_PADDING = 0.1f;
    private static final int TEXEL_BYTES = 16;
    private static final int CURVE_BYTES = 32;
    private static final int TRANSFER_SOURCE_USAGE = 4;
    private static final int SAMPLED_TRANSFER_DESTINATION_USAGE = 5;

    private final List<float[]> curves = new ArrayList<>();
    private final List<int[]> bands = new ArrayList<>();
    private final IntArrayList bandIndices = new IntArrayList();
    private final GlyphCoverageGrid[] fonts;
    public Texture curvesTexture;
    public Texture bandTableTexture;
    public Texture bandIndicesTexture;

    public GlyphAtlas(RenderDevice device) {
        FontAsset[] assets = FontAsset.values();
        fonts = new GlyphCoverageGrid[assets.length];
        for (int index = 0; index < assets.length; index++) {
            fonts[index] = loadFont(assets[index]);
        }
        upload(device);
    }

    public GlyphCoverageGrid font(int ordinal) {
        return fonts[ordinal];
    }

    private GlyphCoverageGrid loadFont(FontAsset asset) {
        GlyphCoverageGrid grid = new GlyphCoverageGrid(asset.firstCodePoint, asset.glyphCount, asset.kerning);
        byte[] bytes = ClientResources.readBytes(asset.path);
        ByteBuffer fontBytes = MemoryUtil.memAlloc(bytes.length);
        fontBytes.put(bytes).flip();
        STBTTFontinfo font = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(font, fontBytes);
        float scale = STBTruetype.stbtt_ScaleForMappingEmToPixels(font, 1.0f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(font, ascent, descent, lineGap);
            grid.ascent = ascent.get(0) * scale;
            grid.lineHeight = (ascent.get(0) - descent.get(0) + lineGap.get(0)) * scale;
        }
        for (int codePoint = grid.firstCodePoint; codePoint < grid.firstCodePoint + grid.glyphCount; codePoint++) {
            loadGlyph(font, codePoint, scale, grid);
        }
        if (grid.kerningAdvances != null) {
            for (int left = 0; left < grid.glyphCount; left++) {
                for (int right = 0; right < grid.glyphCount; right++) {
                    grid.kerningAdvances[left][right] = STBTruetype.stbtt_GetCodepointKernAdvance(font,
                            grid.firstCodePoint + left, grid.firstCodePoint + right) * scale;
                }
            }
        }
        // Original font-data release occurs after successful metric/outline/kerning construction.
        MemoryUtil.memFree(fontBytes);
        return grid;
    }

    private void loadGlyph(STBTTFontinfo font, int codePoint, float scale, GlyphCoverageGrid grid) {
        int glyph = grid.indexOf(codePoint);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer leftBearing = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(font, codePoint, advance, leftBearing);
            grid.advanceWidths[glyph] = advance.get(0) * scale;
            IntBuffer minX = stack.mallocInt(1);
            IntBuffer minY = stack.mallocInt(1);
            IntBuffer maxX = stack.mallocInt(1);
            IntBuffer maxY = stack.mallocInt(1);
            if (!STBTruetype.stbtt_GetCodepointBox(font, codePoint, minX, minY, maxX, maxY)) {
                return;
            }
            float left = minX.get(0);
            float top = maxY.get(0);
            float width = (maxX.get(0) - minX.get(0)) * scale + DOUBLE_PADDING;
            float height = (maxY.get(0) - minY.get(0)) * scale + DOUBLE_PADDING;
            GlyphOutline outline = new GlyphOutline(scale, left, top, width, height, PADDING);
            if (!outline.readCodepoint(font, codePoint)) {
                return;
            }
            int firstCurve = curves.size();
            curves.addAll(outline.curves);
            grid.hasOutline[glyph] = true;
            grid.coverageBandReferences[0][glyph] = IntLists.appendBins(outline.curves, firstCurve, 1, bands, bandIndices);
            grid.coverageBandReferences[1][glyph] = IntLists.appendBins(outline.curves, firstCurve, 0, bands, bandIndices);
            grid.glyphOffsets[0][glyph] = left * scale - PADDING;
            grid.glyphOffsets[1][glyph] = top * scale + PADDING;
            grid.glyphSizes[0][glyph] = width;
            grid.glyphSizes[1][glyph] = height;
        }
    }

    private void upload(RenderDevice device) {
        if (curves.isEmpty()) curves.add(new float[6]);
        if (bands.isEmpty()) bands.add(new int[]{0, 0});
        if (bandIndices.isEmpty()) bandIndices.add(0);
        int curveRows = GlyphAtlasRows.rowsFor(curves.size() * 2);
        int bandRows = GlyphAtlasRows.rowsFor(bands.size());
        int indexRows = GlyphAtlasRows.rowsFor(bandIndices.size());
        curvesTexture = device.createTexture(TextureInfo.builder().width(WIDTH).height(curveRows)
                .format(TextureFormat.RGBA32Float).usage(SAMPLED_TRANSFER_DESTINATION_USAGE).build());
        bandTableTexture = device.createTexture(TextureInfo.builder().width(WIDTH).height(bandRows)
                .format(TextureFormat.RGBA32Float).usage(SAMPLED_TRANSFER_DESTINATION_USAGE).build());
        bandIndicesTexture = device.createTexture(TextureInfo.builder().width(WIDTH).height(indexRows)
                .format(TextureFormat.RGBA32Float).usage(SAMPLED_TRANSFER_DESTINATION_USAGE).build());
        Buffer curveUpload = curveUpload(device, curveRows);
        Buffer bandUpload = bandUpload(device, bandRows);
        Buffer indexUpload = indexUpload(device, indexRows);
        TransferPass transfer = device.beginTransfer();
        transfer.uploadTexture(curvesTexture.views[0], curveUpload.wholeSlice());
        transfer.uploadTexture(bandTableTexture.views[0], bandUpload.wholeSlice());
        transfer.uploadTexture(bandIndicesTexture.views[0], indexUpload.wholeSlice());
        transfer.end();
        device.release(curveUpload);
        device.release(bandUpload);
        device.release(indexUpload);
    }

    private Buffer curveUpload(RenderDevice device, int rows) {
        long bytes = (long) WIDTH * rows * TEXEL_BYTES;
        Buffer upload = device.createBuffer(BufferInfo.builder().usage(TRANSFER_SOURCE_USAGE).size(bytes).mapped(true).build());
        long address = upload.mappedAddress();
        NativeMemoryWrites.fill(address, (byte) 0, bytes);
        for (int index = 0; index < curves.size(); index++) {
            float[] curve = curves.get(index);
            long target = address + (long) index * CURVE_BYTES;
            NativeMemoryWrites.putFloat4(target, curve[0], curve[1], curve[2], curve[3]);
            NativeMemoryWrites.putFloat4(target + TEXEL_BYTES, curve[4], curve[5], 0.0f, 0.0f);
        }
        return upload;
    }

    private Buffer bandUpload(RenderDevice device, int rows) {
        long bytes = (long) WIDTH * rows * TEXEL_BYTES;
        Buffer upload = device.createBuffer(BufferInfo.builder().usage(TRANSFER_SOURCE_USAGE).size(bytes).mapped(true).build());
        long address = upload.mappedAddress();
        NativeMemoryWrites.fill(address, (byte) 0, bytes);
        for (int index = 0; index < bands.size(); index++) {
            int[] band = bands.get(index);
            NativeMemoryWrites.putFloat2(address + (long) index * TEXEL_BYTES, band[0], band[1]);
        }
        return upload;
    }

    private Buffer indexUpload(RenderDevice device, int rows) {
        long bytes = (long) WIDTH * rows * TEXEL_BYTES;
        Buffer upload = device.createBuffer(BufferInfo.builder().usage(TRANSFER_SOURCE_USAGE).size(bytes).mapped(true).build());
        long address = upload.mappedAddress();
        NativeMemoryWrites.fill(address, (byte) 0, bytes);
        for (int index = 0; index < bandIndices.size(); index++) {
            NativeMemoryWrites.putFloat(address + (long) index * TEXEL_BYTES, bandIndices.getInt(index));
        }
        return upload;
    }

    public void destroy(RenderDevice device) {
        device.release(curvesTexture);
        device.release(bandTableTexture);
        device.release(bandIndicesTexture);
    }
}

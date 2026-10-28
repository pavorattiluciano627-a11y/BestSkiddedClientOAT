package dev.echo.render.svg;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import dev.echo.client.render.SvgRasterizer;
import dev.echo.client.render.SvgTextureKey;
import dev.echo.client.resources.ClientResources;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.system.MemoryUtil;

/**
 * Document-loading half of the SVG texture cache. The sized RGBA allocation is
 * transferred to the caller; parsed documents are shared between requested sizes.
 */
public final class SvgDocumentRasterizer {
    private final Map<String, SVGDocument> documents = new ConcurrentHashMap<>();

    /** The caller must free the returned native allocation after upload. */
    public ByteBuffer rasterize(SvgTextureKey key) {
        ByteBuffer pixels = MemoryUtil.memAlloc(key.width() * key.height() * 4);
        try {
            SVGDocument document = documents.computeIfAbsent(key.resourcePath(), this::loadDocument);
            SvgRasterizer.downsample(SvgRasterizer.renderSupersampled(document, key.width(), key.height()),
                    key.width(), key.height(), key.colorMode(), pixels);
            return pixels.flip();
        } catch (RuntimeException | Error failure) {
            MemoryUtil.memFree(pixels);
            throw failure;
        }
    }

    public SVGDocument loadDocument(String resourcePath) {
        byte[] contents = ClientResources.readBytes(resourcePath);
        if (contents == null) {
            throw new IllegalArgumentException("SVG not found: " + resourcePath);
        }
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(contents),
                URI.create("resource:/" + resourcePath.replace(' ', '_')), LoaderContext.createDefault());
        if (document == null) {
            throw new IllegalStateException("Failed to parse SVG: " + resourcePath);
        }
        return document;
    }

    /** Called after worker shutdown and sized-texture invalidation. */
    public void clearDocuments() {
        documents.clear();
    }
}

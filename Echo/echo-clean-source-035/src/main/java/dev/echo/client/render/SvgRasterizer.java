package dev.echo.client.render;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;
import dev.echo.client.render.svg.SvgColorMath;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

/**
 * Reconstructed SVG pixel pipeline, pending the control-flow proofs in needs.json.
 * Renders at four times the destination dimensions and averages premultiplied
 * linear-light colors by exact pixel coverage before returning straight RGBA.
 */
public final class SvgRasterizer {
    private static final int SUPERSAMPLE = 4;
    private static final double[] SRGB_TO_LINEAR = new double[256];

    static {
        for (int channel = 0; channel < SRGB_TO_LINEAR.length; channel++) {
            SRGB_TO_LINEAR[channel] = SvgColorMath.srgbToLinear(channel);
        }
    }

    private SvgRasterizer() {
    }

    /**
     * Allocate native RGBA pixels. The successful caller owns the returned buffer
     * and must call MemoryUtil.memFree after texture upload, even if upload fails.
     * A failure during rasterization releases the allocation here.
     */
    public static ByteBuffer rasterize(SVGDocument document, int width, int height,
                                       SvgColorMode colorMode) {
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            downsample(renderSupersampled(document, width, height), width, height, colorMode, pixels);
            return pixels.flip();
        } catch (RuntimeException | Error failure) {
            MemoryUtil.memFree(pixels);
            throw failure;
        }
    }

    /** The image uses non-premultiplied ARGB, matching BufferedImage type 2. */
    public static BufferedImage renderSupersampled(SVGDocument document, int width, int height) {
        int renderWidth = width * SUPERSAMPLE;
        int renderHeight = height * SUPERSAMPLE;
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            document.render(null, graphics, new ViewBox(0.0F, 0.0F, renderWidth, renderHeight));
            return image;
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Append RGBA bytes to output, leaving its position at the end of the pixels.
     * The input is the tightly packed TYPE_INT_ARGB image created above.
     */
    public static void downsample(BufferedImage image, int width, int height,
                                  SvgColorMode colorMode, ByteBuffer output) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int[] source = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        double scaleX = (double) sourceWidth / width;
        double scaleY = (double) sourceHeight / height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double left = x * scaleX;
                double right = (x + 1) * scaleX;
                double top = y * scaleY;
                double bottom = (y + 1) * scaleY;
                int firstX = Math.max(0, (int) Math.floor(left));
                int lastX = Math.min(sourceWidth - 1, (int) Math.ceil(right) - 1);
                int firstY = Math.max(0, (int) Math.floor(top));
                int lastY = Math.min(sourceHeight - 1, (int) Math.ceil(bottom) - 1);
                double alphaSum = 0.0;
                double redSum = 0.0;
                double greenSum = 0.0;
                double blueSum = 0.0;
                double areaSum = 0.0;

                for (int sourceY = firstY; sourceY <= lastY; sourceY++) {
                    double overlapY = Math.max(0.0,
                            Math.min(bottom, sourceY + 1) - Math.max(top, sourceY));
                    int rowOffset = sourceY * sourceWidth;
                    for (int sourceX = firstX; sourceX <= lastX; sourceX++) {
                        double overlapX = Math.max(0.0,
                                Math.min(right, sourceX + 1) - Math.max(left, sourceX));
                        double area = overlapX * overlapY;
                        if (area <= 0.0) {
                            continue;
                        }
                        int argb = source[rowOffset + sourceX];
                        double alpha = (argb >>> 24 & 0xFF) / 255.0;
                        alphaSum += alpha * area;
                        areaSum += area;
                        if (colorMode == SvgColorMode.PRESERVE && alpha > 0.0) {
                            redSum += SRGB_TO_LINEAR[argb >>> 16 & 0xFF] * alpha * area;
                            greenSum += SRGB_TO_LINEAR[argb >>> 8 & 0xFF] * alpha * area;
                            blueSum += SRGB_TO_LINEAR[argb & 0xFF] * alpha * area;
                        }
                    }
                }

                int alpha = SvgColorMath.clampChannel((int) Math.round(
                        alphaSum / Math.max(areaSum, 1.0E-8) * 255.0));
                if (colorMode == SvgColorMode.ALPHA_MASK) {
                    output.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) alpha);
                } else if (alphaSum > 1.0E-8) {
                    output.put((byte) linearToSrgb(redSum / alphaSum));
                    output.put((byte) linearToSrgb(greenSum / alphaSum));
                    output.put((byte) linearToSrgb(blueSum / alphaSum));
                    output.put((byte) alpha);
                } else {
                    output.putInt(0);
                }
            }
        }
    }

    /** Convert unpremultiplied linear color back to a rounded sRGB byte. */
    public static int linearToSrgb(double linear) {
        double clamped = Math.max(0.0, Math.min(1.0, linear));
        double srgb = clamped <= 0.0031308
                ? clamped * 12.92
                : 1.055 * Math.pow(clamped, 0.4166666666666667) - 0.055;
        return SvgColorMath.clampChannel((int) Math.round(srgb * 255.0));
    }
}

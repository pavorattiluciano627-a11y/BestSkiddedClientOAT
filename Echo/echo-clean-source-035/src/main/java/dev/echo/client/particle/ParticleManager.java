package dev.echo.client.particle;

import dev.echo.client.Echo;
import dev.echo.client.events.Event;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.render.DepthTestedWorldGeometry;
import dev.echo.client.render.OverlayWorldGeometry;
import dev.echo.client.resources.ClientResources;
import dev.echo.render.buffer.Buffer;
import dev.echo.render.buffer.BufferInfo;
import dev.echo.render.command.TransferPass;
import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.Texture;
import dev.echo.render.texture.TextureFormat;
import dev.echo.render.texture.TextureInfo;
import dev.echo.render.texture.TextureView;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Active particle storage, render-event updates and the shared RGBA sprite atlas. */
public class ParticleManager {
    private static final String DEFAULT_TEXTURE;
    private static String updateMarker;

    private final Map<Class<? extends Particle>, String> texturePaths;
    private final Map<Class<?>, ParticleUv> textureCoordinates;
    private TextureView texture;
    private Particle[] particles;
    private int particleCount;

    static {
        if (getUpdateMarker() == null) {
            setUpdateMarker("IKwNsb");
        }
        DEFAULT_TEXTURE = "textures/fx/glow_circle.png";
    }

    public ParticleManager() {
        String marker = getUpdateMarker();
        texturePaths = new HashMap<>();
        textureCoordinates = new HashMap<>();
        particles = new Particle[64];
        particleCount = 0;
        EventBus.register(this);
        registerTexture(AnimatedParticle.class, DEFAULT_TEXTURE);
        if (marker == null) {
            Event.setEventCounter(Event.getEventCounter() + 1);
        }
    }

    public void add(Particle particle) {
        String marker = getUpdateMarker();
        if (marker != null && particleCount >= particles.length) {
            particles = Arrays.copyOf(particles, particles.length * 2);
        }
        // Keep the original post-increment even if the subsequent array store fails.
        particles[particleCount++] = particle;
        if (Event.getEventCounterFallbackValue() == 0) {
            setUpdateMarker("AMTvP");
        }
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        String marker = getUpdateMarker();
        DepthTestedWorldGeometry depthTested = event.getRenderContext().getDepthTestedGeometry();
        OverlayWorldGeometry overlay = event.getRenderContext().getOverlayGeometry();
        int index = 0;
        while (index < particleCount) {
            Particle particle = particles[index];
            if (marker != null) {
                particle.age++;
                particle.tick(event.getTickProgress());
                if (particle.isDead()) {
                    particles[index] = particles[--particleCount];
                    particles[particleCount] = null;
                    // The swapped particle is updated at this same index.
                    continue;
                }
            }
            if (particle.isThroughWalls()) {
                overlay.drawParticle(particle.x, particle.y, particle.z, particle.getSize(),
                        particle.color, textureCoordinates.get(particle.getClass()), particle.rotation);
                if (marker != null) {
                    index++;
                    continue;
                }
            }
            depthTested.drawParticle(particle.x, particle.y, particle.z, particle.getSize(),
                    particle.color, textureCoordinates.get(particle.getClass()), particle.rotation);
            index++;
            if (marker == null) {
                return;
            }
        }
    }

    public void registerTexture(Class<? extends Particle> particleType, String resourcePath) {
        texturePaths.put(particleType, resourcePath);
    }

    /** Decodes registered images, packs one horizontal atlas, then uploads it. */
    public void initialize() {
        String marker = getUpdateMarker();
        if (texturePaths.isEmpty()) {
            return;
        }
        List<ParticleBuffer> images = new ArrayList<>();
        for (Map.Entry<Class<? extends Particle>, String> entry : texturePaths.entrySet()) {
            byte[] bytes = ClientResources.readBytes(entry.getValue());
            if (marker == null) {
                // Original null-marker path continues with an empty atlas after this read.
                break;
            }
            if (bytes == null) {
                continue;
            }
            ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer components = stack.mallocInt(1);
                STBImage.stbi_set_flip_vertically_on_load(false);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, components, 4);
                MemoryUtil.memFree(encoded);
                if (pixels == null) {
                    continue;
                }
                images.add(new ParticleBuffer(entry.getKey(), pixels, width.get(0), height.get(0)));
            }
        }
        if (marker != null && images.isEmpty()) {
            return;
        }
        int atlasWidth = images.stream().mapToInt(ParticleBuffer::width).sum();
        int atlasHeight = images.stream().mapToInt(ParticleBuffer::height).max().orElse(0);
        // Integer multiplication and calloc's zero-filled padding match the original.
        ByteBuffer atlasPixels = MemoryUtil.memCalloc(atlasWidth * atlasHeight * 4);
        int xOffset = 0;
        for (ParticleBuffer image : images) {
            if (marker == null) {
                return;
            }
            for (int row = 0; row < image.height(); row++) {
                long source = MemoryUtil.memAddress(image.pixels()) + (long) row * image.width() * 4L;
                long destination = MemoryUtil.memAddress(atlasPixels) + ((long) row * atlasWidth + xOffset) * 4L;
                MemoryUtil.memCopy(source, destination, (long) image.width() * 4L);
            }
            float minU = (float) xOffset / (float) atlasWidth;
            float maxU = (float) (xOffset + image.width()) / (float) atlasWidth;
            float maxV = (float) image.height() / (float) atlasHeight;
            textureCoordinates.put(image.particleType(), new ParticleUv(minU, 0.0F, maxU, maxV));
            xOffset += image.width();
            STBImage.stbi_image_free(image.pixels());
        }
        texture = uploadAtlas(Echo.renderDevice, atlasPixels, atlasWidth, atlasHeight);
        MemoryUtil.memFree(atlasPixels);
    }

    /** Inlined original RGBA upload operation; source and staging ownership stay explicit. */
    private static TextureView uploadAtlas(RenderDevice device, ByteBuffer pixels, int width, int height) {
        Texture atlas = device.createTexture(TextureInfo.builder().width(width).height(height)
                .format(TextureFormat.RGBA8Unorm).usage(5).build());
        long byteCount = (long) width * height * 4L;
        Buffer staging = device.createBuffer(BufferInfo.builder().usage(4).size(byteCount).mapped(true).build());
        MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), staging.mappedAddress(), byteCount);
        TransferPass upload = device.beginTransfer();
        upload.uploadTexture(atlas.views[0], staging.wholeSlice());
        upload.end();
        device.release(staging);
        return atlas.views[0];
    }

    public TextureView getTexture() {
        return texture;
    }

    /** Explicit lifecycle operation requested for the clean provider; retains the reusable atlas. */
    public void clear() {
        Arrays.fill(particles, null);
        particleCount = 0;
    }

    public static void setUpdateMarker(String marker) {
        updateMarker = marker;
    }

    public static String getUpdateMarker() {
        return updateMarker;
    }
}

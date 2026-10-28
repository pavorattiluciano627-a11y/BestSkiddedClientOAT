package dev.echo.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.echo.client.Echo;
import dev.echo.render.texture.TextureView;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.system.MemoryUtil;

/**
 * Cached Echo player-head textures built from actual Minecraft skin pixels.
 * Original entry/guard/cache applicability is recorded in the texture-resources
 * receipt; this candidate contains no runtime key or substitute avatar lookup.
 */
public class TextureResources {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final Map<String, TextureView> playerHeads = new HashMap<>();

    private TextureResources() {
    }

    public static TextureView playerHead(PlayerInfo player) {
        if (player == null) {
            return null;
        }
        Identifier skin = player.getSkin().body().texturePath();
        String key = skin.toString();
        TextureView cached = playerHeads.get(key);
        if (cached != null) {
            return cached;
        }
        TextureView head = createPlayerHead(skin);
        if (head == null) {
            return null;
        }
        playerHeads.put(key, head);
        return head;
    }

    private static TextureView createPlayerHead(Identifier skin) {
        NativeImage image = readImage(skin);
        if (image == null) {
            return null;
        }
        try {
            ByteBuffer pixels = extractFaceRgba(image);
            if (pixels == null) {
                return null;
            }
            return ImageDecoder.uploadRgba(Echo.renderDevice, pixels, 8, 8);
        } finally {
            image.close();
        }
    }

    private static NativeImage readImage(Identifier skin) {
        AbstractTexture texture = CLIENT.getTextureManager().getTexture(skin);
        if (texture instanceof DynamicTexture imageTexture && imageTexture.getPixels() != null) {
            return copyImage(imageTexture.getPixels());
        }
        try {
            Optional<Resource> resource = CLIENT.getResourceManager().getResource(skin);
            if (resource.isEmpty()) {
                return null;
            }
            try (InputStream input = resource.get().open()) {
                return NativeImage.read(input);
            }
        } catch (Exception unavailable) {
            return null;
        }
    }

    private static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixel(x, y, source.getPixel(x, y));
            }
        }
        return copy;
    }

    private static ByteBuffer extractFaceRgba(NativeImage skin) {
        if (skin.getWidth() < 64 || skin.getHeight() < 32) {
            return null;
        }
        ByteBuffer pixels = MemoryUtil.memAlloc(256);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int base = skin.getPixel(8 + x, 8 + y);
                int overlay = skin.getPixel(40 + x, 8 + y);
                TexturePixelWriter.putRgba(pixels, blendArgb(base, overlay));
            }
        }
        pixels.flip();
        return pixels;
    }

    private static int blendArgb(int base, int overlay) {
        int overlayAlpha = (overlay >>> 24) & 255;
        if (overlayAlpha == 0) {
            return base;
        }
        float weight = overlayAlpha / 255.0F;
        int baseAlpha = (base >>> 24) & 255;
        int baseRed = (base >>> 16) & 255;
        int baseGreen = (base >>> 8) & 255;
        int baseBlue = base & 255;
        int overlayRed = (overlay >>> 16) & 255;
        int overlayGreen = (overlay >>> 8) & 255;
        int overlayBlue = overlay & 255;
        int alpha = Math.min(255, Math.round(baseAlpha + (255 - baseAlpha) * weight));
        int red = Math.round(baseRed * (1.0F - weight) + overlayRed * weight);
        int green = Math.round(baseGreen * (1.0F - weight) + overlayGreen * weight);
        int blue = Math.round(baseBlue * (1.0F - weight) + overlayBlue * weight);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}

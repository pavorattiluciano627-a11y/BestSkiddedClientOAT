package dev.echo.client.render;

import dev.echo.render.device.RenderDevice;
import dev.echo.render.texture.TextureView;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

/**
 * Persistent item-atlas slots with per-frame requests and published UV entries.
 * Ordinary-source candidate; exact binding/guard requirements, including the
 * cleanup map target, remain in the analysis ledger and are not runtime dispatch.
 */
public class ItemTextureCache {
    private static final int ATLAS_SIZE = 2048;
    private final RenderDevice device;
    private final Map<Object, Request> requests = new LinkedHashMap<>();
    private final Map<Object, Entry> entries = new HashMap<>();
    private final Map<Object, Integer> slots = new HashMap<>();
    private final MinecraftItemAtlasSurface surface = new MinecraftItemAtlasSurface();
    private int tileSize;
    private TextureView atlasTexture;

    public ItemTextureCache(RenderDevice device) {
        this.device = device;
    }

    private record Request(ItemStack stack, LivingEntity entity, int seed) {
    }

    public record Entry(TextureView texture, float uLeft, float vTop, float uRight, float vBottom) {
    }

    private static KeyedItemRenderState createRenderState(ItemStack stack, LivingEntity entity, int seed) {
        KeyedItemRenderState state = new KeyedItemRenderState();
        MinecraftClient client = MinecraftClient.getInstance();
        client.getItemModelManager().clearAndUpdate(state, stack, ItemDisplayContext.GUI, client.world, entity, seed);
        return state;
    }

    public Entry request(ItemStack stack, LivingEntity entity, int seed) {
        if (stack == null || stack.isEmpty() || MinecraftClient.getInstance().world == null || entity == null) {
            return null;
        }
        List<?> key = List.copyOf((List<?>) createRenderState(stack, entity, seed).getModelKey());
        requests.putIfAbsent(key, new Request(stack.copy(), entity, seed));
        return entries.get(key);
    }

    public void clear() {
        releaseAtlas();
    }

    public void flush() {
        entries.clear();
        if (requests.isEmpty()) {
            return;
        }
        ensureAtlas();
        int columns = ATLAS_SIZE / tileSize;
        float uvSize = tileSize / (float) ATLAS_SIZE;
        for (Map.Entry<Object, Request> queued : requests.entrySet()) {
            Request request = queued.getValue();
            KeyedItemRenderState state = createRenderState(request.stack(), request.entity(), request.seed());
            Integer slot = slots.get(queued.getKey());
            boolean newSlot = slot == null;
            if (newSlot) {
                if (slots.size() >= columns * columns) {
                    continue;
                }
                slot = slots.size();
                slots.put(queued.getKey(), slot);
            }
            if (newSlot || state.isAnimated()) {
                surface.renderTile(state, slot % columns * tileSize, slot / columns * tileSize, tileSize, !newSlot);
            }
            float left = slot % columns * uvSize;
            float top = 1.0F - slot / columns * uvSize;
            entries.put(queued.getKey(), new Entry(atlasTexture, left, top, left + uvSize, top - uvSize));
        }
        requests.clear();
    }

    private void ensureAtlas() {
        int requiredTileSize = 16 * MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (atlasTexture != null && tileSize == requiredTileSize) {
            return;
        }
        releaseAtlas();
        tileSize = requiredTileSize;
        surface.allocate();
        atlasTexture = GlTextureBridge.importColor(device, surface.colorView());
    }

    private void releaseAtlas() {
        if (!surface.hasColorTexture()) {
            return;
        }
        surface.releaseTextures();
        atlasTexture = null;
        slots.clear();
        entries.clear();
    }
}

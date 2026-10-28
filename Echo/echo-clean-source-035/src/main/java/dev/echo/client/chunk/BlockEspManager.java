package dev.echo.client.chunk;

import dev.echo.client.Echo;
import dev.echo.client.MinecraftAccess;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.events.world.BlockUpdateEvent;
import dev.echo.client.events.world.ChunkLoadEvent;
import dev.echo.client.events.world.ChunkUnloadEvent;
import dev.echo.client.render.OverlayWorldGeometry;
import dev.echo.client.utils.ChunkCoordinates;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

/** Owner-scoped block highlight registrations, asynchronous scans and cluster tracers. */
public class BlockEspManager implements MinecraftAccess {
    private static final int OUTLINE_FLAG = 0x01000000;
    private static final int TRACER_FLAG = 0x02000000;
    private final Map<Object, Object2IntMap<Block>> registrations = new HashMap<>();
    private final Object2BooleanMap<Object> outlines = new Object2BooleanOpenHashMap<>();
    private final Object2BooleanMap<Object> tracers = new Object2BooleanOpenHashMap<>();
    private final ChunkManager chunks;
    private volatile boolean hasTracers;
    private final ExecutorService scanExecutor = Executors.newWorkStealingPool();

    public BlockEspManager() {
        EventBus.register(this);
        chunks = Echo.chunkManager;
        rebuildPalette();
    }

    @Subscribe
    public void onChunkLoad(ChunkLoadEvent event) {
        WorldChunk chunk = event.getChunk();
        chunks.removeChunk(chunk.getPos());
        scanExecutor.submit(() -> searchChunk(chunk));
    }

    @Subscribe
    public void onChunkUnload(ChunkUnloadEvent event) {
        chunks.removeChunk(event.getChunkPos());
    }

    @Subscribe
    public void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos position = event.getPosition();
        Block block = event.getState().getBlock();
        int blockId = Registries.BLOCK.getRawId(block);
        if (blockId != 0 && chunks.palette()[blockId] != 0) {
            chunks.setBlock(position.getX(), position.getY(), position.getZ(), blockId);
        } else {
            chunks.removeBlock(position.getX(), position.getY(), position.getZ());
        }
    }

    @Subscribe
    public void onWorldContext(WorldTickContextEvent event) {
        chunks.clear();
        if (!registrations.isEmpty()) {
            searchWorld();
        }
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        if (!hasTracers) {
            return;
        }
        OverlayWorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        int[] palette = chunks.palette();
        ChunkSectionData[] sections = chunks.sections();
        int count = chunks.size();
        for (int index = 0; index < count; index++) {
            ChunkSectionData section = sections[index];
            for (BlockCluster cluster : section.clusters()) {
                int color = palette[cluster.stateId()];
                if (cluster.stateId() == 0 || (color & TRACER_FLAG) == 0) {
                    continue;
                }
                int originX = section.sectionX << 4;
                int originY = section.sectionY << 4;
                int originZ = section.sectionZ << 4;
                geometry.drawTracer(originX + cluster.x(), originY + cluster.y(), originZ + cluster.z(), 0xff000000 | color);
            }
        }
    }

    public void register(Object owner, Iterable<Block> blocks, int color, boolean outline, boolean tracer) {
        Object2IntOpenHashMap<Block> colors = new Object2IntOpenHashMap<>();
        for (Block block : blocks) {
            colors.put(block, color);
        }
        registrations.put(owner, colors);
        outlines.put(owner, outline);
        tracers.put(owner, tracer);
        rebuildPalette();
        searchWorld();
    }

    public void register(Object owner, Object2IntMap<Block> colors, boolean outline, boolean tracer) {
        // Retain the caller's map by reference, as in the original overload.
        registrations.put(owner, colors);
        outlines.put(owner, outline);
        tracers.put(owner, tracer);
        rebuildPalette();
        searchWorld();
    }

    public void updateColor(Object owner, int color) {
        Object2IntMap<Block> colors = registrations.get(owner);
        if (colors == null) { return; }
        colors.replaceAll((block, previous) -> color);
        rebuildPalette();
    }

    public void updateAlpha(Object owner, int alpha) {
        Object2IntMap<Block> colors = registrations.get(owner);
        if (colors == null) { return; }
        colors.replaceAll((block, previous) -> alpha << 24 | previous & 0x00ffffff);
        rebuildPalette();
    }

    public void setOutline(Object owner, boolean outline) {
        if (!outlines.containsKey(owner)) { return; }
        outlines.put(owner, outline);
        rebuildPalette();
    }

    public void setTracers(Object owner, boolean tracer) {
        if (!tracers.containsKey(owner)) { return; }
        tracers.put(owner, tracer);
        rebuildPalette();
    }

    public void unregister(Object owner) {
        outlines.removeBoolean(owner);
        tracers.removeBoolean(owner);
        if (registrations.remove(owner) == null) {
            return;
        }
        rebuildPalette();
        searchWorld();
    }

    public synchronized void rebuildPalette() {
        int[] palette = chunks.palette();
        Arrays.fill(palette, 0);
        DefaultedRegistry<Block> registry = Registries.BLOCK;
        boolean anyTracers = false;
        for (Map.Entry<Object, Object2IntMap<Block>> entry : registrations.entrySet()) {
            boolean tracer = tracers.getBoolean(entry.getKey());
            int flags = (outlines.getBoolean(entry.getKey()) ? OUTLINE_FLAG : 0) | (tracer ? TRACER_FLAG : 0);
            anyTracers |= tracer && !entry.getValue().isEmpty();
            for (Object2IntMap.Entry<Block> color : entry.getValue().object2IntEntrySet()) {
                int blockId = registry.getRawId(color.getKey());
                palette[blockId] = color.getIntValue() & 0xfcffffff | flags;
            }
        }
        hasTracers = anyTracers;
        chunks.notifyPaletteChanged();
    }

    public void searchWorld() {
        chunks.clear();
        scanExecutor.submit(() -> {
            AtomicReferenceArray<WorldChunk> loaded = CLIENT.world.getChunkManager().chunks.chunks;
            chunks.clear();
            IntStream.range(0, loaded.length()).parallel().mapToObj(loaded::get)
                    .filter(Objects::nonNull).forEach(this::searchChunk);
        });
    }

    public void searchChunk(WorldChunk chunk) {
        ChunkSection[] sections = chunk.getSectionArray();
        ChunkPos position = chunk.getPos();
        DefaultedRegistry<Block> registry = Registries.BLOCK;
        int[] palette = chunks.palette();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            if (section == null || section.isEmpty()) {
                continue;
            }
            PalettedContainer<BlockState> states = section.getBlockStateContainer();
            if (!states.hasAny(state -> palette[registry.getRawId(state.getBlock())] != 0)) {
                continue;
            }
            ChunkSectionData found = new ChunkSectionData(ChunkCoordinates.getX(position),
                    chunk.sectionIndexToCoord(sectionIndex), ChunkCoordinates.getZ(position));
            for (int index = 0; index < 4096; index++) {
                BlockState state = states.get(index);
                int blockId = registry.getRawId(state.getBlock());
                if (blockId != 0 && palette[blockId] != 0) {
                    found.append(index, blockId);
                }
            }
            if (found.size() > 0) {
                chunks.putSection(found);
            }
        }
    }
}

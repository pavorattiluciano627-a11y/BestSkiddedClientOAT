package dev.echo.client.features.impl.render;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.BlockCullingEvent;
import dev.echo.client.events.render.BlockLightingEvent;
import dev.echo.client.events.render.BlockRenderSeedEvent;
import dev.echo.client.events.render.BlockRenderTypeEvent;
import dev.echo.client.events.render.FluidRenderEvent;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ItemListSetting;
import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.setting.specialized.XrayBlockItemListSetting;
import dev.echo.client.utils.strings.Concat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Selected-block rendering, exposure testing and reversible chunk-culling state. */
public class XrayFeature extends Feature {
    private static final Direction[] NEIGHBORS = Direction.values();
    private volatile Set<Block> selectedBlocks;
    private ThreadLocal<Boolean> currentBlockExposed;
    private boolean savedChunkCullingEnabled;
    private static final List<RegistryEntryGroup<Item>> ORE_GROUPS = List.of(
            oreGroup("Diamond", ItemTags.DIAMOND_ORES),
            oreGroup("Gold", ItemTags.GOLD_ORES),
            oreGroup("Iron", ItemTags.IRON_ORES),
            oreGroup("Emerald", ItemTags.EMERALD_ORES),
            oreGroup("Redstone", ItemTags.REDSTONE_ORES),
            oreGroup("Lapis", ItemTags.LAPIS_ORES),
            oreGroup("Coal", ItemTags.COAL_ORES),
            oreGroup("Copper", ItemTags.COPPER_ORES));
    private static final Item[] DEFAULT_ITEMS = {
        Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.ANCIENT_DEBRIS,
        Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE,
        Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE,
        Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE,
        Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE,
        Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE,
        Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE,
        Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.NETHER_QUARTZ_ORE
    };
    public static BooleanSetting throughLiquids;
    public static BooleanSetting exposedToAir;
    public static ItemListSetting blocks = new XrayBlockItemListSetting(CatalogText.literal("Blocks"));

    public XrayFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Xray"), dev.echo.client.bootstrap.CatalogText.literal("Renders blocks through walls"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        selectedBlocks = Collections.emptySet();
        currentBlockExposed = ThreadLocal.withInitial(() -> false);
        savedChunkCullingEnabled = true;
        blocks.onChange(this::onSelectedBlocksChanged);
        throughLiquids.onChange(this::reloadIfEnabled);
        exposedToAir.onChange(this::reloadIfEnabled);
    }

    @Override
    public void onEnable() {
        rebuildSelectedBlocks();
        savedChunkCullingEnabled = Objects.requireNonNull(client, "object reference is null").smartCull;
        Objects.requireNonNull(client, "object reference is null").smartCull = false;
        super.onEnable();
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Objects.requireNonNull(client, "object reference is null").smartCull = savedChunkCullingEnabled;
        MinecraftRenderState.reloadWorldRenderer(client);
    }

    @Subscribe
    public void onBlockRenderType(BlockRenderTypeEvent event) {
        if (!isSelectedBlock(event.block)) {
            event.cancel();
        }
    }

    @Subscribe
    public void onBlockLighting(BlockLightingEvent event) {
        event.fullBrightness = true;
    }

    @Subscribe
    public void onBlockRenderSeed(BlockRenderSeedEvent event) {
        currentBlockExposed.set(false);
        if (!exposedToAir.getValue() || !isSelectedBlock(event.state.getBlock())) {
            return;
        }
        currentBlockExposed.set(hasExposedFace(event.state, event.position));
    }

    @Subscribe
    public void onBlockCulling(BlockCullingEvent event) {
        if (!exposedToAir.getValue() || currentBlockExposed.get()) {
            event.cancel();
        }
    }

    @Subscribe
    public void onFluidRender(FluidRenderEvent event) {
        if (throughLiquids.getValue()) {
            event.cancel();
        }
    }

    private static RegistryEntryGroup<Item> oreGroup(String label, TagKey<Item> tag) {
        return new RegistryEntryGroup<>(Concat.strings(label), item -> new ItemStack(item).is(tag));
    }

    private void rebuildSelectedBlocks() {
        initializeFreecamState();
        Set<Block> selected = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (blocks.isEntrySelected(block.asItem())) {
                selected.add(block);
            }
        }
        selectedBlocks = Collections.unmodifiableSet(selected);
    }

    private boolean hasExposedFace(BlockState state, BlockPos position) {
        initializeFreecamState();
        ClientLevel world = client.level;
        if (world == null) {
            return false;
        }
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction side : NEIGHBORS) {
            if (Block.shouldRenderFace(state, world.getBlockState(neighbor.setWithOffset(position, side)), side)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelectedBlock(Block block) {
        return selectedBlocks.contains(block);
    }

    private void onSelectedBlocksChanged() {
        rebuildSelectedBlocks();
        reloadIfEnabled();
    }

    private void reloadIfEnabled() {
        if (isEnabled()) {
            MinecraftRenderState.reloadWorldRenderer(client);
        }
    }

    /** Retains initialization formerly triggered by the Freecam control-state getter. */
    private static void initializeFreecamState() {
        try {
            Class.forName(FreecamFeature.class.getName(), true, FreecamFeature.class.getClassLoader());
        } catch (ClassNotFoundException impossibleForClassLiteral) {
            throw new IllegalStateException("Resolved class initialization failed", impossibleForClassLiteral);
        }
    }

    /** Source-level access for the original nestmate; the original array is retained. */
    public static Item[] getDefaultItems() {
        return DEFAULT_ITEMS;
    }

    /** Source-level access for the original nestmate; group identity remains shared. */
    public static List<RegistryEntryGroup<Item>> getOreGroups() {
        return ORE_GROUPS;
    }

}

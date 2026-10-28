package dev.echo.client.utils.blocks;

import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.text.SegmentedText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

/** Shared, lazy block-tag groups with fresh outer-list copies. */
public final class BlockGroups {
    private BlockGroups() {
    }

    private static List<RegistryEntryGroup<Block>> createGroups() {
        List<RegistryEntryGroup<Block>> groups = new ArrayList<>();
        groups.add(createTagGroup(SegmentedText.literal("Coal Ores"), BlockTagQueries.minecraftTag("coal_ores")));
        groups.add(createTagGroup(SegmentedText.literal("Iron Ores"), BlockTags.IRON_ORES));
        groups.add(createTagGroup(SegmentedText.literal("Gold Ores"), BlockTags.GOLD_ORES));
        groups.add(createTagGroup(SegmentedText.literal("Diamond Ores"), BlockTagQueries.minecraftTag("diamond_ores")));
        groups.add(createTagGroup(SegmentedText.literal("Emerald Ores"), BlockTagQueries.minecraftTag("emerald_ores")));
        groups.add(createTagGroup(SegmentedText.literal("Lapis Ores"), BlockTagQueries.minecraftTag("lapis_ores")));
        groups.add(createTagGroup(SegmentedText.literal("Redstone Ores"), BlockTagQueries.minecraftTag("redstone_ores")));
        groups.add(createTagGroup(SegmentedText.literal("Copper Ores"), BlockTags.COPPER_ORES));
        return groups;
    }

    private static RegistryEntryGroup<Block> createTagGroup(CharSequence label, TagKey<Block> tag) {
        return new RegistryEntryGroup<>(label, block -> BlockTagQueries.contains(tag, block));
    }

    public static List<RegistryEntryGroup<Block>> copyGroups() {
        return new ArrayList<>(Catalog.GROUPS);
    }

    private static final class Catalog {
        private static final List<RegistryEntryGroup<Block>> GROUPS = createGroups();

        private Catalog() {
        }
    }
}

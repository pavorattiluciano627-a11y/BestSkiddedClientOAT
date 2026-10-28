package dev.echo.client.utils.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Direct tag operations used by the block-selection preset builder. */
public final class BlockTagQueries {
    private BlockTagQueries() {}

    // Original member identity and bytecode evidence: symbols.json, startAccount.
    public static TagKey<Block> minecraftTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }

    // Origin: hack/TableResolver.lambda$createTagGroup$0(TagKey,Block)Z.
    public static boolean contains(TagKey<Block> tag, Block block) {
        return block.defaultBlockState().is(tag);
    }
}

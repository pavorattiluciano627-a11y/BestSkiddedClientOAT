package dev.echo.client.utils.blocks;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Direct tag operations used by the block-selection preset builder. */
public final class BlockTagQueries {
    private BlockTagQueries() {}

    // Original member identity and bytecode evidence: symbols.json, startAccount.
    public static TagKey<Block> minecraftTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla(path));
    }

    // Origin: hack/TableResolver.lambda$createTagGroup$0(TagKey,Block)Z.
    public static boolean contains(TagKey<Block> tag, Block block) {
        return block.getDefaultState().isIn(tag);
    }
}

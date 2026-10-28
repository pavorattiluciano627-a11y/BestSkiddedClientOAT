package dev.echo.client.utils.placement;

import java.util.function.Predicate;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;

/**
 * Fixed-join placement predicates and their shared type-check state. Both
 * marker states and every stack value are preserved. Shared-cache applicability
 * remains an outstanding proof before integration into the complete client.
 */
public final class PlacementStackPredicates {
    private static String typeCheckBypassMarker;

    public static final Predicate<ItemStack> WALL_ITEMS = PlacementStackPredicates::isWall;
    public static final Predicate<ItemStack> FULL_CUBE_ITEMS = PlacementStackPredicates::isDefaultStateFullCube;

    static {
        setTypeCheckBypassMarker(null);
    }

    private PlacementStackPredicates() {
    }

    public static void setTypeCheckBypassMarker(String marker) {
        typeCheckBypassMarker = marker;
    }

    public static String typeCheckBypassMarker() {
        return typeCheckBypassMarker;
    }

    public static boolean isWall(ItemStack stack) {
        String marker = typeCheckBypassMarker();
        Item item = stack.getItem();
        if (marker == null && !(item instanceof BlockItem)) {
            return false;
        }
        // With a non-null marker the original performs an unchecked cast.
        BlockItem blockItem = (BlockItem) item;
        return blockItem.getBlock() instanceof WallBlock;
    }

    public static boolean isDefaultStateFullCube(ItemStack stack) {
        String marker = typeCheckBypassMarker();
        Item item = stack.getItem();
        if (marker == null && !(item instanceof BlockItem)) {
            return false;
        }
        Block block = ((BlockItem) item).getBlock();
        // Preserve the fixed IR's explicit initialization before the callee.
        // This initializes a fixed class; it performs no member lookup/dispatch.
        try {
            Class.forName(Block.class.getName(), true, Block.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Resolved class initialization failed", exception);
        }
        return BlockShapeChecks.isDefaultStateFullCube(block);
    }
}

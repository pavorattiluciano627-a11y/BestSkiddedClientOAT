package dev.echo.client.feature.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Inventory mutation shared by the wind-charge/pearl throw features. */
public final class ThrowInventoryOperations {
    private ThrowInventoryOperations() {
    }

    /**
     * Sends the hand-swap action before publishing the corresponding local hand changes.
     * The captured offhand stack is retained by reference, as in both original callers.
     * @param client current client with a live player/network handler
     */
    public static void swapHands(Minecraft client) {
        ItemStack previousOffhand = client.player.getItemInHand(InteractionHand.OFF_HAND);
        client.player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        client.player.setItemInHand(InteractionHand.OFF_HAND, client.player.getItemInHand(InteractionHand.MAIN_HAND));
        client.player.setItemInHand(InteractionHand.MAIN_HAND, previousOffhand);
    }
}

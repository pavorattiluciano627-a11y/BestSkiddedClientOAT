package dev.echo.client.feature.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Inventory mutation shared by the wind-charge/pearl throw features. */
public final class ThrowInventoryOperations {
    private ThrowInventoryOperations() {
    }

    /**
     * Sends the hand-swap action before publishing the corresponding local hand changes.
     * The captured offhand stack is retained by reference, as in both original callers.
     * @param client current client with a live player/network handler
     */
    public static void swapHands(MinecraftClient client) {
        ItemStack previousOffhand = client.player.getStackInHand(Hand.OFF_HAND);
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        client.player.setStackInHand(Hand.OFF_HAND, client.player.getStackInHand(Hand.MAIN_HAND));
        client.player.setStackInHand(Hand.MAIN_HAND, previousOffhand);
    }
}

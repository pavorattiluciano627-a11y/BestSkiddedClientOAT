package dev.echo.client.utils.inventory;

import dev.echo.client.MinecraftAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;

/** Slot interaction and packet operations; recovery applicability is recorded separately. */
public final class SlotPackets {
    private SlotPackets() {
    }

    public static void clickSlot(int syncId, int slot, int button, SlotAction action) {
        if (MinecraftAccess.CLIENT.player == null) {
            return;
        }
        clickSlot(syncId, slot, button, action, MinecraftAccess.CLIENT.player);
    }

    /** Reuses the recovered interaction-manager operation and its original guard order. */
    public static void clickSlot(int syncId, int slot, int button, SlotAction action, Player player) {
        if (player == null) {
            return;
        }
        if (MinecraftAccess.CLIENT.gameMode == null) {
            return;
        }
        MinecraftAccess.CLIENT.gameMode.handleContainerInput(syncId, slot, button, toMinecraftAction(action), player);
    }

    public static void sendClickSlot(int syncId, int slot, int button, SlotAction action) {
        if (MinecraftAccess.CLIENT.getConnection() == null) {
            return;
        }
        MinecraftAccess.CLIENT.getConnection().send(new ServerboundContainerClickPacket(
                syncId, 0, (short) slot, (byte) button, toMinecraftAction(action),
                Int2ObjectMaps.emptyMap(), HashedStack.EMPTY));
    }

    public static SlotAction fromEnum(Object value) {
        if (!(value instanceof Enum<?> enumValue)) {
            return null;
        }
        try {
            return SlotAction.valueOf(enumValue.name());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static String actionName(ServerboundContainerClickPacket packet) {
        return packet.containerInput().name();
    }

    private static ContainerInput toMinecraftAction(SlotAction action) {
        return switch (action) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
        };
    }
}

package dev.echo.client.utils.inventory;

import dev.echo.client.MinecraftAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

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
    public static void clickSlot(int syncId, int slot, int button, SlotAction action, PlayerEntity player) {
        if (player == null) {
            return;
        }
        if (MinecraftAccess.CLIENT.interactionManager == null) {
            return;
        }
        MinecraftAccess.CLIENT.interactionManager.clickSlot(syncId, slot, button, toMinecraftAction(action), player);
    }

    public static void sendClickSlot(int syncId, int slot, int button, SlotAction action) {
        if (MinecraftAccess.CLIENT.getNetworkHandler() == null) {
            return;
        }
        MinecraftAccess.CLIENT.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                syncId, 0, (short) slot, (byte) button, toMinecraftAction(action),
                Int2ObjectMaps.emptyMap(), ItemStackHash.EMPTY));
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

    public static String actionName(ClickSlotC2SPacket packet) {
        return packet.actionType().name();
    }

    private static SlotActionType toMinecraftAction(SlotAction action) {
        return switch (action) {
            case PICKUP -> SlotActionType.PICKUP;
            case QUICK_MOVE -> SlotActionType.QUICK_MOVE;
            case SWAP -> SlotActionType.SWAP;
            case CLONE -> SlotActionType.CLONE;
            case THROW -> SlotActionType.THROW;
            case QUICK_CRAFT -> SlotActionType.QUICK_CRAFT;
            case PICKUP_ALL -> SlotActionType.PICKUP_ALL;
        };
    }
}

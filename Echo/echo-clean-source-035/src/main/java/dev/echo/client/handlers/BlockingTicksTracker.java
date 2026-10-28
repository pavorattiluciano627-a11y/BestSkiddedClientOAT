package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;

/** Counts consecutive ticks during which remote players use an attack-blocking item. */
public final class BlockingTicksTracker extends ClientHandler {
    private static final Map<UUID, Integer> blockingTicks = new HashMap<>();

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (CLIENT.world == null || CLIENT.player == null) {
            blockingTicks.clear();
            return;
        }
        Set<UUID> presentPlayers = new HashSet<>();
        for (PlayerEntity player : CLIENT.world.getPlayers()) {
            if (player == CLIENT.player) {
                continue;
            }
            UUID id = player.getUuid();
            presentPlayers.add(id);
            if (player.isUsingItem()
                    && player.getStackInHand(player.getActiveHand()).contains(DataComponentTypes.BLOCKS_ATTACKS)) {
                blockingTicks.merge(id, 1, Integer::sum);
            } else {
                blockingTicks.remove(id);
            }
        }
        blockingTicks.keySet().removeIf(id -> !presentPlayers.contains(id));
    }

    public static int ticksBlocking(PlayerEntity player) {
        if (player == null) {
            return 0;
        }
        Integer ticks = blockingTicks.get(player.getUuid());
        return ticks == null ? 0 : ticks;
    }
}

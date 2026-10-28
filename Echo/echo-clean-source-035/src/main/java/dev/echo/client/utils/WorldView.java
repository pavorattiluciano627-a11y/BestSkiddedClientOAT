package dev.echo.client.utils;

import dev.echo.client.scripting.api.BlockStateView;
import dev.echo.client.scripting.api.EntityView;
import dev.echo.client.scripting.api.PlayerView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Script-facing view of one retained client world.
 *
 * <p>Analysis candidate for the original normal iteration domain. The original
 * key bounds, EntityView control state, member bindings, and script-facing name
 * mapping remain explicit integration obligations in the adjacent receipt.
 */
public class WorldView {
    private final ClientWorld world;

    /** Retains the supplied reference, including null, as the original constructor does. */
    public WorldView(ClientWorld world) {
        this.world = world;
    }

    /**
     * Wraps players in their current list order. The result's runtime component
     * type is EntityView even though each populated entry is a PlayerView.
     */
    public EntityView[] getPlayers() {
        List<? extends PlayerEntity> players = world.getPlayers();
        EntityView[] result = new EntityView[players.size()];
        for (int index = 0; index < players.size(); index++) {
            result[index] = new PlayerView(players.get(index));
        }
        return result;
    }

    /** Creates fresh wrappers in the world's entity iteration order. */
    public EntityView[] getEntities() {
        ArrayList<EntityView> result = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            result.add(new EntityView(entity));
        }
        return result.toArray(new EntityView[0]);
    }

    /** Queries the retained world at the supplied, unrestricted block coordinates. */
    public BlockStateView getBlock(int x, int y, int z) {
        return new BlockStateView(world.getBlockState(new BlockPos(x, y, z)));
    }
}

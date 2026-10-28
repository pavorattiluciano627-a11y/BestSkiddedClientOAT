package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

/** Base for a client-side player copy whose water state is refreshed explicitly. */
public abstract class ClientPlayerCopy extends OtherClientPlayerEntity {
    protected ClientPlayerCopy(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    protected final void refreshWaterState() {
        this.updateWaterState();
    }
}

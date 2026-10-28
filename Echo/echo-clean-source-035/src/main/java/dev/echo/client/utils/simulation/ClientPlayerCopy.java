package dev.echo.client.utils.simulation;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

/** Base for a client-side player copy whose water state is refreshed explicitly. */
public abstract class ClientPlayerCopy extends RemotePlayer {
    protected ClientPlayerCopy(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    protected final void refreshWaterState() {
        this.updateFluidInteraction();
    }
}

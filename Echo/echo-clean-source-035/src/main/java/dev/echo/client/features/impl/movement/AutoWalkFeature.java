package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;

/** Candidate under the callsite and guard obligations in needs.json. */
public class AutoWalkFeature extends Feature {
    public AutoWalkFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Auto Walk"), dev.echo.client.bootstrap.CatalogText.literal("Automatically walks forward"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    @Subscribe
    public void onMovementInput(PlayerInputEvent event) {
        if (client.player == null || client.world == null || client.player.isDead()) {
            return;
        }
        event.forward = true;
        event.allowMovementInFreecam = true;
    }

}

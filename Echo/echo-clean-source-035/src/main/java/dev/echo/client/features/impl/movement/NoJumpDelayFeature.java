package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.LivingEntityAccessor;
import static dev.echo.client.features.FeatureLiterals.literal;

/** Removes the player's jump cooldown before input is handled. */
public class NoJumpDelayFeature extends Feature {
    public NoJumpDelayFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("No Jump Delay"), dev.echo.client.bootstrap.CatalogText.literal("Removes jump cooldown"), dev.echo.client.features.Category.MOVEMENT));
    }

    @Subscribe
    public void onHandleInput(HandleInputEvent.Pre event) {
        if (client.player == null || client.world == null) return;
        ((LivingEntityAccessor) client.player).setJumpingCooldown(0);
    }

}

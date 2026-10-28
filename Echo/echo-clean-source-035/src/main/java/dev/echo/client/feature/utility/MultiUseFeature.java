package dev.echo.client.feature.utility;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.UsingItemQueryEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;

/** Allows the input handler to attack while the player is using an item. */
public class MultiUseFeature extends Feature {
    public MultiUseFeature() {
        super(new FeatureInfo(CatalogText.literal("Multi Use"),
            CatalogText.literal("Attack and use items in the same tick"), Category.UTILITY, false));
    }

    @Subscribe
    private void onUsingItemQuery(UsingItemQueryEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        event.setUsingItem(false);
    }
}

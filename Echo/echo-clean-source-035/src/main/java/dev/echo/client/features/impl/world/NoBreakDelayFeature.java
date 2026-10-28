package dev.echo.client.features.impl.world;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.text.SegmentedText;

/** Enables the block-break cooldown override consumed by the interaction mixin. */
public class NoBreakDelayFeature extends Feature {
    public NoBreakDelayFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("No Break Delay"), dev.echo.client.bootstrap.CatalogText.literal("Removes block break delay"), dev.echo.client.features.Category.WORLD));
    }

}

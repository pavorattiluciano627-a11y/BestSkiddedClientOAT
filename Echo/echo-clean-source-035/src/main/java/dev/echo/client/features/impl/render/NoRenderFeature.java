package dev.echo.client.features.impl.render;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.FireOverlayEvent;
import dev.echo.client.events.render.FloatingItemOverlayEvent;
import dev.echo.client.events.render.NauseaOverlayEvent;
import dev.echo.client.events.render.PortalOverlayEvent;
import dev.echo.client.events.render.SpyglassOverlayEvent;
import dev.echo.client.events.render.VignetteOverlayEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import static dev.echo.client.features.FeatureLiterals.literal;

/** Suppresses individually selected overlays while its lifecycle latch is active. */
public class NoRenderFeature extends Feature {
    public static BooleanSetting noFireOverlay;
    public static BooleanSetting noNausea;
    public static BooleanSetting noVignette;
    public static BooleanSetting noFloatingItem;
    public static BooleanSetting noSpyglassOverlay;
    public static BooleanSetting noPortalOverlay;
    public static BooleanSetting noCrystalBounce;
    private boolean renderSuppressionActive;

    public NoRenderFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("No Render"), dev.echo.client.bootstrap.CatalogText.literal("Disables certain rendering"), dev.echo.client.features.Category.RENDER, false));
    }

    @Override
    public void postInitialize() {
        renderSuppressionActive = false;
        super.postInitialize();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        renderSuppressionActive = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        renderSuppressionActive = false;
    }

    @Subscribe
    public final void onFireOverlay(FireOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noFireOverlay.getValue()) event.cancel();
    }

    @Subscribe
    public final void onNauseaOverlay(NauseaOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noNausea.getValue()) event.cancel();
    }

    @Subscribe
    public final void onVignetteOverlay(VignetteOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noVignette.getValue()) event.cancel();
    }

    @Subscribe
    public final void onFloatingItemOverlay(FloatingItemOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noFloatingItem.getValue()) event.cancel();
    }

    @Subscribe
    public final void onSpyglassOverlay(SpyglassOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noSpyglassOverlay.getValue()) event.cancel();
    }

    @Subscribe
    public final void onPortalOverlay(PortalOverlayEvent event) {
        if (isOutsideWorld() || !renderSuppressionActive) return;
        if (noPortalOverlay.getValue()) event.cancel();
    }

}

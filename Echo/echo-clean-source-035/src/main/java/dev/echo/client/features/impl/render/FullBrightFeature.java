package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import java.util.Objects;

/** Full-bright state query and lifecycle. Conditional source; NB-092 remains withheld. */
public class FullBrightFeature extends Feature {
    private static volatile FullBrightFeature activeInstance;

    public FullBrightFeature() {
        super(new FeatureInfo(CatalogText.literal("Full Bright"),
                CatalogText.literal("Lights up the world so you can see in the dark"), Category.RENDER));
    }

    @Override
    public void onEnable() {
        activeInstance = this;
        super.onEnable();
    }

    /**
     * NB-092: retains the state-dependent receiver, including its null failure.
     * The Freecam call initializes its real declaring class before the volatile read.
     * This body does not establish the native binding or global flag invariant.
     */
    @Override
    public void onDisable() {
        boolean clearCurrentInstance = FreecamFeature.isPerspectiveRefreshEnabled();
        FullBrightFeature receiver = activeInstance;
        if (clearCurrentInstance) {
            if (receiver == this) activeInstance = null;
            receiver = this;
        }
        Objects.requireNonNull(receiver, "object reference is null").unregisterBaseListeners();
    }

    /** Lexical super invocation on the selected receiver, without virtual redispatch. */
    private void unregisterBaseListeners() {
        super.onDisable();
    }

    /** The fixed caller domain includes the original negative-short null-check path. */
    public static boolean isFullBrightActive() {
        boolean bypass = FreecamFeature.isPerspectiveRefreshBypassed();
        boolean eventsDisabled = Echo.eventsDisabled;
        if (bypass) return eventsDisabled;
        if (eventsDisabled) return false;
        FullBrightFeature instance = activeInstance;
        return instance != null && instance.isEnabled();
    }
}

package dev.echo.client.features.internals;

import dev.echo.client.Echo;
import dev.echo.client.bridge.WindowControl;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.screens.ScreenManager;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.Platform;

/** Opens the dashboard and owns its scrolling and stream-proof controls. */
public class ClickGUIFeature extends Feature {
    public static FloatSetting scrollSpeed;
    public static BooleanSetting streamProof;
    public static final boolean SUPPORTS_STREAM_PROOF;
    private static final String SCROLL_SPEED_TOOLTIP;

    static {
        SCROLL_SPEED_TOOLTIP = "Timing";
        SUPPORTS_STREAM_PROOF =
                Platform.get() == Platform.WINDOWS || Platform.get() == Platform.MACOSX;
    }

    /** Metadata construction precedes the catalog's separate setting-install phase. */
    public ClickGUIFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Click GUI"), dev.echo.client.bootstrap.CatalogText.literal("Click-based GUI"), dev.echo.client.features.Category.INTERNALS, 344));
    }

    @Override
    public void postInitialize() {
        streamProof.onChange(ClickGUIFeature::applyStreamProofSetting);
        streamProof.setVisibilityPredicate(ClickGUIFeature::isStreamProofSettingVisible);
        scrollSpeed.tooltip(SCROLL_SPEED_TOOLTIP);
    }

    /** Opens the stored screen and resets this one-shot action on every exit. */
    @Override
    public void onEnable() {
        try {
            try {
                initializeScreenManagerType();
                if (Echo.screenManager != null) {
                    Echo.screenManager.open();
                }
            } catch (Exception failure) {
                failure.printStackTrace();
            }
        } finally {
            setEnabled(false);
        }
    }

    /** Dashboard visibility is independent of the base action's enabled bit. */
    @Override
    public boolean isEnabled() {
        initializeScreenManagerType();
        return Echo.screenManager != null && Echo.screenManager.isOpen();
    }

    @Override
    public boolean recordsStateChanges() {
        return false;
    }

    @Override
    public boolean persistsEnabledState() {
        return false;
    }

    public float getScrollSpeed() {
        return scrollSpeed.getValue();
    }

    private static boolean isStreamProofSettingVisible(Object context) {
        return SUPPORTS_STREAM_PROOF;
    }

    private static void applyStreamProofSetting() {
        // Keep the effective-value read before the platform flag and the switch
        // before fullscreen invalidation. A switch failure prevents invalidation.
        Echo.setExternalRendererEnabled(streamProof.getValue() && SUPPORTS_STREAM_PROOF);
        Minecraft currentClient = Minecraft.getInstance();
        if (currentClient == null || currentClient.getWindow() == null) {
            return;
        }
        ((WindowControl) (Object) currentClient.getWindow()).invalidateFullscreenState();
    }

    private static void initializeScreenManagerType() {
        try {
            Class.forName(ScreenManager.class.getName(), true, ScreenManager.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
    }

}

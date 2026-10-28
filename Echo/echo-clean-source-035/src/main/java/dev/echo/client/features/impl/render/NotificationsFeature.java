package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.ui.UiSize;

/** Notification layout and live duration setting. */
public class NotificationsFeature extends EchoHudElement {
    private FloatSetting duration;

    public NotificationsFeature() {
        super(new FeatureInfo(CatalogText.literal("Notifications"),
                CatalogText.literal("Timed client event messages"), Category.RENDER),
                HudElementType.NOTIFICATIONS);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        return HudRenderer.INSTANCE.measureElement(layout, HudElementType.NOTIFICATIONS);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer.INSTANCE.drawNotifications(layout);
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        duration.onChange(this::onDurationChanged);
        HudRenderer.INSTANCE.setNotificationDuration(duration.getValue());
        duration.tooltip("Timing");
    }

    private void onDurationChanged() {
        HudRenderer.INSTANCE.setNotificationDuration(duration.getValue());
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(NotificationsFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.duration);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(NotificationsFeature feature, dev.echo.client.setting.FloatSetting preparedDuration) {
        feature.duration = preparedDuration;
    }
}

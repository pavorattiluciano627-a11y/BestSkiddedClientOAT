package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.handlers.LatencyTracker;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudIcon;
import dev.echo.client.hud.HudLayout;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.integration.OfflineRuntimeSession;
import dev.echo.render.FontAsset;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.ui.UiSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Client information bar with the original runtime-text caches and optional metrics. */
public class InfoBarFeature extends EchoHudElement {
    private BooleanSetting showFPS;
    private BooleanSetting showFrameTime;
    private BooleanSetting showPing;
    private BooleanSetting showUsername;
    private String cachedUid;
    private String cachedUsername;

    public InfoBarFeature() {
        super(new FeatureInfo(CatalogText.literal("Info Bar"),
                CatalogText.literal("Client, FPS, profile and UID"), Category.RENDER), HudElementType.INFO_BAR);
    }

    @Override
    protected boolean shouldRender() {
        return !client.debugHudEntryList.isF3Enabled();
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.INFO_BAR.defaultX();
        float y = HudElementType.INFO_BAR.defaultY();
        hud.drawPanel(layout, x, y, layout.getWidth(), 41.0f, 10.0f, 1.0f, 14.0f);
        float cursor = x + 14.0f;
        layout.drawIcon(HudIcon.LOGO, cursor, y + 10.5f, 20.0f, 20.0f, -1, 1.0f);
        cursor += 32.0f;
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Echo", cursor, y + 12.5f, 13.5f, -1, 1.0f);
        cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Echo", 13.5f) + 4.0f;
        layout.drawText(FontAsset.INTER_SEMI_BOLD, "Client", cursor, y + 12.5f, 13.5f, -4144960, 1.0f);
        cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Client", 13.5f);
        for (InfoBarSegment segment : collectSegments()) {
            cursor += 13.0f;
            layout.drawSeparator(cursor, y + 16.0f, 1.0f);
            cursor += 14.0f;
            layout.drawIcon(segment.icon(), cursor, y + 14.5f, 12.0f, 12.0f, -1, 1.0f);
            cursor += 18.0f;
            layout.drawText(FontAsset.INTER_SEMI_BOLD, segment.label(), cursor, y + 13.0f, 13.0f, -1, 1.0f);
            cursor += layout.textWidth(FontAsset.INTER_SEMI_BOLD, segment.label(), 13.0f);
            if (segment.value().isEmpty()) {
                continue;
            }
            cursor += 4.0f;
            layout.drawText(FontAsset.INTER, segment.value(), cursor, y + 13.0f, 13.0f, -1929379841, 1.0f);
            cursor += layout.textWidth(FontAsset.INTER, segment.value(), 13.0f);
        }
        layout.drawHeaderHighlight(x + 64.0f, y + 5.0f, 41.0f, 1.0f, 0.5f, 1.0f);
    }

    @Override
    public UiSize measure(HudElementLayout layout) {
        float width = 46.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Echo", 13.5f)
                + 4.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, "Client", 13.5f) + 14.0f;
        for (InfoBarSegment segment : collectSegments()) {
            width += 45.0f + layout.textWidth(FontAsset.INTER_SEMI_BOLD, segment.label(), 13.0f);
            if (!segment.value().isEmpty()) {
                width += 4.0f + layout.textWidth(FontAsset.INTER, segment.value(), 13.0f);
            }
        }
        return new UiSize(HudLayout.roundWidth(width), 41.0f);
    }

    private List<InfoBarSegment> collectSegments() {
        HudRenderer hud = HudRenderer.INSTANCE;
        ArrayList<InfoBarSegment> segments = new ArrayList<>();
        if (showFPS.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.FLASH, Integer.toString(hud.getFramesPerSecond()), "FPS"));
        }
        if (showFrameTime.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.FLASH,
                    String.format(Locale.ROOT, "%.1f", hud.getFrameTimeMillis()), "MS"));
        }
        if (showPing.getValue()) {
            int latency = getLatency();
            segments.add(new InfoBarSegment(HudIcon.GLOBAL, latency < 0 ? "-" : Integer.toString(latency), "MS"));
        }
        if (showUsername.getValue()) {
            segments.add(new InfoBarSegment(HudIcon.PROFILE, getUsername(), ""));
        }
        segments.add(new InfoBarSegment(HudIcon.TAG, getUid(), "UID"));
        return segments;
    }

    private String getUid() {
        if (cachedUid == null) {
            String value = OfflineRuntimeSession.shared().call("uid", new String[0]);
            if (value == null || value.isEmpty()) {
                return "-";
            }
            cachedUid = value;
        }
        return cachedUid;
    }

    private String getUsername() {
        if (cachedUsername == null) {
            String value = OfflineRuntimeSession.shared().call("username", new String[0]);
            if (value == null || value.isEmpty()) {
                return HudRenderer.INSTANCE.getUsernameFallback();
            }
            cachedUsername = value;
        }
        return cachedUsername;
    }

    private int getLatency() {
        if (client.player == null || client.getNetworkHandler() == null) {
            return -1;
        }
        return LatencyTracker.milliseconds();
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(InfoBarFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.showFPS);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.showFrameTime);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.showPing);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.showUsername);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(InfoBarFeature feature, dev.echo.client.setting.BooleanSetting preparedShowFrameTime, dev.echo.client.setting.BooleanSetting preparedShowFPS, dev.echo.client.setting.BooleanSetting preparedShowPing, dev.echo.client.setting.BooleanSetting preparedShowUsername) {
        feature.showFrameTime = preparedShowFrameTime;
        feature.showFPS = preparedShowFPS;
        feature.showPing = preparedShowPing;
        feature.showUsername = preparedShowUsername;
    }
}

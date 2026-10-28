package dev.echo.client.features.hud;

import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.HudFeature;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.ui.UiSize;
import net.minecraft.client.MinecraftClient;

/** Appearance, reference-canvas placement and balanced rendering for a HUD element. */
public abstract class EchoHudElement extends HudFeature {
    private final HudElementType elementType;
    private final FloatSetting scale;
    private final FloatSetting opacity;

    protected EchoHudElement(FeatureInfo info, HudElementType elementType) {
        super(info);
        scale = new FloatSetting(CatalogText.literal("Scale"), 1.0f, 0.5f, 2.0f, 0.05f,
                CatalogText.literal("x"));
        opacity = new FloatSetting(CatalogText.literal("Opacity"), 1.0f, 0.1f, 1.0f, 0.05f);
        this.elementType = elementType;
        configurePosition(elementType.defaultX(), elementType.defaultY(),
                elementType.defaultWidth(), elementType.defaultHeight(), 1920.0f, 1080.0f, false);
    }

    @Override
    public void postInitialize() {
        super.postInitialize();
        scale.tooltip("Appearance");
        opacity.tooltip("Appearance");
    }

    public HudElementType getElementType() { return elementType; }
    public float getScale() { return scale.getValue(); }
    public float getOpacity() { return opacity.getValue(); }

    @Override
    public float getWidth() {
        return HudRenderer.INSTANCE.getElementSize(elementType).width() * getEffectiveScale();
    }

    @Override
    public float getHeight() {
        return HudRenderer.INSTANCE.getElementSize(elementType).height() * getEffectiveScale();
    }

    private float getEffectiveScale() {
        return HudRenderer.INSTANCE.getCanvasScale() * scale.getValue();
    }

    public UiSize measure(HudElementLayout layout) {
        return UiSize.fromDefaults(elementType);
    }

    public void render(HudElementLayout layout) {
        if (shouldRender()) {
            renderAt(layout, getX(), getY());
        }
    }

    public void renderAt(HudElementLayout layout, float x, float y) {
        UiSize size = HudRenderer.INSTANCE.getElementSize(elementType);
        layout.beginElement(elementType, x, y, size.width(), size.height());
        try {
            renderContents(layout);
        } finally {
            layout.endElement();
        }
    }

    protected abstract void renderContents(HudElementLayout layout);

    protected boolean shouldRender() {
        return true;
    }

    protected boolean isRightOfScreenCenter() {
        int viewportWidth = (int) Math.ceil(
                MinecraftClient.getInstance().getWindow().getFramebufferWidth() / 2.0);
        return getX() + getWidth() * 0.5f > viewportWidth * 0.5f;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(EchoHudElement feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.scale);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, feature.opacity);
    }
}

package dev.echo.client.features.impl.render;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.EntityOutlineEvent;
import dev.echo.client.events.render.OutlineFramebufferEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.hud.ItemHudLayout;
import dev.echo.client.render.EntityMaterialSettings;
import dev.echo.client.render.TexturePasses;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.CustomWidgetSetting;
import dev.echo.client.setting.EntityTypeListSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TextColor;

/** Entity-outline capture, material settings and speed-scaled animation phase. */
public class ChamsFeature extends Feature implements EntityMaterialSettings {
    private static ColorSetting color1;
    private static ColorSetting color2;
    private static FloatSetting lineWidth;
    private static FloatSetting outlineOpacity;
    private static FloatSetting fillOpacity;
    private static FloatSetting glowRadius;
    private static FloatSetting glowIntensity;
    private static FloatSetting speed;
    private static FloatSetting gradientScale;
    private static EntityTypeListSetting entityTypes = new EntityTypeListSetting(literal("Targets"));
    private static BooleanSetting renderSelf;
    private static BooleanSetting teamColors;
    private static CustomWidgetSetting chamsWidget =
            new CustomWidgetSetting(literal("Preview"), 320.0f, ChamsFeature::renderPreview);
    private boolean outlinedEntityThisFrame;
    private float animationPhase;
    private long previousAnimationNanos;

    public ChamsFeature() {
        super(new FeatureInfo(literal("Chams"), literal("Gradient outline shader on entities"), Category.RENDER));
    }

    @Override
    public void postInitialize() {
        color1.setVisibilityPredicate(ignored -> !teamColors.getValue());
        color2.setVisibilityPredicate(ignored -> !teamColors.getValue());
        color1.tooltip("Appearance");
        color2.tooltip("Appearance");
        lineWidth.tooltip("Appearance");
        outlineOpacity.tooltip("Appearance");
        fillOpacity.tooltip("Appearance");
        glowRadius.tooltip("Appearance");
        glowIntensity.tooltip("Appearance");
        speed.tooltip("Timing");
        gradientScale.tooltip("Appearance");
        teamColors.tooltip("Appearance");
        chamsWidget.tooltip("Preview");
    }

    @Override public ColorSetting getColor1Setting() { return color1; }
    @Override public ColorSetting getColor2Setting() { return color2; }
    @Override public FloatSetting getLineWidthSetting() { return lineWidth; }
    @Override public FloatSetting getOutlineOpacitySetting() { return outlineOpacity; }
    @Override public FloatSetting getFillOpacitySetting() { return fillOpacity; }
    @Override public FloatSetting getGlowRadiusSetting() { return glowRadius; }
    @Override public FloatSetting getGlowIntensitySetting() { return glowIntensity; }
    @Override public FloatSetting getGradientScaleSetting() { return gradientScale; }
    @Override public boolean usesTeamColors() { return teamColors.getValue(); }

    @Override
    public float getAnimationPhase() {
        long now = System.nanoTime();
        if (previousAnimationNanos != 0L) {
            float elapsedSeconds = (float) (now - previousAnimationNanos) / 1.0E9f;
            animationPhase = (animationPhase + elapsedSeconds * speed.getValue()) % 1.0f;
        }
        previousAnimationNanos = now;
        return animationPhase;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousAnimationNanos = 0L;
        outlinedEntityThisFrame = false;
        TexturePasses.releaseResources();
    }

    @Subscribe
    private void onEntityOutline(EntityOutlineEvent event) {
        if (shouldOutline(event.getEntity())) {
            event.setOutlined(true);
        }
        if (event.isOutlined()) {
            outlinedEntityThisFrame = true;
        }
    }

    @Subscribe
    private void onOutlineRender(OutlineFramebufferEvent.Render event) {
        event.cancel();
    }

    @Subscribe
    private void onOutlinePrepare(OutlineFramebufferEvent.Prepare event) {
        boolean hadOutlinedEntity = outlinedEntityThisFrame;
        outlinedEntityThisFrame = false;
        if (!hadOutlinedEntity || !event.canDrawEntityOutlines() || event.getFramebuffer() == null) {
            return;
        }
        TexturePasses.applyMaterial(this, event.getFramebuffer());
        TexturePasses.blitToMainFramebuffer(event.getFramebuffer());
        event.cancel();
    }

    private boolean shouldOutline(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity == client.player) {
            return renderSelf.getValue();
        }
        if (entity instanceof PlayerEntity player && TargetsFeature.antiBot.getValue()
                && TargetSelection.isBot(player)) {
            return false;
        }
        return entityTypes.accepts(entity);
    }

    private static void renderPreview(ItemHudLayout context) {
        ChamsFeature feature = Echo.featureManager == null ? null
                : (ChamsFeature) Echo.featureManager.findFeature(ChamsFeature.class);
        if (feature == null) {
            return;
        }
        context.drawEntityMaterial(feature, previewTeamColor());
    }

    private static int previewTeamColor() {
        TextColor nameColor = client.player == null ? null : TargetsFeature.getNameColor(client.player);
        return nameColor == null ? -1 : 0xff000000 | nameColor.getRgb();
    }

    public static void appendBootstrapSettings(ChamsFeature feature) {
        CatalogSettings.append(feature, color1);
        CatalogSettings.append(feature, color2);
        CatalogSettings.append(feature, lineWidth);
        CatalogSettings.append(feature, outlineOpacity);
        CatalogSettings.append(feature, fillOpacity);
        CatalogSettings.append(feature, glowRadius);
        CatalogSettings.append(feature, glowIntensity);
        CatalogSettings.append(feature, speed);
        CatalogSettings.append(feature, gradientScale);
        CatalogSettings.append(feature, entityTypes);
        CatalogSettings.append(feature, renderSelf);
        CatalogSettings.append(feature, teamColors);
        CatalogSettings.append(feature, chamsWidget);
    }

    public static void assignBootstrapSettings(ChamsFeature feature, FloatSetting preparedSpeed,
            ColorSetting preparedColor1, BooleanSetting preparedRenderSelf, FloatSetting preparedGradientScale,
            FloatSetting preparedGlowRadius, FloatSetting preparedLineWidth, ColorSetting preparedColor2,
            FloatSetting preparedOutlineOpacity, BooleanSetting preparedTeamColors,
            FloatSetting preparedFillOpacity, FloatSetting preparedGlowIntensity) {
        speed = preparedSpeed;
        color1 = preparedColor1;
        renderSelf = preparedRenderSelf;
        gradientScale = preparedGradientScale;
        glowRadius = preparedGlowRadius;
        lineWidth = preparedLineWidth;
        color2 = preparedColor2;
        outlineOpacity = preparedOutlineOpacity;
        teamColors = preparedTeamColors;
        fillOpacity = preparedFillOpacity;
        glowIntensity = preparedGlowIntensity;
    }
}

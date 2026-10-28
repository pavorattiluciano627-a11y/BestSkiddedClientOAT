package dev.echo.client.features.impl.render;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.Echo;
import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.HudRenderEvent;
import dev.echo.client.events.render.TextTransformEvent;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.events.world.EntityHitboxEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.internals.TargetsFeature;
import dev.echo.client.hud.ItemHudLayout;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.render.ProjectedBounds;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.render.WorldGeometry;
import dev.echo.client.render.WorldProjection;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.CustomWidgetSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.ui.ArgbMath;
import dev.echo.client.ui.UiBounds;
import dev.echo.client.utils.RandomMath;
import dev.echo.client.utils.targeting.TargetSelection;
import dev.echo.render.FontAsset;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World boxes/tracers and a FIFO of screen-space player overlays. */
public class PlayerESPFeature extends Feature {
    private static final CharSequence NONE = literal("None");
    private static final CharSequence DYNAMIC = literal("Dynamic");
    private static final CharSequence BOX = literal("Box");
    private static final CharSequence TWO_DIMENSIONAL = literal("2D");
    private static final CharSequence LEFT = literal("Left");
    private static final CharSequence RIGHT = literal("Right");
    private static final CharSequence BOTTOM = literal("Bottom");
    private static final CharSequence STATIC = literal("Static");
    private static MultiChoiceSetting modes;
    private static BooleanSetting fill;
    private static BooleanSetting renderSelf;
    private static ChoiceSetting healthBar;
    private static ChoiceSetting healthColor;
    private static ColorSetting fullHealthColor;
    private static ColorSetting lowHealthColor;
    private static ColorSetting healthBarColor;
    private static BooleanSetting showName;
    private static BooleanSetting showHeldItem;
    private static BooleanSetting tracers;
    private static ColorSetting color;
    private static BooleanSetting teamColor;
    private static CustomWidgetSetting playerEspWidget =
            new CustomWidgetSetting(literal("Preview"), 320.0f, PlayerESPFeature::renderPreview);
    private static final float TEXT_SIZE = 7.0f;
    private static final float PREVIEW_HEALTH = 0.65f;
    private Queue<Runnable> overlayCalls;

    public PlayerESPFeature() {
        super(new FeatureInfo(literal("Player ESP"), literal("Highlights players"), Category.RENDER));
    }

    @Override
    public void postInitialize() {
        overlayCalls = new ArrayDeque<>();
        super.postInitialize();
        healthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE));
        fullHealthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(DYNAMIC));
        lowHealthColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(DYNAMIC));
        healthBarColor.setVisibilityPredicate(ignored -> !healthBar.isSelected(NONE) && healthColor.isSelected(STATIC));
        fill.tooltip("Appearance");
        renderSelf.tooltip("Appearance");
        healthColor.tooltip("Appearance");
        fullHealthColor.tooltip("Appearance");
        lowHealthColor.tooltip("Appearance");
        healthBarColor.tooltip("Appearance");
        tracers.tooltip("Appearance");
        color.tooltip("Appearance");
        teamColor.tooltip("Appearance");
        playerEspWidget.tooltip("Preview");
    }

    @Subscribe
    public void onWorldRender(WorldRenderEvent event) {
        if (client.level == null) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getOverlayGeometry();
        UiRenderer renderer = Echo.uiRenderer;
        float tickDelta = event.getTickProgress();
        Vec3 camera = MinecraftRenderState.getCamera(client).position();
        Frustum frustum = new Frustum(event.getViewMatrix(), event.getProjectionMatrix());
        frustum.prepare(camera.x, camera.y, camera.z);
        boolean boxMode = modes.isSelected(BOX);
        boolean screenMode = modes.isSelected(TWO_DIMENSIONAL);
        boolean leftHealth = healthBar.isSelected(LEFT);
        boolean rightHealth = healthBar.isSelected(RIGHT);
        boolean bottomHealth = healthBar.isSelected(BOTTOM);
        boolean drawHealth = leftHealth || rightHealth || bottomHealth;
        int borderColor = 0xff000000;
        float stroke = 0.5f;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player)) {
                continue;
            }
            boolean self = player == client.player;
            if (self && (!renderSelf.getValue() || client.options.getCameraType().isFirstPerson())) {
                continue;
            }
            if (!self && TargetsFeature.antiBot.getValue() && TargetSelection.isBot(player)) {
                continue;
            }
            int argb = playerColor(player);
            int opaqueColor = argb | 0xff000000;
            Vec3 position = player.getPosition(tickDelta);
            EntityHitboxEvent hitbox = new EntityHitboxEvent(player,
                    player.getBoundingBox().inflate((double) player.getPickRadius()));
            hitbox.post();
            AABB box = hitbox.getBoundingBox().move(position.x - player.getX(),
                    position.y - player.getY(), position.z - player.getZ());
            if (boxMode) {
                geometry.drawBox(box.minX, box.minY, box.minZ,
                        box.getXsize(), box.getYsize(), box.getZsize(), argb);
                WorldProjection.drawBoxOutline(geometry, box, opaqueColor);
            }
            if (tracers.getValue() && !self) {
                geometry.drawTracer(position.x, position.y + (double) player.getEyeHeight(), position.z, opaqueColor);
            }
            if (!screenMode && !drawHealth && !showName.getValue() && !showHeldItem.getValue()) {
                continue;
            }
            if (!self && !frustum.isVisible(box)) {
                continue;
            }
            ProjectedBounds projected = WorldProjection.projectBox(event, box);
            if (!projected.visible) {
                continue;
            }
            float x = UiRenderer.snapPixel((float) projected.minX);
            float y = UiRenderer.snapPixel((float) projected.minY);
            float width = UiRenderer.snapPixel((float) projected.maxX) - x;
            float height = UiRenderer.snapPixel((float) projected.maxY) - y;
            if (screenMode) {
                if (fill.getValue()) {
                    overlayCalls.add(() -> renderer.drawRoundedRect(x, y, width, height, 0.0f, argb));
                }
                overlayCalls.add(() -> {
                    WorldProjection.drawRectangleBorder(renderer, x - stroke, y - stroke,
                            width + stroke * 2.0f, height + stroke * 2.0f, stroke * 3.0f, borderColor);
                    WorldProjection.drawRectangleBorder(renderer, x, y, width, height, stroke, opaqueColor);
                });
            }
            if (drawHealth) {
                float health = healthFraction(player);
                int healthArgb = healthColor(health);
                float gap = 1.5f;
                float thickness = RandomMath.clamp(width * 0.06f, 0.75f, 2.0f);
                if (bottomHealth) {
                    float length = Math.max(width * health, thickness);
                    float barY = y + height + gap;
                    overlayCalls.add(() -> {
                        WorldProjection.drawRectangleBorder(renderer, x - stroke, barY - stroke,
                                length + stroke * 2.0f, thickness + stroke * 2.0f, stroke, borderColor);
                        renderer.drawRoundedRect(x, barY, length, thickness, 0.0f, healthArgb);
                    });
                } else {
                    float length = Math.max(height * health, thickness);
                    float barX = rightHealth ? x + width + gap : x - thickness - gap;
                    float barY = y + height - length;
                    overlayCalls.add(() -> {
                        WorldProjection.drawRectangleBorder(renderer, barX - stroke, barY - stroke,
                                thickness + stroke * 2.0f, length + stroke * 2.0f, stroke, borderColor);
                        renderer.drawRoundedRect(barX, barY, thickness, length, 0.0f, healthArgb);
                    });
                }
            }
            if (showName.getValue()) {
                String name = TextTransformEvent.transform(player.getName().getString());
                float textWidth = FontAsset.INTER.measureText(name, TEXT_SIZE);
                float textX = x + width / 2.0f - textWidth / 2.0f;
                float textY = y - TEXT_SIZE - 2.0f;
                overlayCalls.add(() -> renderer.drawTextWithShadow(FontAsset.INTER, name, textX, textY, TEXT_SIZE, -1));
            }
            if (showHeldItem.getValue()) {
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) {
                    continue;
                }
                String name = held.getHoverName().getString();
                float textWidth = FontAsset.INTER.measureText(name, TEXT_SIZE);
                float textX = x + width / 2.0f - textWidth / 2.0f;
                float textY = y + height + 2.0f + (bottomHealth ? 4.0f : 0.0f);
                overlayCalls.add(() -> renderer.drawTextWithShadow(FontAsset.INTER, name, textX, textY, TEXT_SIZE, -1));
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onHudRender(HudRenderEvent event) {
        // Poll before invocation: a failing callback is consumed while the remainder stays queued.
        while (!overlayCalls.isEmpty()) {
            overlayCalls.poll().run();
        }
    }

    private static void renderPreview(ItemHudLayout context) {
        UiBounds bounds = context.getPlayerBounds();
        if (bounds == null) {
            context.drawPlayer();
            return;
        }
        boolean rightHealth = healthBar.isSelected(RIGHT);
        boolean bottomHealth = healthBar.isSelected(BOTTOM);
        boolean drawHealth = rightHealth || bottomHealth || healthBar.isSelected(LEFT);
        int argb = playerColor(client.player);
        float stroke = context.getLineWidth();
        if (tracers.getValue()) {
            context.drawLine(context.getWidth() * 0.12f, context.getHeight(), bounds.centerX(),
                    context.getPlayerEyeY(), stroke, argb | 0xff000000);
        }
        context.drawPlayer();
        context.drawPlayerEsp(bounds, argb, modes.isSelected(BOX), modes.isSelected(TWO_DIMENSIONAL), fill.getValue());
        if (drawHealth) {
            drawPreviewHealth(context, bounds, rightHealth, bottomHealth, stroke);
        }
        if (showName.getValue()) {
            float top = Math.min(bounds.y(), context.getPlayerTop());
            context.drawCenteredText(FontAsset.INTER, previewName(), bounds.centerX(), top - 16.0f, 12.0f, -1);
        }
        if (showHeldItem.getValue()) {
            context.drawCenteredText(FontAsset.INTER, previewHeldItem(), bounds.centerX(),
                    bounds.bottom() + (bottomHealth ? 14 : 6), 12.0f, -1);
        }
    }

    private static void drawPreviewHealth(ItemHudLayout context, UiBounds bounds,
            boolean right, boolean bottom, float stroke) {
        float scale = context.getAlignedLineWidth();
        float gap = scale * 3.0f;
        float thickness = RandomMath.clamp(bounds.width() * 0.06f, scale * 1.5f, scale * 4.0f);
        float length = Math.max((bottom ? bounds.width() : bounds.height()) * PREVIEW_HEALTH, thickness);
        UiBounds healthBounds = bottom
                ? new UiBounds(bounds.x(), bounds.bottom() + gap, length, thickness)
                : new UiBounds(right ? bounds.right() + gap : bounds.x() - thickness - gap,
                        bounds.bottom() - length, thickness, length);
        context.drawRectOutline(healthBounds, stroke, 0xff000000);
        context.drawRect(healthBounds, healthColor(PREVIEW_HEALTH));
    }

    private static int playerColor(Player player) {
        int argb = color.getArgb();
        TextColor nameColor = teamColor.getValue() && player != null ? TargetsFeature.getNameColor(player) : null;
        return nameColor == null ? argb : argb & 0xff000000 | nameColor.getValue() & 0xffffff;
    }

    private static String previewName() {
        String name = client.player == null ? "Player" : client.player.getName().getString();
        return TextTransformEvent.transform(name);
    }

    private static String previewHeldItem() {
        ItemStack held = client.player == null ? ItemStack.EMPTY : client.player.getMainHandItem();
        return held.isEmpty() ? "Diamond Sword" : held.getHoverName().getString();
    }

    private static float healthFraction(Player player) {
        float health = player.getHealth() + player.getAbsorptionAmount();
        return Math.min(Math.max(health / player.getMaxHealth(), 0.0f), 1.0f);
    }

    private static int healthColor(float health) {
        if (healthColor.isSelected(STATIC)) {
            return healthBarColor.getArgb();
        }
        return ArgbMath.interpolate(lowHealthColor.getArgb(), fullHealthColor.getArgb(), health);
    }

    public static void appendBootstrapSettings(PlayerESPFeature feature) {
        CatalogSettings.append(feature, modes);
        CatalogSettings.append(feature, fill);
        CatalogSettings.append(feature, renderSelf);
        CatalogSettings.append(feature, healthBar);
        CatalogSettings.append(feature, healthColor);
        CatalogSettings.append(feature, fullHealthColor);
        CatalogSettings.append(feature, lowHealthColor);
        CatalogSettings.append(feature, healthBarColor);
        CatalogSettings.append(feature, showName);
        CatalogSettings.append(feature, showHeldItem);
        CatalogSettings.append(feature, tracers);
        CatalogSettings.append(feature, color);
        CatalogSettings.append(feature, teamColor);
        CatalogSettings.append(feature, playerEspWidget);
    }

    public static void assignBootstrapSettings(PlayerESPFeature feature, ChoiceSetting preparedHealthColor,
            ColorSetting preparedColor, BooleanSetting preparedTeamColor, ColorSetting preparedFullHealthColor,
            BooleanSetting preparedRenderSelf, BooleanSetting preparedShowName, ColorSetting preparedHealthBarColor,
            BooleanSetting preparedShowHeldItem, BooleanSetting preparedFill, BooleanSetting preparedTracers,
            ColorSetting preparedLowHealthColor, MultiChoiceSetting preparedModes, ChoiceSetting preparedHealthBar) {
        healthColor = preparedHealthColor;
        color = preparedColor;
        teamColor = preparedTeamColor;
        fullHealthColor = preparedFullHealthColor;
        renderSelf = preparedRenderSelf;
        showName = preparedShowName;
        healthBarColor = preparedHealthBarColor;
        showHeldItem = preparedShowHeldItem;
        fill = preparedFill;
        tracers = preparedTracers;
        lowHealthColor = preparedLowHealthColor;
        modes = preparedModes;
        healthBar = preparedHealthBar;
    }
}

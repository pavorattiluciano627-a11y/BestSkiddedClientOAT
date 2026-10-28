package dev.echo.client.features.impl.render;

import static dev.echo.client.bootstrap.CatalogText.literal;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.mixin.PersistentProjectileEntityAccessor;
import dev.echo.client.render.WorldGeometry;
import dev.echo.client.render.WorldProjection;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.utils.trajectory.BlockImpactProjectileTrajectories;
import dev.echo.client.utils.trajectory.BowTrajectories;
import dev.echo.client.utils.trajectory.CrossbowTrajectories;
import dev.echo.client.utils.trajectory.FluidAwareProjectileTrajectories;
import dev.echo.client.utils.trajectory.ThrownProjectileTrajectories;
import dev.echo.client.utils.trajectory.WindChargeTrajectories;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Held-projectile previews and trajectories of projectiles already in the world. */
public class TrajectoriesFeature extends Feature {
    private static final int IMPACT_CIRCLE_SEGMENTS = 24;
    private static final double IMPACT_CIRCLE_RADIUS = 0.4;
    private static final double IMPACT_SURFACE_OFFSET = 0.02;
    private static final CharSequence PEARL = literal("Pearl");
    private static final CharSequence POTION = literal("Potion");
    private static final CharSequence WIND_CHARGE = literal("Wind Charge");
    private static final CharSequence BOW = literal("Bow");
    private static final CharSequence CROSSBOW = literal("Crossbow");
    private static final CharSequence FIREWORK = literal("Firework");
    private static final CharSequence ARROW = literal("Arrow");
    private static MultiChoiceSetting trajectories;
    private static ColorSetting pearlColor;
    private static ColorSetting potionColor;
    private static ColorSetting windChargeColor;
    private static ColorSetting bowColor;
    private static ColorSetting crossbowColor;
    private static ColorSetting fireworkColor;
    private static ColorSetting arrowColor;

    public TrajectoriesFeature() {
        super(new FeatureInfo(literal("Trajectories"), literal("Renders projectile trajectories"), Category.RENDER));
    }

    @Override
    public void postInitialize() {
        // These calls originally resolve fresh text wrappers, rather than reusing the defaults' objects.
        trajectories.setSelected(literal("Pearl"), true);
        trajectories.setSelected(literal("Potion"), true);
        trajectories.setSelected(literal("Wind Charge"), true);
        trajectories.setSelected(literal("Bow"), true);
        trajectories.setSelected(literal("Crossbow"), true);
        trajectories.setSelected(literal("Firework"), true);
        trajectories.setSelected(literal("Arrow"), true);
        pearlColor.setVisibilityPredicate(ignored -> showsPearls());
        potionColor.setVisibilityPredicate(ignored -> showsPotions());
        windChargeColor.setVisibilityPredicate(ignored -> showsWindCharges());
        bowColor.setVisibilityPredicate(ignored -> showsBow());
        crossbowColor.setVisibilityPredicate(ignored -> showsCrossbow());
        fireworkColor.setVisibilityPredicate(ignored -> showsFireworks());
        arrowColor.setVisibilityPredicate(ignored -> showsArrows());
        pearlColor.tooltip("Appearance");
        potionColor.tooltip("Appearance");
        windChargeColor.tooltip("Appearance");
        bowColor.tooltip("Appearance");
        crossbowColor.tooltip("Appearance");
        fireworkColor.tooltip("Appearance");
        arrowColor.tooltip("Appearance");
    }

    @Subscribe
    private void onWorldRender(WorldRenderEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        WorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        float tickDelta = event.getTickProgress();
        if (showsPearls() && holdsItem(Items.ENDER_PEARL)) {
            drawTrajectory(geometry, FluidAwareProjectileTrajectories.fromPlayer(client.player, tickDelta),
                    trajectoryColor(pearlColor));
        }
        if (showsPearls()) {
            int argb = trajectoryColor(pearlColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity.getType() == EntityType.ENDER_PEARL) {
                    drawTrajectory(geometry, FluidAwareProjectileTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsPotions() && holdsPotion()) {
            drawTrajectory(geometry, ThrownProjectileTrajectories.fromPlayer(client.player, tickDelta),
                    trajectoryColor(potionColor));
        }
        if (showsPotions()) {
            int argb = trajectoryColor(potionColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PotionEntity) {
                    drawTrajectory(geometry, ThrownProjectileTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsWindCharges() && holdsItem(Items.WIND_CHARGE)) {
            drawTrajectory(geometry, WindChargeTrajectories.fromPlayer(client.player, tickDelta),
                    trajectoryColor(windChargeColor));
        }
        if (showsWindCharges()) {
            int argb = trajectoryColor(windChargeColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity.getType() == EntityType.WIND_CHARGE) {
                    drawTrajectory(geometry, WindChargeTrajectories.fromEntity(entity), argb);
                }
            }
        }
        if (showsBow() && holdsItem(Items.BOW)) {
            drawTrajectory(geometry, BowTrajectories.fromPlayer(client.player, bowChargeTicks(), tickDelta),
                    trajectoryColor(bowColor));
        }
        if (showsCrossbow() && holdsItem(Items.CROSSBOW)) {
            ItemStack crossbow = heldCrossbow();
            if (!crossbow.isEmpty()) {
                int argb = trajectoryColor(crossbowColor);
                for (List<Vec3d> path : CrossbowTrajectories.fromPlayer(client.player, crossbow, tickDelta)) {
                    drawTrajectory(geometry, path, argb);
                }
            }
        }
        if (showsFireworks()) {
            int argb = trajectoryColor(fireworkColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof FireworkRocketEntity firework) {
                    drawTrajectory(geometry, CrossbowTrajectories.fromFirework(firework), argb);
                }
            }
        }
        if (showsArrows()) {
            int argb = trajectoryColor(arrowColor);
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof PersistentProjectileEntity projectile
                        && !((PersistentProjectileEntityAccessor) projectile).invokeIsInGround()) {
                    drawTrajectory(geometry, BlockImpactProjectileTrajectories.fromEntity(projectile), argb);
                }
            }
        }
    }

    private int bowChargeTicks() {
        if (!client.player.isUsingItem()) {
            return 20;
        }
        ItemStack active = client.player.getActiveItem();
        if (!(active.getItem() instanceof BowItem)) {
            return 20;
        }
        int ticks = active.getMaxUseTime(client.player) - client.player.getItemUseTimeLeft();
        return MathHelper.clamp(ticks, 0, 20);
    }

    private static boolean showsPearls() { return trajectories.isSelected(PEARL); }
    private static boolean showsWindCharges() { return trajectories.isSelected(WIND_CHARGE); }
    private static boolean showsPotions() { return trajectories.isSelected(POTION); }
    private static boolean showsBow() { return trajectories.isSelected(BOW); }
    private static boolean showsCrossbow() { return trajectories.isSelected(CROSSBOW); }
    private static boolean showsFireworks() { return trajectories.isSelected(FIREWORK); }
    private static boolean showsArrows() { return trajectories.isSelected(ARROW); }

    private ItemStack heldCrossbow() {
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() instanceof CrossbowItem) {
            return mainHand;
        }
        ItemStack offHand = client.player.getOffHandStack();
        return offHand.getItem() instanceof CrossbowItem ? offHand : ItemStack.EMPTY;
    }

    private boolean holdsPotion() {
        return holdsItem(Items.SPLASH_POTION) || holdsItem(Items.LINGERING_POTION);
    }

    private static boolean holdsItem(Item item) {
        return client.player.getMainHandStack().getItem() == item
                || client.player.getOffHandStack().getItem() == item;
    }

    private void drawTrajectory(WorldGeometry geometry, List<Vec3d> path, int argb) {
        if (path.size() < 2) {
            return;
        }
        for (int index = 0; index < path.size() - 1; index++) {
            Vec3d start = path.get(index);
            Vec3d end = path.get(index + 1);
            geometry.drawLine(start.x, start.y, start.z, end.x, end.y, end.z, argb);
        }
        Vec3d penultimate = path.get(path.size() - 2);
        Vec3d impact = path.get(path.size() - 1);
        Vec3d normal = new Vec3d(0.0, 1.0, 0.0);
        if (client.world != null) {
            BlockHitResult hit = client.world.raycast(new RaycastContext(penultimate, impact,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
            if (hit.getType() != HitResult.Type.MISS) {
                impact = hit.getPos();
                Direction side = hit.getSide();
                normal = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
                Vec3d towardPrevious = penultimate.subtract(impact);
                if (towardPrevious.lengthSquared() > 1.0E-6 && normal.dotProduct(towardPrevious) < 0.0) {
                    normal = normal.multiply(-1.0);
                }
            }
        }
        Vec3d center = impact.add(normal.normalize().multiply(IMPACT_SURFACE_OFFSET));
        WorldProjection.drawCircle(geometry, center, normal, IMPACT_CIRCLE_RADIUS, IMPACT_CIRCLE_SEGMENTS, argb);
    }

    private int trajectoryColor(ColorSetting setting) {
        return ColorHelper.fromFloats(setting.getAlphaFraction(), setting.getRedFraction(),
                setting.getGreenFraction(), setting.getBlueFraction());
    }

    public static void appendBootstrapSettings(TrajectoriesFeature feature) {
        CatalogSettings.append(feature, trajectories);
        CatalogSettings.append(feature, pearlColor);
        CatalogSettings.append(feature, potionColor);
        CatalogSettings.append(feature, windChargeColor);
        CatalogSettings.append(feature, bowColor);
        CatalogSettings.append(feature, crossbowColor);
        CatalogSettings.append(feature, fireworkColor);
        CatalogSettings.append(feature, arrowColor);
    }

    public static CharSequence bootstrapTrajectoriesOption2() { return PEARL; }
    public static CharSequence bootstrapTrajectoriesOption5() { return POTION; }
    public static CharSequence bootstrapTrajectoriesOption() { return WIND_CHARGE; }
    public static CharSequence bootstrapTrajectoriesOption3() { return BOW; }
    public static CharSequence bootstrapTrajectoriesOption4() { return CROSSBOW; }
    public static CharSequence bootstrapTrajectoriesOption7() { return FIREWORK; }
    public static CharSequence bootstrapTrajectoriesOption6() { return ARROW; }

    public static void assignBootstrapSettings(TrajectoriesFeature feature, ColorSetting preparedArrowColor,
            ColorSetting preparedPearlColor, ColorSetting preparedWindChargeColor, ColorSetting preparedPotionColor,
            ColorSetting preparedFireworkColor, ColorSetting preparedBowColor, ColorSetting preparedCrossbowColor,
            MultiChoiceSetting preparedTrajectories) {
        arrowColor = preparedArrowColor;
        pearlColor = preparedPearlColor;
        windChargeColor = preparedWindChargeColor;
        potionColor = preparedPotionColor;
        fireworkColor = preparedFireworkColor;
        bowColor = preparedBowColor;
        crossbowColor = preparedCrossbowColor;
        trajectories = preparedTrajectories;
    }
}

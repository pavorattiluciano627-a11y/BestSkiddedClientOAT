package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.HandleInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.interaction.BlockBreakingEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.interaction.ItemUseEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.RotationPriority;
import dev.echo.client.handlers.RotationScope;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.items.ActiveHand;
import dev.echo.client.utils.items.ItemProperties;
import dev.echo.client.utils.targeting.AttackRange;
import dev.echo.client.utils.targeting.AttackRangeProperties;
import dev.echo.client.utils.targeting.EntityPredicates;
import dev.echo.client.utils.targeting.TargetSelection;
import dev.echo.client.utils.strings.Concat;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

/** One-tick knockback redirection, preserving the packet/attack two-phase order. */
public class DisplaceFeature extends Feature {
    private static final CharSequence LEFT = CatalogText.literal("Left");
    private static final CharSequence RIGHT = CatalogText.literal("Right");
    private static final CharSequence BACK = CatalogText.literal("Back");
    private static final CharSequence SEPARATOR = CatalogText.literal(" ");
    private static final int ATTACK_DELAY = 1;
    private static final int HAZARD_HORIZONTAL_RANGE = 4;
    private static final int HAZARD_VERTICAL_RANGE = 2;
    private static final int MINIMUM_DROP = 5;
    private static BooleanSetting knockbackEnchant;
    private static BooleanSetting dynamic;
    private static ChoiceSetting direction;
    private static FloatSetting yawOffset;
    private boolean waitingForMovementPacket;
    private boolean attackQueued;
    private boolean resetAfterMovementPacket;
    private boolean performingQueuedAttack;
    private int queuedTargetId;
    private int remainingInputTicks;
    private int lastAttackAge;
    private float queuedYaw;
    private float queuedPitch;
    private RotationScope rotationScope;

    public DisplaceFeature() {
        super(new FeatureInfo(CatalogText.literal("Displace"),
                CatalogText.literal("Delays knockback hits by one tick to redirect knockback"), Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        queuedTargetId = -1;
        lastAttackAge = -1;
        yawOffset.setVisibilityPredicate(value -> !direction.isSelected(BACK));
        dynamic.description(CatalogText.literal("Knock the target toward nearby fire, lava, or a drop of over 5 blocks when one is within 4 blocks, otherwise use Direction"));
    }

    @Override
    public CharSequence getSuffix() {
        if (direction.isSelected(BACK)) return BACK;
        Concat degrees = Concat.integer(Math.round(yawOffset.getValue()));
        return Concat.sequences(direction.isSelected(RIGHT) ? RIGHT : LEFT, SEPARATOR, degrees);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        closeRotationScopeAndReset();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onAttackAttempt(AttackAttemptEvent event) {
        if (attackedThisTick() && event.getTarget() == null) {
            event.cancel();
            return;
        }
        if (!shouldQueueAttack(event)) return;
        queuedYaw = queuedYaw((LivingEntity) event.getTarget());
        queuedPitch = client.player.getPitch();
        queuedTargetId = event.getTarget().getId();
        remainingInputTicks = ATTACK_DELAY;
        rotationScope = rotations().override(this, RotationPriority.CRITICAL,
                new Rotation(queuedYaw, queuedPitch)).orElse(null);
        if (rotationScope == null) return;
        waitingForMovementPacket = true;
        attackQueued = false;
        performingQueuedAttack = false;
        event.cancel();
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void afterMovementPacket(MovementPacketEvent.Post event) {
        if (waitingForMovementPacket) {
            waitingForMovementPacket = false;
            attackQueued = true;
            return;
        }
        if (!resetAfterMovementPacket) return;
        resetAttackState();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void beforeHandleInput(HandleInputEvent.Pre event) {
        if (!attackQueued) return;
        remainingInputTicks--;
        if (remainingInputTicks < 0) {
            closeRotationScopeAndReset();
            return;
        }
        Entity target = client.world == null ? null : client.world.getEntityById(queuedTargetId);
        if (!canAttack(target) || !intersectsCrosshair(target)) {
            closeRotationScopeAndReset();
            return;
        }
        attackQueued = false;
        performingQueuedAttack = true;
        lastAttackAge = client.player.age;
        client.interactionManager.attackEntity(client.player, target);
        if (!client.player.isSpectator()) ActiveHand.swingHand(client.player, Hand.MAIN_HAND);
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void beforeBlockBreaking(BlockBreakingEvent.Pre event) {
        if (attackedThisTick()) event.cancel();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void beforeItemUse(ItemUseEvent.Pre event) {
        if (attackedThisTick() && event.isBlockHit()) event.cancel();
    }

    private boolean attackedThisTick() {
        return client.player != null && client.player.age == lastAttackAge;
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void afterEntityAttack(EntityAttackEvent.Post event) {
        if (!performingQueuedAttack) return;
        if (client.player == null || event.getPlayer() != client.player) return;
        if (event.getTarget() == null || event.getTarget().getId() != queuedTargetId) return;
        if (rotationScope != null) rotationScope.close();
        rotationScope = null;
        resetAfterMovementPacket = true;
    }

    private boolean shouldQueueAttack(AttackAttemptEvent event) {
        if (isOutsideWorld() || event.cancelled || waitingForMovementPacket || attackQueued
                || resetAfterMovementPacket || performingQueuedAttack) return false;
        if (event.getPlayer() != client.player || !(event.getTarget() instanceof LivingEntity target)) return false;
        if (!TargetSelection.isValidTarget(target)) return false;
        if (!(client.player.getAttackCooldownProgress(0.5F) > 0.9F)) return false;
        if (target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE) >= 1.0) return false;
        if (target instanceof PlayerEntity player && EntityPredicates.isBlocking(player)) return false;
        if (!hasKnockback()) return false;
        return canAttack(target);
    }

    private boolean canAttack(Entity entity) {
        if (!(entity instanceof LivingEntity target)) return false;
        if (client.player == null || client.interactionManager == null) return false;
        if (!TargetSelection.isValidTarget(target)) return false;
        ItemStack stack = client.player.getStackInHand(Hand.MAIN_HAND);
        if (!stack.isItemEnabled(client.world.getEnabledFeatures())) return false;
        if (client.player.isRiding()) return false;
        if (client.player.isBelowMinimumAttackCharge(stack, 0)) return false;
        return AttackRange.canAttack(client.player, target);
    }

    private float queuedYaw(LivingEntity target) {
        if (dynamic.getValue()) {
            BlockPos origin = target.getBlockPos();
            BlockPos hazard = BlockPos.findClosest(origin, HAZARD_HORIZONTAL_RANGE, HAZARD_VERTICAL_RANGE,
                    pos -> isHazard(pos, origin.getY())).orElse(null);
            if (hazard != null) {
                Vec3d destination = Vec3d.ofCenter(hazard);
                double x = destination.x - target.getX();
                double z = destination.z - target.getZ();
                if (x * x + z * z > 0.01) {
                    return Rotation.lookingAt(target.getEntityPos(), destination).yaw();
                }
            }
        }
        float yaw = client.player.getYaw();
        if (direction.isSelected(LEFT)) return MathHelper.wrapDegrees(yaw - yawOffset.getValue());
        if (direction.isSelected(RIGHT)) return MathHelper.wrapDegrees(yaw + yawOffset.getValue());
        return MathHelper.wrapDegrees(yaw + 180.0F);
    }

    private boolean hasKnockback() {
        if (client.player.isSprinting()) return true;
        if (!knockbackEnchant.getValue()) return false;
        return ItemProperties.hasEnchantment(client.player.getStackInHand(Hand.MAIN_HAND), Enchantments.KNOCKBACK);
    }

    private void resetAttackState() {
        rotationScope = null;
        waitingForMovementPacket = false;
        attackQueued = false;
        resetAfterMovementPacket = false;
        performingQueuedAttack = false;
        queuedTargetId = -1;
        remainingInputTicks = 0;
    }

    private void closeRotationScopeAndReset() {
        if (rotationScope != null) rotationScope.close();
        resetAttackState();
    }

    private boolean intersectsCrosshair(Entity target) {
        Vec3d eyes = client.player.getCameraPosVec(1.0F);
        Vec3d look = client.player.getRotationVector(client.player.getPitch(), client.player.getYaw());
        Vec3d end = eyes.add(look.multiply(AttackRangeProperties.getEffectiveMaxRange(client.player)));
        Box box = target.getBoundingBox().expand(target.getTargetingMargin());
        return box.contains(eyes) || box.raycast(eyes, end).isPresent();
    }

    private boolean isHazard(BlockPos position, int targetY) {
        if (client.world.getBlockState(position).isIn(BlockTags.FIRE)) return true;
        if (client.world.getFluidState(position).isIn(FluidTags.LAVA)) return true;
        return targetY - client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, position.getX(), position.getZ()) > MINIMUM_DROP;
    }

    public static CharSequence bootstrapDirectionOption() { return BACK; }
    public static CharSequence bootstrapDirectionOption3() { return LEFT; }
    public static CharSequence bootstrapDirectionOption2() { return RIGHT; }

    public static void appendBootstrapSettings(DisplaceFeature feature) {
        CatalogSettings.append(feature, knockbackEnchant);
        CatalogSettings.append(feature, dynamic);
        CatalogSettings.append(feature, direction);
        CatalogSettings.append(feature, yawOffset);
    }

    public static void assignBootstrapSettings(DisplaceFeature feature, FloatSetting preparedYawOffset,
            ChoiceSetting preparedDirection, BooleanSetting preparedDynamic, BooleanSetting preparedKnockbackEnchant) {
        yawOffset = preparedYawOffset;
        direction = preparedDirection;
        dynamic = preparedDynamic;
        knockbackEnchant = preparedKnockbackEnchant;
    }
}

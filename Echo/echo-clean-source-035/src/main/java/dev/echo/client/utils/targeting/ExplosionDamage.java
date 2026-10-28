package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal and anchor damage estimates with sampled anchor exposure and the
 * original armor, resistance and enchantment reduction order. Original key,
 * guard, shared-cache and initialization applicability is recorded in the
 * world-04 explosion-damage receipt.
 */
public final class ExplosionDamage implements MinecraftAccess {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ExplosionDamage() {
    }

    /**
     * Samples the box in X/Y/Z order. A hit on acceptedHitBlock counts as exposed;
     * obstructionBlock is tested geometrically before each world raycast.
     */
    public static float exposure(Vec3 explosion, AABB bounds, Entity entity,
                                 BlockPos acceptedHitBlock, BlockPos obstructionBlock) {
        if (explosion == null || bounds == null || entity == null || CLIENT.level == null) {
            return 0.0F;
        }
        double stepX = 1.0 / (bounds.getXsize() * 2.0 + 1.0);
        double stepY = 1.0 / (bounds.getYsize() * 2.0 + 1.0);
        double stepZ = 1.0 / (bounds.getZsize() * 2.0 + 1.0);
        if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) {
            return 0.0F;
        }
        double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
        int exposed = 0;
        int total = 0;
        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    Vec3 sample = new Vec3(
                            Mth.lerp(x, bounds.minX, bounds.maxX) + offsetX,
                            Mth.lerp(y, bounds.minY, bounds.maxY),
                            Mth.lerp(z, bounds.minZ, bounds.maxZ) + offsetZ);
                    if (isExposed(sample, explosion, entity, acceptedHitBlock, obstructionBlock)) {
                        exposed++;
                    }
                    total++;
                }
            }
        }
        return total == 0 ? 0.0F : (float) exposed / (float) total;
    }

    private static boolean isExposed(Vec3 start, Vec3 end, Entity entity,
                                     BlockPos acceptedHitBlock, BlockPos obstructionBlock) {
        if (obstructionBlock != null && intersectsBlock(start, end, obstructionBlock)) {
            return false;
        }
        HitResult hit = CLIENT.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        return acceptedHitBlock != null && hit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(acceptedHitBlock);
    }

    private static boolean intersectsBlock(Vec3 start, Vec3 end, BlockPos blockPos) {
        return new AABB(blockPos).clip(start, end).isPresent();
    }

    /** Estimates an anchor explosion, including an optional proposed obstruction. */
    public static float anchorDamage(LivingEntity entity, BlockPos anchorPosition, BlockPos obstructionBlock) {
        if (entity == null || anchorPosition == null || CLIENT.level == null) {
            return 0.0F;
        }
        Vec3 explosion = Vec3.atCenterOf(anchorPosition);
        float exposure = exposure(explosion, entity.getBoundingBox(), entity, anchorPosition, obstructionBlock);
        return reducedDamage(entity, explosion, 5.0F, exposure);
    }

    /** The original crystal estimate uses full exposure, without an exposure raycast. */
    public static float crystalDamage(LivingEntity entity, Vec3 explosion) {
        if (entity == null || explosion == null || CLIENT.level == null) {
            return 0.0F;
        }
        return reducedDamage(entity, explosion, 6.0F, 1.0F);
    }

    private static float reducedDamage(LivingEntity entity, Vec3 explosion, float power, float exposure) {
        float diameter = power * 2.0F;
        double normalizedDistance = Math.sqrt(entity.distanceToSqr(explosion)) / (double) diameter;
        if (normalizedDistance > 1.0) {
            return 0.0F;
        }
        double impact = (1.0 - normalizedDistance) * (double) exposure;
        float rawDamage = (float) ((impact * impact + impact) / 2.0 * 7.0 * (double) diameter + 1.0);
        float damage = CombatRules.getDamageAfterAbsorb(entity, rawDamage,
                entity.level().damageSources().explosion(null),
                (float) entity.getArmorValue(), (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            damage = Math.max(damage * (float) (25 - (resistance.getAmplifier() + 1) * 5) / 25.0F, 0.0F);
        }
        float protection = 0.0F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemEnchantments enchantments = entity.getItemBySlot(slot)
                    .getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().is(Enchantments.BLAST_PROTECTION)) {
                    protection += (float) (entry.getIntValue() * 2);
                } else if (entry.getKey().is(Enchantments.PROTECTION)) {
                    protection += (float) entry.getIntValue();
                }
            }
        }
        return protection > 0.0F ? CombatRules.getDamageAfterMagicAbsorb(damage, protection) : damage;
    }
}

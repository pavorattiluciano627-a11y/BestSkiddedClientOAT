package dev.echo.client.utils.targeting;

import dev.echo.client.MinecraftAccess;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
    public static float exposure(Vec3d explosion, Box bounds, Entity entity,
                                 BlockPos acceptedHitBlock, BlockPos obstructionBlock) {
        if (explosion == null || bounds == null || entity == null || CLIENT.world == null) {
            return 0.0F;
        }
        double stepX = 1.0 / (bounds.getLengthX() * 2.0 + 1.0);
        double stepY = 1.0 / (bounds.getLengthY() * 2.0 + 1.0);
        double stepZ = 1.0 / (bounds.getLengthZ() * 2.0 + 1.0);
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
                    Vec3d sample = new Vec3d(
                            MathHelper.lerp(x, bounds.minX, bounds.maxX) + offsetX,
                            MathHelper.lerp(y, bounds.minY, bounds.maxY),
                            MathHelper.lerp(z, bounds.minZ, bounds.maxZ) + offsetZ);
                    if (isExposed(sample, explosion, entity, acceptedHitBlock, obstructionBlock)) {
                        exposed++;
                    }
                    total++;
                }
            }
        }
        return total == 0 ? 0.0F : (float) exposed / (float) total;
    }

    private static boolean isExposed(Vec3d start, Vec3d end, Entity entity,
                                     BlockPos acceptedHitBlock, BlockPos obstructionBlock) {
        if (obstructionBlock != null && intersectsBlock(start, end, obstructionBlock)) {
            return false;
        }
        HitResult hit = CLIENT.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        return acceptedHitBlock != null && hit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(acceptedHitBlock);
    }

    private static boolean intersectsBlock(Vec3d start, Vec3d end, BlockPos blockPos) {
        return new Box(blockPos).raycast(start, end).isPresent();
    }

    /** Estimates an anchor explosion, including an optional proposed obstruction. */
    public static float anchorDamage(LivingEntity entity, BlockPos anchorPosition, BlockPos obstructionBlock) {
        if (entity == null || anchorPosition == null || CLIENT.world == null) {
            return 0.0F;
        }
        Vec3d explosion = Vec3d.ofCenter(anchorPosition);
        float exposure = exposure(explosion, entity.getBoundingBox(), entity, anchorPosition, obstructionBlock);
        return reducedDamage(entity, explosion, 5.0F, exposure);
    }

    /** The original crystal estimate uses full exposure, without an exposure raycast. */
    public static float crystalDamage(LivingEntity entity, Vec3d explosion) {
        if (entity == null || explosion == null || CLIENT.world == null) {
            return 0.0F;
        }
        return reducedDamage(entity, explosion, 6.0F, 1.0F);
    }

    private static float reducedDamage(LivingEntity entity, Vec3d explosion, float power, float exposure) {
        float diameter = power * 2.0F;
        double normalizedDistance = Math.sqrt(entity.squaredDistanceTo(explosion)) / (double) diameter;
        if (normalizedDistance > 1.0) {
            return 0.0F;
        }
        double impact = (1.0 - normalizedDistance) * (double) exposure;
        float rawDamage = (float) ((impact * impact + impact) / 2.0 * 7.0 * (double) diameter + 1.0);
        float damage = DamageUtil.getDamageLeft(entity, rawDamage,
                entity.getEntityWorld().getDamageSources().explosion(null),
                (float) entity.getArmor(), (float) entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
        StatusEffectInstance resistance = entity.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) {
            damage = Math.max(damage * (float) (25 - (resistance.getAmplifier() + 1) * 5) / 25.0F, 0.0F);
        }
        float protection = 0.0F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemEnchantmentsComponent enchantments = entity.getEquippedStack(slot)
                    .getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.BLAST_PROTECTION)) {
                    protection += (float) (entry.getIntValue() * 2);
                } else if (entry.getKey().matchesKey(Enchantments.PROTECTION)) {
                    protection += (float) entry.getIntValue();
                }
            }
        }
        return protection > 0.0F ? DamageUtil.getInflictedDamage(damage, protection) : damage;
    }
}

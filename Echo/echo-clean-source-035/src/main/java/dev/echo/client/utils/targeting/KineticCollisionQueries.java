package dev.echo.client.utils.targeting;

import java.util.Collection;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

/** The direct Minecraft collision-query operation used by kinetic attack prediction. */
public final class KineticCollisionQueries {
    private KineticCollisionQueries() {
    }

    public static Collection<EntityHitResult> collectHits(LivingEntity attacker, AttackRange range) {
        return ProjectileUtil.getHitEntitiesAlong(attacker, range,
                target -> PiercingWeapon.canHitEntity(attacker, target),
                ClipContext.Block.COLLIDER).map(blockHit -> List.of(), entityHits -> entityHits);
    }
}

package dev.echo.client.scripting.api;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class HitResultView {
    private final HitResult hit;

    public HitResultView(HitResult hit) {
        this.hit = hit;
    }

    public String getType() {
        return hit.getType().name();
    }

    public Vector3View getPosition() {
        return new Vector3View(hit.getLocation());
    }

    public Vector3iView getBlockPosition() {
        return hit instanceof BlockHitResult block ? new Vector3iView(block.getBlockPos()) : null;
    }

    public EntityView getEntity() {
        return hit instanceof EntityHitResult entity ? new EntityView(entity.getEntity()) : null;
    }
}

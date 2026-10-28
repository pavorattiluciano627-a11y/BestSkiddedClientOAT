package dev.echo.client.utils.trajectory;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Candidate path record. Arrays and lists retain the original reference semantics. */
public record TargetPath(Vec3 launchPosition, Vec3 inheritedVelocity,
        AABB[] targetBoxes, List<AABB> obstructionBoxes) {
    public TargetPath withTargetBoxes(AABB[] boxes) {
        return new TargetPath(launchPosition, inheritedVelocity, boxes, obstructionBoxes);
    }

    public TargetPath withoutObstructions() {
        return new TargetPath(launchPosition, inheritedVelocity, targetBoxes, List.of());
    }
}

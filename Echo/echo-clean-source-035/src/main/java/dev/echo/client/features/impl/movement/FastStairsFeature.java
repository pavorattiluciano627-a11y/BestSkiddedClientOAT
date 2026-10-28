package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Synthesizes jump input while sprinting toward a reachable stair with clear headroom. */
public class FastStairsFeature extends Feature {
    private static final double MAX_CENTER_DISTANCE = 1.01;

    public FastStairsFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Fast Stairs"), dev.echo.client.bootstrap.CatalogText.literal("Perfectly jump while walking up on stairs"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    @Subscribe
    private void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !isEnabled()) {
            return;
        }
        try {
            Class.forName(Screen.class.getName(), true, Screen.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        if (Minecraft.getInstance().gui.screen() != null) {
            return;
        }
        if (client.player.isDeadOrDying() || !client.player.onGround()
                || client.player.isInWater() || client.player.isInLava()
                || !client.player.isSprinting() || !event.forward) {
            return;
        }

        float yawRadians = client.player.getYRot() * ((float) Math.PI / 180.0f);
        double forwardX = -Mth.sin((double) yawRadians);
        double forwardZ = Mth.cos((double) yawRadians);
        CollisionContext context = CollisionContext.of(client.player);
        BlockPos feet = BlockPos.containing(client.player.getX(),
                client.player.getBoundingBox().minY, client.player.getZ());
        BlockState above = client.level.getBlockState(feet.above());
        if (!above.getCollisionShape(client.level, feet.above(), context).isEmpty()) {
            return;
        }
        BlockState twoAbove = client.level.getBlockState(feet.above(2));
        if (!twoAbove.getCollisionShape(client.level, feet.above(2), context).isEmpty()) {
            return;
        }

        double aheadX = client.player.getX() + forwardX;
        double aheadZ = client.player.getZ() + forwardZ;
        BlockPos ahead = BlockPos.containing(aheadX, client.player.getBlockY(), aheadZ);
        BlockState stair = client.level.getBlockState(ahead);
        if (!(stair.getBlock() instanceof StairBlock)) {
            return;
        }
        VoxelShape collision = stair.getCollisionShape(client.level, ahead, context);
        if (collision.isEmpty() || !(collision.max(Direction.Axis.Y) >= 0.5)) {
            return;
        }
        double centerX = ahead.getX() + 0.5;
        double centerZ = ahead.getZ() + 0.5;
        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        double deltaX = centerX - playerX;
        double deltaZ = centerZ - playerZ;
        if (Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) > MAX_CENTER_DISTANCE) {
            return;
        }
        if (client.player.onGround()) {
            event.jump = true;
        }
    }

}

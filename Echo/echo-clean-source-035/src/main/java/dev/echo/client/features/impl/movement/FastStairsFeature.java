package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

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
        if (MinecraftClient.getInstance().currentScreen != null) {
            return;
        }
        if (client.player.isDead() || !client.player.isOnGround()
                || client.player.isTouchingWater() || client.player.isInLava()
                || !client.player.isSprinting() || !event.forward) {
            return;
        }

        float yawRadians = client.player.getYaw() * ((float) Math.PI / 180.0f);
        double forwardX = -MathHelper.sin((double) yawRadians);
        double forwardZ = MathHelper.cos((double) yawRadians);
        ShapeContext context = ShapeContext.of(client.player);
        BlockPos feet = BlockPos.ofFloored(client.player.getX(),
                client.player.getBoundingBox().minY, client.player.getZ());
        BlockState above = client.world.getBlockState(feet.up());
        if (!above.getCollisionShape(client.world, feet.up(), context).isEmpty()) {
            return;
        }
        BlockState twoAbove = client.world.getBlockState(feet.up(2));
        if (!twoAbove.getCollisionShape(client.world, feet.up(2), context).isEmpty()) {
            return;
        }

        double aheadX = client.player.getX() + forwardX;
        double aheadZ = client.player.getZ() + forwardZ;
        BlockPos ahead = BlockPos.ofFloored(aheadX, client.player.getBlockY(), aheadZ);
        BlockState stair = client.world.getBlockState(ahead);
        if (!(stair.getBlock() instanceof StairsBlock)) {
            return;
        }
        VoxelShape collision = stair.getCollisionShape(client.world, ahead, context);
        if (collision.isEmpty() || !(collision.getMax(Direction.Axis.Y) >= 0.5)) {
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
        if (client.player.isOnGround()) {
            event.jump = true;
        }
    }

}

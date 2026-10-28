package dev.echo.client.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps serialized block-hit offsets within the unit block. */
@Mixin(FriendlyByteBuf.class)
public abstract class PacketByteBufMixin {
    @Inject(method = "writeBlockHitResult", at = @At("HEAD"), cancellable = true)
    private void onWriteBlockHitResultHead(BlockHitResult hit, CallbackInfo callback) {
        BlockPos block = hit.getBlockPos();
        Direction side = hit.getDirection();
        Vec3 position = hit.getLocation();
        double offsetX = position.x - block.getX();
        double offsetY = position.y - block.getY();
        double offsetZ = position.z - block.getZ();
        float hitX = (float) Math.clamp(offsetX, 0.0, 1.0);
        float hitY = (float) Math.clamp(offsetY, 0.0, 1.0);
        float hitZ = (float) Math.clamp(offsetZ, 0.0, 1.0);

        FriendlyByteBuf buffer = (FriendlyByteBuf) (Object) this;
        buffer.writeBlockPos(block);
        buffer.writeEnum(side);
        buffer.writeFloat(hitX);
        buffer.writeFloat(hitY);
        buffer.writeFloat(hitZ);
        buffer.writeBoolean(hit.isInside());
        buffer.writeBoolean(hit.isWorldBorderHit());
        callback.cancel();
    }
}

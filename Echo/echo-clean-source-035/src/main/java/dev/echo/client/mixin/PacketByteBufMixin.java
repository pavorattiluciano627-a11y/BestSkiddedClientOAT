package dev.echo.client.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps serialized block-hit offsets within the unit block. */
@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin {
    @Inject(method = "writeBlockHitResult", at = @At("HEAD"), cancellable = true)
    private void onWriteBlockHitResultHead(BlockHitResult hit, CallbackInfo callback) {
        BlockPos block = hit.getBlockPos();
        Direction side = hit.getSide();
        Vec3d position = hit.getPos();
        double offsetX = position.x - block.getX();
        double offsetY = position.y - block.getY();
        double offsetZ = position.z - block.getZ();
        float hitX = (float) Math.clamp(offsetX, 0.0, 1.0);
        float hitY = (float) Math.clamp(offsetY, 0.0, 1.0);
        float hitZ = (float) Math.clamp(offsetZ, 0.0, 1.0);

        PacketByteBuf buffer = (PacketByteBuf) (Object) this;
        buffer.writeBlockPos(block);
        buffer.writeEnumConstant(side);
        buffer.writeFloat(hitX);
        buffer.writeFloat(hitY);
        buffer.writeFloat(hitZ);
        buffer.writeBoolean(hit.isInsideBlock());
        buffer.writeBoolean(hit.isAgainstWorldBorder());
        callback.cancel();
    }
}

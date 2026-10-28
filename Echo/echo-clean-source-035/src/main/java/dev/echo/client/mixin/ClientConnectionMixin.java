package dev.echo.client.mixin;

import dev.echo.client.Echo;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.events.network.PacketSendEvent;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Unique private boolean echo$resendingWithoutFlush;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void beforeHandlePacket(
            Packet<T> packet, PacketListener listener, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void beforeSend(Packet<?> packet, ChannelFutureListener listener,
            boolean flush, CallbackInfo callback) {
        if (Echo.eventsDisabled) {
            return;
        }
        if (echo$resendingWithoutFlush) {
            return;
        }
        PacketSendEvent event = new PacketSendEvent(packet);
        event.post();
        if (event.cancelled) {
            callback.cancel();
        } else if (flush && !event.shouldFlush()) {
            callback.cancel();
            echo$resendingWithoutFlush = true;
            try {
                ((ClientConnection) (Object) this).send(packet, listener, false);
            } finally {
                echo$resendingWithoutFlush = false;
            }
        }
    }
}

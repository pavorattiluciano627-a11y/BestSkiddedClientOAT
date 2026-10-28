package dev.echo.client.features.impl.internals;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.utils.chat.ChatHudMessages;
import dev.echo.client.utils.strings.Concat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

/** Logs server transaction identifiers using the original client-executor ordering. */
public class TransactionsFeature extends Feature {
    private static final String TRANSACTION_PREFIX = "Transaction Packet: ";

    public TransactionsFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Transactions"), dev.echo.client.bootstrap.CatalogText.literal("Transaction packet logging. Useful for figuring out the anticheat of a server"), dev.echo.client.features.Category.INTERNALS, false));
    }

    @Subscribe
    private void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPingPacket) {
            client.execute(() -> logTransaction(event));
        }
    }

    private static void logTransaction(PacketReceiveEvent event) {
        int parameter = ((ClientboundPingPacket) event.getPacket()).getId();
        Component message = Component.nullToEmpty(Concat.sequences(TRANSACTION_PREFIX, Concat.integer(parameter)).toString());
        ChatHudMessages.addMessage(message);
    }

}

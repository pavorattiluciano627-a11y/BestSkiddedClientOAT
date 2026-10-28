package dev.echo.client.handlers;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.network.PacketReceiveEvent;
import dev.echo.client.utils.network.PlayerLatency;
import java.util.Arrays;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;

/** Samples server round-trip time and reports the median of the five newest samples. */
public final class LatencyTracker extends ClientHandler {
    private static final int PROBE_INTERVAL_TICKS = 10;
    private static final int SAMPLE_CAPACITY = 5;
    private static final long STALE_AFTER_MILLIS = 5_000L;
    private static final int[] samples = new int[SAMPLE_CAPACITY];
    private static int writeIndex;
    private static int sampleCount;
    private static volatile int medianMillis = -1;
    private static volatile long lastSampleMillis;
    private int ticks;

    public static int milliseconds() {
        int measured = medianMillis;
        if (measured < 0 || Util.getMillis() - lastSampleMillis > STALE_AFTER_MILLIS) {
            return PlayerLatency.milliseconds();
        }
        return measured;
    }

    @Subscribe
    private void onTick(ClientTickEvent event) {
        if (CLIENT.player == null || CLIENT.getConnection() == null) {
            clearSamples();
            return;
        }
        if (CLIENT.getDebugOverlay().showNetworkCharts()) {
            return;
        }
        if (++ticks % PROBE_INTERVAL_TICKS != 0) {
            return;
        }
        CLIENT.getConnection().send(new ServerboundPingRequestPacket(Util.getMillis()));
    }

    @Subscribe
    private void onPacketReceived(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundPongResponsePacket pong) {
            recordSample((int) (Util.getMillis() - pong.time()));
        }
    }

    private static synchronized void recordSample(int elapsedMillis) {
        if (elapsedMillis < 0) {
            return;
        }
        samples[writeIndex] = elapsedMillis;
        writeIndex = (writeIndex + 1) % SAMPLE_CAPACITY;
        if (sampleCount < SAMPLE_CAPACITY) {
            sampleCount++;
        }
        int[] ordered = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(ordered);
        medianMillis = ordered[sampleCount / 2];
        lastSampleMillis = Util.getMillis();
    }

    private static synchronized void clearSamples() {
        writeIndex = 0;
        sampleCount = 0;
        medianMillis = -1;
    }
}

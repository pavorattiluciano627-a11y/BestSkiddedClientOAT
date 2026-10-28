package dev.echo.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Tracks elapsed monotonic milliseconds and player-age ticks independently. */
public class ActionTimer {
    private long lastMillis;
    private long lastPlayerAge;

    public long currentMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    private long currentPlayerAge() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0L : player.tickCount;
    }

    public void reset() {
        lastMillis = currentMillis();
        lastPlayerAge = currentPlayerAge();
    }

    public boolean hasElapsedMillis(long milliseconds) {
        return currentMillis() - lastMillis >= milliseconds;
    }

    public long elapsedMillis() {
        return currentMillis() - lastMillis;
    }

    public boolean hasElapsedMillis(double milliseconds) {
        return milliseconds == 0.0 || (double) (currentMillis() - lastMillis) >= milliseconds;
    }

    public boolean hasElapsedTicks(int ticks) {
        if (ticks == 0) {
            return true;
        }
        long age = currentPlayerAge();
        if (age < lastPlayerAge) {
            lastPlayerAge = age;
            return false;
        }
        return age - lastPlayerAge >= ticks;
    }

    public int elapsedTicks() {
        return (int) (currentPlayerAge() - lastPlayerAge);
    }
}

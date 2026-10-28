package dev.echo.client.scripting.api;

import dev.echo.client.utils.WorldView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

/** Nullable live client state exposed through fresh script wrapper instances. */
public class MinecraftApi extends ScriptableObject {
    @Override public String getClassName() { return "minecraft"; }

    /** Returns null outside a world, otherwise a fresh wrapper for the current player. */
    @JSFunction("getEvent")
    public static PlayerView getPlayer(Context context, Scriptable receiver, Object[] arguments, Function function) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? null : new PlayerView(player);
    }

    /** Returns null when there is no current client world. */
    @JSFunction("isValue")
    public static WorldView getWorld(Context context, Scriptable receiver, Object[] arguments, Function function) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world == null ? null : new WorldView(world);
    }

    /** Preserves an absent crosshair result and wraps every nonnull hit result. */
    @JSFunction("removeChannel")
    public static HitResultView getCrosshairTarget(Context context, Scriptable receiver, Object[] arguments, Function function) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        return hit == null ? null : new HitResultView(hit);
    }
}

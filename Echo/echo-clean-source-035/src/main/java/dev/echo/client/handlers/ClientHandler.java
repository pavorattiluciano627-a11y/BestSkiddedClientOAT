package dev.echo.client.handlers;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.MinecraftClient;

/** Base for client services that subscribe to the event bus. */
public class ClientHandler implements MinecraftAccess {
    protected static final MinecraftClient CLIENT = MinecraftClient.getInstance();
}

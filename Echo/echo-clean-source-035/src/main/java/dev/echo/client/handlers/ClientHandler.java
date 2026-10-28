package dev.echo.client.handlers;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.Minecraft;

/** Base for client services that subscribe to the event bus. */
public class ClientHandler implements MinecraftAccess {
    protected static final Minecraft CLIENT = Minecraft.getInstance();
}

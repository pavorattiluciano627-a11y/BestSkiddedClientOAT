package dev.echo.client.utils.chat;

import dev.echo.client.MinecraftAccess;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;

/** Direct client-chat delivery using the original shared Minecraft client reference. */
public final class ChatHudMessages implements MinecraftAccess {
    private ChatHudMessages() {
    }

    public static void addMessage(Component message) {
        if (CLIENT.gui == null) {
            return;
        }
        CLIENT.gui.hud.getChat().addMessage(message, null, net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_CLIENT, null);
    }
}

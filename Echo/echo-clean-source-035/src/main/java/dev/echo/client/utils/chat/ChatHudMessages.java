package dev.echo.client.utils.chat;

import dev.echo.client.MinecraftAccess;
import net.minecraft.text.Text;

/** Direct client-chat delivery using the original shared Minecraft client reference. */
public final class ChatHudMessages implements MinecraftAccess {
    private ChatHudMessages() {
    }

    public static void addMessage(Text message) {
        if (CLIENT.inGameHud == null) {
            return;
        }
        CLIENT.inGameHud.getChatHud().addMessage(message);
    }
}

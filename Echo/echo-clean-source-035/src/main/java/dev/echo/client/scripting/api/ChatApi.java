package dev.echo.client.scripting.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;

/** Prints script messages to the local chat HUD. */
public class ChatApi extends ScriptableObject {
    /** {@inheritDoc} */
    @Override
    public String getClassName() { return "chat"; }

    /**
     * Converts the first argument using Rhino and displays it locally.
     * Extra arguments are ignored. The explicit annotation retains the original
     * JavaScript property name while the Java method has a semantic name.
     *
     * @param context the calling Rhino context
     * @param receiver the calling script receiver
     * @param arguments the supplied JavaScript arguments
     * @param function the calling host function
     */
    @JSFunction("writeEvent")
    public static void print(Context context, Scriptable receiver, Object[] arguments, Function function) {
        if (arguments.length == 0) return;
        Component message = Component.literal(Context.toString(arguments[0]));
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) return;
        client.gui.hud.getChat().addMessage(message, null, net.minecraft.client.multiplayer.chat.GuiMessageSource.SYSTEM_CLIENT, null);
    }
}

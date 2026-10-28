package dev.echo.client.screens;

import dev.echo.client.render.GuiDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base-screen candidate; projection targets and API names await central reconciliation. */
public abstract class EchoScreen extends Screen {
    /** Creates a screen with the supplied title. */
    protected EchoScreen(Component title) {
        super(title);
    }

    /** Suppresses the vanilla background: the original body is a single return instruction. */
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        // The client renderer draws the background separately.
    }

    /** Suppresses the vanilla in-game dim layer, as in the original single-return body. */
    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor context) {
        // This is intentional original behavior, not a missing rendering handler.
    }

    /** Converts the vanilla scaled x coordinate to the client's projection coordinates. */
    public float toUiX(float x) {
        return x * GuiDimensions.width()
                / Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** Converts the vanilla scaled y coordinate to the client's projection coordinates. */
    public float toUiY(float y) {
        return y * GuiDimensions.height()
                / Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}

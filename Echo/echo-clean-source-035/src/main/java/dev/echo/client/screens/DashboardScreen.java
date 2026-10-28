package dev.echo.client.screens;

import dev.echo.client.Echo;
import dev.echo.client.events.EventBus;
import dev.echo.client.screens.dashboard.DashboardController;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * Minecraft screen adapter for the dashboard's registered renderer and input state.
 * The original key/cache and opaque-guard applicability conditions remain in the
 * adjacent provider-recovery report.
 */
public final class DashboardScreen extends EchoScreen {
    /** Original ConfigurationService.availablePage; deliberately nonvolatile. */
    private static boolean sharedControlState;

    static {
        setSharedControlState(true);
    }

    private static final String SCREEN_TITLE = new String("Echo");
    private static final String AUTOSAVE_PROFILE = new String("_autosave");

    /** Stores the original shared control flag without introducing synchronization. */
    public static void setSharedControlState(boolean state) {
        sharedControlState = state;
    }

    /** Returns the current shared control flag. */
    public static boolean getSharedControlState() {
        return sharedControlState;
    }

    /** Reads through the original getter and returns its logical inversion. */
    public static boolean isSharedControlStateClear() {
        boolean state = getSharedControlState();
        if (!state) return true;
        return false;
    }

    /** Constructs only the screen; the dashboard controller is acquired during init. */
    public DashboardScreen() {
        super(Text.literal(SCREEN_TITLE));
    }

    /** Initializes the vanilla screen, registers this instance, then opens its controller. */
    @Override
    protected void init() {
        super.init();
        EventBus.register(this);
        DashboardController.INSTANCE.onScreenOpened();
    }

    /** Updates pointer state; the controller's registered render event draws the dashboard. */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        DashboardController.INSTANCE.mouseMoved(toUiX(mouseX), toUiY(mouseY));
    }

    /** Dispatches a press before attempting vanilla child-widget handling. */
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return DashboardController.INSTANCE.mouseClicked(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseClicked(click, doubled);
    }

    /** Preserves the original drag contract: the controller receives position and button. */
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return DashboardController.INSTANCE.mouseDragged(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseDragged(click, deltaX, deltaY);
    }

    /** Delivers release even when the controller declines to consume it. */
    @Override
    public boolean mouseReleased(Click click) {
        return DashboardController.INSTANCE.mouseReleased(
                toUiX((float) click.x()), toUiY((float) click.y()), click.button())
                || super.mouseReleased(click);
    }

    /** Forwards vertical scrolling to the controller and the full event on vanilla fallback. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return DashboardController.INSTANCE.mouseScrolled(
                toUiX((float) mouseX), toUiY((float) mouseY), vertical)
                || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    /** Preserves the physical key, scan code and modifier tuple. */
    @Override
    public boolean keyPressed(KeyInput input) {
        return DashboardController.INSTANCE.keyPressed(input.key(), input.scancode(), input.modifiers())
                || super.keyPressed(input);
    }

    /** Forwards the full Unicode code point without narrowing it to a UTF-16 character. */
    @Override
    public boolean charTyped(CharInput input) {
        return DashboardController.INSTANCE.charTyped(input.codepoint()) || super.charTyped(input);
    }

    /** Unregisters after vanilla removal and schedules the original profile autosave. */
    @Override
    public void removed() {
        super.removed();
        EventBus.unregister(this);
        if (Echo.featureConfig != null) {
            Echo.featureConfig.saveAsync(AUTOSAVE_PROFILE);
        }
    }

    /** The dashboard leaves the integrated game running. */
    @Override
    public boolean shouldPause() {
        return false;
    }
}

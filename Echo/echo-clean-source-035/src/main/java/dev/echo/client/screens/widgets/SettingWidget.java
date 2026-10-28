package dev.echo.client.screens.widgets;

import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.Setting;

/**
 * The original setting-widget rendering and input contract.
 * Renderer and Setting names are integration proposals recorded in interface-needs.json.
 */
public interface SettingWidget {
    /** Draws this widget using the existing client renderer and supplied opacity. */
    void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity);

    /** Returns the widget's current layout height. */
    float height();

    /** Handles a mouse press and reports whether it was consumed. */
    boolean mouseClicked(double mouseX, double mouseY, int button);

    /** Handles a mouse release. */
    void mouseReleased(double mouseX, double mouseY, int button);

    /**
     * Handles a key press. The original interface leaves unhandled keys available
     * to its caller; editable widgets override this method.
     */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles character input; the original default deliberately has no effect. */
    default void charTyped(char character, int modifiers) {
        // Exact original interface default: return without changing state.
    }

    /** Handles vertical scrolling; the original default does not consume it. */
    default boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return false;
    }

    /** Returns the setting edited by this widget. */
    Setting setting();
}

package dev.echo.client.screens.widgets;

import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.setting.KeybindSetting;
import dev.echo.client.setting.Setting;
import dev.echo.client.ui.ThemePalette;
import dev.echo.client.ui.TooltipRenderer;
import dev.echo.client.utils.strings.TextBuffers;
import dev.echo.render.FontAsset;

/**
 * Keyboard/mouse setting editor. Original identity: hack/TypeBuilder.
 * The adjacent receipt records conditional caller/cache/control applicability.
 */
public class KeybindWidget implements SettingWidget {
    // Two independently decoded original string-cache slots, with distinct identities.
    private static final String CAPTURE_RENDER_LABEL = new String("...");
    private static final String CAPTURE_HIT_TEST_LABEL = new String("...");

    private final KeybindSetting setting;
    private float x;
    private float y;
    private boolean capturing;

    public KeybindWidget(KeybindSetting setting) {
        capturing = false;
        this.setting = setting;
    }

    @Override
    public void render(UiRenderer renderer, float x, float y, int mouseX, int mouseY, float opacity) {
        this.x = x;
        this.y = y;
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        boolean hovered = ThemePalette.contains(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, mouseX, mouseY);
        if (hovered) {
            CharSequence description = setting.getDescription();
            if (description != null && description.length() > 0) {
                TooltipRenderer.offer(setting, setting.getLabel(), description, mouseX, mouseY);
            }
        }
        renderer.drawRoundedRect(rowX, y, rowWidth, ThemePalette.ROW_HEIGHT, ThemePalette.ROW_RADIUS,
                ThemePalette.multiplyAlpha((hovered ? ThemePalette.ROW_HOVER : ThemePalette.ROW_BACKGROUND).getRGB(), opacity));
        renderer.drawRoundedRect(rowX, y + 2.0f, 1.5f, ThemePalette.ROW_HEIGHT - 4.0f, 0.0f,
                ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity));
        CharSequence label = TextBuffers.abbreviate(setting.getLabel(), 14);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, label, rowX + ThemePalette.CONTENT_PADDING,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f,
                ThemePalette.multiplyAlpha(ThemePalette.PRIMARY_TEXT, opacity));
        String keyLabel = capturing ? CAPTURE_RENDER_LABEL : setting.getKey().displayLabel();
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 6.0f) + 8.0f;
        float keyX = rowX + rowWidth - ThemePalette.CONTENT_PADDING - keyWidth;
        float keyY = y + (ThemePalette.ROW_HEIGHT - 10.0f) / 2.0f;
        int background = capturing
                ? ThemePalette.multiplyAlpha(ThemePalette.ROW_ACCENT.getRGB(), opacity)
                : ThemePalette.multiplyAlpha(ThemePalette.SLOT_BACKGROUND.getRGB(), opacity);
        int foreground = capturing
                ? ThemePalette.multiplyAlpha(ThemePalette.TAB_ACTIVE_TEXT, opacity)
                : ThemePalette.multiplyAlpha(ThemePalette.SELECTED_OPTION_TEXT, opacity);
        renderer.drawRoundedRect(keyX, keyY, keyWidth, 10.0f, 5.0f, background);
        renderer.drawText(FontAsset.INTER_SEMI_BOLD, keyLabel, keyX + 4.0f,
                y + ThemePalette.ROW_HEIGHT / 2.0f - 4.0f, 6.0f, foreground);
    }

    @Override
    public float height() {
        return ThemePalette.ROW_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float rowX = x + ThemePalette.ROW_INDENT;
        float rowWidth = ThemePalette.PANEL_WIDTH - ThemePalette.ROW_INDENT - 4.0f;
        if (capturing) {
            setting.setKey(InputKey.fromMouseCode(button));
            setting.setListeningForKey(false);
            capturing = false;
            return true;
        }
        if (!InputKey.MOUSE_LEFT.matches(button)) {
            return false;
        }
        String keyLabel = capturing ? CAPTURE_HIT_TEST_LABEL : setting.getKey().displayLabel();
        float keyWidth = FontAsset.INTER_SEMI_BOLD.measureText(keyLabel, 6.0f) + 8.0f;
        float keyX = rowX + rowWidth - ThemePalette.CONTENT_PADDING - keyWidth;
        float keyY = y + (ThemePalette.ROW_HEIGHT - 10.0f) / 2.0f;
        if (ThemePalette.contains(keyX, keyY, keyWidth, 10.0f, (float) mouseX, (float) mouseY)) {
            capturing = !capturing;
            setting.setListeningForKey(capturing);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // Original getConnection(DJDI)V is exactly RETURN (one byte), including during capture.
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturing) {
            return false;
        }
        if (keyCode == InputKey.ESCAPE.code()) {
            setting.setKey(InputKey.NONE);
        } else {
            setting.setKey(InputKey.fromCode(keyCode));
        }
        // The original never reads scanCode or modifiers and does not change bind mode.
        setting.setListeningForKey(false);
        capturing = false;
        return true;
    }

    /** Original concrete getter clearJob(J)Lhack/RegistryMapper;. */
    public KeybindSetting getSetting() {
        return setting;
    }

    /** Original interface adapter removeProvider(CIC)Lhack/TransportFactory;. */
    @Override
    public Setting setting() {
        return getSetting();
    }
}

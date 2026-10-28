package dev.echo.client.screens.panels;

import dev.echo.client.input.InputKey;
import dev.echo.client.render.UiRenderer;
import dev.echo.client.ui.ThemePalette;
import dev.echo.render.FontAsset;
import dev.echo.render.texture.TextureView;
import java.awt.Color;

/**
 * Original four-destination navigation bar and its independently owned HUD preview.
 * The source-domain conditions for removed dispatch and control guards are retained
 * in this recovery's evidence; this is a conditional source candidate.
 */
public class NavigationSidebar {
    private static final float BAR_HEIGHT = 30.0F;
    private static final float BUTTON_SIZE = 24.0F;
    private static final float ICON_SIZE = 9.0F;
    private static final float BUTTON_GAP = 3.0F;
    private static final float BAR_PADDING = 3.0F;
    private static final float BAR_RADIUS = 8.0F;
    private static final float BUTTON_RADIUS = 5.0F;
    private static final float TOP_MARGIN = 6.0F;

    private NavigationPage selectedPage;
    private final HudFeaturePreview hudPreview;
    private float barX;
    private float barWidth;
    private final float[] buttonX;
    private final float[] buttonY;

    /** Selects Features before constructing the original preview controller. */
    public NavigationSidebar() {
        selectedPage = NavigationPage.FEATURES;
        hudPreview = new HudFeaturePreview();
        buttonX = new float[NavigationPage.values().length];
        buttonY = new float[NavigationPage.values().length];
    }

    public NavigationPage getSelectedPage() {
        return selectedPage;
    }

    public boolean isSelected(NavigationPage page) {
        return selectedPage == page;
    }

    public float contentTop() {
        return 42.0F;
    }

    /** Draws the backdrop, bar, destination backgrounds, then their original glyphs. */
    public void render(UiRenderer renderer, float viewportWidth, int mouseX, int mouseY,
                       SidebarPalette palette, float opacity, TextureView backdrop) {
        NavigationPage[] pages = NavigationPage.values();
        int count = pages.length;
        barWidth = 6.0F + count * BUTTON_SIZE + (count - 1) * BUTTON_GAP;
        barX = (viewportWidth - barWidth) / 2.0F;
        if (backdrop != null) {
            renderer.drawBackdrop(backdrop, barX, TOP_MARGIN, barWidth, BAR_HEIGHT, BAR_RADIUS, opacity);
        }
        renderer.drawRoundedRect(barX, TOP_MARGIN, barWidth, BAR_HEIGHT, BAR_RADIUS,
                ThemePalette.multiplyAlpha(palette.background().getRGB(), opacity));
        float x = barX + BAR_PADDING;
        float y = TOP_MARGIN + BAR_PADDING;
        for (int index = 0; index < count; index++) {
            buttonX[index] = x;
            buttonY[index] = y;
            boolean selected = pages[index] == selectedPage;
            boolean hovered = mouseX >= x && mouseX <= x + BUTTON_SIZE
                    && mouseY >= y && mouseY <= y + BUTTON_SIZE;
            Color background = selected ? palette.selectedBackground()
                    : hovered ? palette.hoveredBackground() : palette.buttonBackground();
            renderer.drawRoundedRect(x, y, BUTTON_SIZE, BUTTON_SIZE, BUTTON_RADIUS,
                    ThemePalette.multiplyAlpha(background.getRGB(), opacity));
            if (FontAsset.LUCIDE != null) {
                Color icon = selected ? palette.selectedIcon() : palette.icon();
                float iconWidth = FontAsset.LUCIDE.measureText(pages[index].icon, ICON_SIZE);
                float iconX = x + (BUTTON_SIZE - iconWidth) / 2.0F;
                float iconY = y + 12.0F - 4.5F;
                renderer.drawText(FontAsset.LUCIDE, pages[index].icon, iconX, iconY, ICON_SIZE,
                        ThemePalette.multiplyAlpha(icon.getRGB(), opacity));
            }
            x += 27.0F;
        }
    }

    public void renderPage(UiRenderer renderer, int viewportWidth, int viewportHeight,
                           int mouseX, int mouseY, float opacity) {
        if (selectedPage == NavigationPage.HUD_EDITOR) {
            hudPreview.render(renderer, viewportWidth, viewportHeight, mouseX, mouseY, opacity);
        }
    }

    /** Page publication precedes reset; a reset failure leaves the new page selected. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (InputKey.MOUSE_LEFT.matches(button)) {
            NavigationPage[] pages = NavigationPage.values();
            for (int index = 0; index < pages.length; index++) {
                if (mouseX >= buttonX[index] && mouseX <= buttonX[index] + BUTTON_SIZE
                        && mouseY >= buttonY[index] && mouseY <= buttonY[index] + BUTTON_SIZE) {
                    if (pages[index] != selectedPage) {
                        selectedPage = pages[index];
                        hudPreview.reset();
                    }
                    return true;
                }
            }
        }
        if (selectedPage == NavigationPage.HUD_EDITOR && InputKey.MOUSE_LEFT.matches(button)) {
            return hudPreview.mouseClicked(mouseX, mouseY);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                int viewportWidth, int viewportHeight) {
        if (selectedPage == NavigationPage.HUD_EDITOR) {
            return hudPreview.mouseDragged(mouseX, mouseY, button, viewportWidth, viewportHeight);
        }
        return false;
    }

    public boolean mouseReleased(int button) {
        if (selectedPage == NavigationPage.HUD_EDITOR) {
            return hudPreview.mouseReleased(button);
        }
        return false;
    }
}

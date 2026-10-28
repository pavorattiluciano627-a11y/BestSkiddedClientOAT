package dev.echo.client.screens.panels;

import java.awt.Color;

/**
 * The original sidebar's six independently supplied colors.
 *
 * @param selectedBackground the selected destination's background
 * @param background the surrounding bar's background
 * @param buttonBackground the unselected, unhovered button background
 * @param hoveredBackground the hovered button background
 * @param icon the unselected icon color
 * @param selectedIcon the selected icon color
 */
public record SidebarPalette(Color selectedBackground, Color background, Color buttonBackground,
                             Color hoveredBackground, Color icon, Color selectedIcon) {
}

package dev.echo.client.screens.dashboard;

import dev.echo.client.render.SvgColorMode;

/**
 * Original dashboard assets and their color treatment.
 * Candidate: darkVariant still requires the screen-guard invariant in needs.json.
 */
public enum DashboardIcon {
    COMBAT("gui/echo/svg/Combat Icon.svg"),
    VISUALS("gui/echo/svg/Visuals Icon.svg"),
    UTILITY("gui/echo/svg/Utility Icon.svg"),
    WORLD("gui/echo/svg/World Icon.svg"),
    PLANET("gui/echo/svg/planet.svg"),
    MANAGER("gui/echo/svg/Manager Icon.svg"),
    LEGIT("gui/echo/svg/Legit Icon.svg"),
    CONFIGS("gui/echo/svg/Configs Icon.svg"),
    SCRIPTS("gui/echo/svg/Scripts Icon.svg"),
    FRIENDS("gui/echo/svg/Friends Icon.svg"),
    PROFILE("gui/echo/svg/Profile Icon.svg"),
    OVERLAYS("gui/echo/hud/layer.svg"),
    MODIFY_HUD("gui/echo/svg/Modify HUD Icon.svg"),
    PLUS("gui/echo/svg/Plus Icon.svg"),
    CHEVRON("gui/echo/svg/Chevron Icon.svg"),
    DROPDOWN_CHEVRON("gui/echo/svg/Dropdown Chevron.svg"),
    DROPDOWN_CHEVRON_UP("gui/echo/svg/Dropdown Chevron Up.svg"),
    GHOST("gui/echo/svg/Ghost Icon.svg"),
    GHOST_DARK("gui/echo/svg/Ghost Icon.svg"),
    DANGER("gui/echo/svg/Danger Icon.svg"),
    DANGER_DARK("gui/echo/svg/Danger Icon.svg"),
    FILTER("gui/echo/svg/Filter Icon.svg"),
    LOGO("gui/echo/svg/Echo Logo.svg", SvgColorMode.PRESERVE),
    SIDEBAR_GLOW("gui/echo/svg/Sidebar Glow Panel.svg", SvgColorMode.PRESERVE),
    BIND_TOGGLE("gui/echo/svg/bind_toggle.svg"),
    BIND_HOLD("gui/echo/svg/bind_hold.svg"),
    SCRIPT_CARD("gui/echo/svg/Script Card.svg"),
    PREVIEW_PREV("gui/echo/svg/arrow-short-left.svg"),
    PREVIEW_NEXT("gui/echo/svg/arrow-short-right.svg"),
    EDITOR_SCRIPT("gui/echo/svg/Editor Script.svg"),
    EDITOR_IMPORT("gui/echo/svg/Editor Import.svg"),
    EDITOR_RUN("gui/echo/svg/Editor Run.svg", SvgColorMode.PRESERVE),
    DELETE("gui/echo/svg/Delete Icon.svg"),
    MORE("gui/echo/svg/More Icon.svg"),
    PUBLIC("gui/echo/svg/Public.svg"),
    PRIVATE("gui/echo/svg/Private.svg");

    /** Unmodified resource path, including original spaces and capitalization. */
    public final String path;
    /** SVG rasterization color policy. */
    public final SvgColorMode colorMode;

    DashboardIcon(String path) {
        this(path, SvgColorMode.ALPHA_MASK);
    }

    DashboardIcon(String path, SvgColorMode colorMode) {
        this.path = path;
        this.colorMode = colorMode;
    }

    /** Chooses the original dark-state identity; both identities share SVG bytes. */
    public DashboardIcon darkVariant() {
        return switch (this) {
            case GHOST -> GHOST_DARK;
            case DANGER -> DANGER_DARK;
            default -> this;
        };
    }
}

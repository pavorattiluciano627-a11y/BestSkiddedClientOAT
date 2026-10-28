package dev.echo.client.hud;

import dev.echo.client.render.SvgColorMode;

/** Original HUD icon order, relative asset paths, and SVG color treatment. */
public enum HudIcon {
    ARROW_UP("gui/echo/hud/arrow-up.svg"),
    CAMERA("gui/echo/hud/camera.svg"),
    DANGER("gui/echo/hud/danger.svg"),
    EYE("gui/echo/hud/eye.svg"),
    EYE_SLASH("gui/echo/hud/eye-slash.svg"),
    FLASH("gui/echo/hud/flash.svg"),
    FLASH_SLASH("gui/echo/hud/flash-slash.svg"),
    GLOBAL("gui/echo/hud/global.svg"),
    HEART("gui/echo/hud/heart.svg"),
    KEYBOARD("gui/echo/hud/keyboard.svg"),
    PLANET("gui/echo/svg/planet.svg"),
    PROFILE("gui/echo/hud/profile.svg"),
    REFRESH_CIRCLE("gui/echo/hud/refresh-circle.svg"),
    SCAN("gui/echo/hud/scan.svg"),
    SHIELD_SECURITY("gui/echo/hud/shield-security.svg"),
    SHIELD_TICK("gui/echo/hud/shield-tick.svg"),
    STAR("gui/echo/hud/star.svg"),
    TAG("gui/echo/hud/tag.svg"),
    LOGO("gui/echo/svg/Echo Logo.svg", SvgColorMode.PRESERVE);

    public final String path;
    public final SvgColorMode mode;

    HudIcon(String path) {
        this(path, SvgColorMode.ALPHA_MASK);
    }

    HudIcon(String path, SvgColorMode mode) {
        this.path = path;
        this.mode = mode;
    }
}

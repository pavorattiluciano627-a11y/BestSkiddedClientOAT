package dev.echo.client.render;

import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;

/**
 * The original material-settings interface implemented by entity and hand chams.
 * Implementations return their live setting objects; this contract owns no copies,
 * cached values, callbacks, default settings or animation clock.
 */
public interface EntityMaterialSettings {
    ColorSetting getColor1Setting();

    ColorSetting getColor2Setting();

    FloatSetting getLineWidthSetting();

    FloatSetting getOutlineOpacitySetting();

    FloatSetting getFillOpacitySetting();

    FloatSetting getGlowRadiusSetting();

    FloatSetting getGlowIntensitySetting();

    FloatSetting getGradientScaleSetting();

    /** Samples the implementation's phase operation, which may advance its clock/state. */
    float getAnimationPhase();

    /** Entity chams overrides this with its live Team Colors setting; hand chams inherits false. */
    default boolean usesTeamColors() {
        return false;
    }
}

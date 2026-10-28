package dev.echo.client.bootstrap.defaults;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.features.impl.render.ViewModelFeature;

/** Constructs, assigns, collects and post-initializes View Model. */
public final class ViewModelDefaults {
    private ViewModelDefaults() {
    }

    /** Runs only after the bootstrap has allocated every feature. */
    public static void initialize(ViewModelFeature feature) {
        dev.echo.client.setting.FloatSetting preparedPosXOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosX Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.echo.client.setting.FloatSetting preparedRotXMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotX Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.FloatSetting preparedEatingBob = new dev.echo.client.setting.FloatSetting(CatalogText.literal("EatingBob"), 1.0f, 0.0f, 1.0f, 0.1f);
        dev.echo.client.setting.FloatSetting preparedRotYMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotY Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.ChoiceSetting preparedStyles = new dev.echo.client.setting.ChoiceSetting(CatalogText.literal("Styles"), CatalogText.literal("Default"), new java.lang.CharSequence[] {CatalogText.literal("Default"), CatalogText.literal("None"), CatalogText.literal("Custom"), CatalogText.literal("Smooth"), CatalogText.literal("Bounce"), CatalogText.literal("Slash"), CatalogText.literal("Punch"), CatalogText.literal("Swipe"), CatalogText.literal("Spin360"), CatalogText.literal("Scale"), CatalogText.literal("Spin"), CatalogText.literal("Katana"), CatalogText.literal("Sigma"), CatalogText.literal("Push"), CatalogText.literal("Stab"), CatalogText.literal("Slice"), CatalogText.literal("Exhibition")});
        dev.echo.client.setting.FloatSetting preparedStrengthOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Strength Off"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        dev.echo.client.setting.FloatSetting preparedStrengthMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Strength Main"), 1.0f, 0.0f, 2.0f, 0.1f, CatalogText.literal("x"));
        dev.echo.client.setting.FloatSetting preparedScaleOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Scale Off"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        dev.echo.client.setting.FloatSetting preparedRotYOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotY Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.FloatSetting preparedPosXMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosX Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.echo.client.setting.FloatSetting preparedRotZMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotZ Main"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.FloatSetting preparedScaleMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("Scale Main"), 1.0f, 0.1f, 2.0f, 0.2f, CatalogText.literal("x"));
        dev.echo.client.setting.FloatSetting preparedPosYOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosY Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.echo.client.setting.FloatSetting preparedRotZOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotZ Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.BooleanSetting preparedEatingAnimation = new dev.echo.client.setting.BooleanSetting(CatalogText.literal("Eating Animation"), true);
        dev.echo.client.setting.FloatSetting preparedPosZMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosZ Main"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.echo.client.setting.FloatSetting preparedPosZOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosZ Off"), 0.0f, -3.0f, 3.0f, 0.2f);
        dev.echo.client.setting.FloatSetting preparedRotXOff = new dev.echo.client.setting.FloatSetting(CatalogText.literal("RotX Off"), 0.0f, -180.0f, 180.0f, 1.0f, CatalogText.literal(" deg"));
        dev.echo.client.setting.FloatSetting preparedPosYMain = new dev.echo.client.setting.FloatSetting(CatalogText.literal("PosY Main"), 0.0f, -3.0f, 3.0f, 0.2f);

        ViewModelFeature.posXOff = preparedPosXOff;
        ViewModelFeature.rotXMain = preparedRotXMain;
        ViewModelFeature.eatingBob = preparedEatingBob;
        ViewModelFeature.rotYMain = preparedRotYMain;
        ViewModelFeature.styles = preparedStyles;
        ViewModelFeature.strengthOff = preparedStrengthOff;
        ViewModelFeature.strengthMain = preparedStrengthMain;
        ViewModelFeature.scaleOff = preparedScaleOff;
        ViewModelFeature.rotYOff = preparedRotYOff;
        ViewModelFeature.posXMain = preparedPosXMain;
        ViewModelFeature.rotZMain = preparedRotZMain;
        ViewModelFeature.scaleMain = preparedScaleMain;
        ViewModelFeature.posYOff = preparedPosYOff;
        ViewModelFeature.rotZOff = preparedRotZOff;
        ViewModelFeature.eatingAnimation = preparedEatingAnimation;
        ViewModelFeature.posZMain = preparedPosZMain;
        ViewModelFeature.posZOff = preparedPosZOff;
        ViewModelFeature.rotXOff = preparedRotXOff;
        ViewModelFeature.posYMain = preparedPosYMain;

        feature.settings.clear();
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.eatingAnimation);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.eatingBob);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.styles);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.strengthMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.strengthOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.scaleMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.scaleOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posXMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posYMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posZMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posXOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posYOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.posZOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotXMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotYMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotZMain);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotXOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotYOff);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, dev.echo.client.features.impl.render.ViewModelFeature.rotZOff);
        feature.postInitialize();
    }
}

package dev.echo.client.features.impl.movement;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.setting.MultiChoiceSetting;
import dev.echo.client.bootstrap.CatalogText;

/** Candidate under the conditional direct-member and guard domains. */
public class KeepSprintFeature extends Feature {
    private static final CharSequence GROUND = CatalogText.literal("Ground");
    private static final CharSequence AIR = CatalogText.literal("Air");
    public static FloatSetting speedKeptVelocity;
    private static BooleanSetting shouldSprint;
    private static MultiChoiceSetting sprintCondition;

    public KeepSprintFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Keep Sprint"), dev.echo.client.bootstrap.CatalogText.literal("Keeps you sprinting"), dev.echo.client.features.Category.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        speedKeptVelocity.tooltip("Timing");
        sprintCondition.setSelected(CatalogText.literal("Air"), true);
        sprintCondition.setSelected(CatalogText.literal("Ground"), true);
    }

    public boolean shouldKeepSprinting() {
        if (!shouldSprint.getValue()) {
            return false;
        }
        return sprintCondition.isSelected(client.player.isOnGround() ? GROUND : AIR);
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(KeepSprintFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeepSprintFeature.speedKeptVelocity);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeepSprintFeature.shouldSprint);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, KeepSprintFeature.sprintCondition);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(KeepSprintFeature feature, dev.echo.client.setting.MultiChoiceSetting preparedSprintCondition, dev.echo.client.setting.FloatSetting preparedSpeedKeptVelocity, dev.echo.client.setting.BooleanSetting preparedShouldSprint) {
        KeepSprintFeature.sprintCondition = preparedSprintCondition;
        KeepSprintFeature.speedKeptVelocity = preparedSpeedKeptVelocity;
        KeepSprintFeature.shouldSprint = preparedShouldSprint;
    }
}

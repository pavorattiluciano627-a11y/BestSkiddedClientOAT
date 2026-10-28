package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.FloatSetting;
import net.minecraft.entity.attribute.EntityAttributes;

/** Configurable player step height, preserving the original lifecycle restoration order. */
public class StepFeature extends Feature {
    private static FloatSetting height;
    private static BooleanSetting disableOnSneak;

    public StepFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Step"), dev.echo.client.bootstrap.CatalogText.literal("Changes the step height of the player"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    private void setStepHeight(float value) {
        client.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT).setBaseValue(value);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        client.player.getAttributeInstance(
                net.minecraft.entity.attribute.EntityAttributes.STEP_HEIGHT).setBaseValue(0.6f);
    }

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (client.player.isSneaking() && disableOnSneak.getValue()) {
            return;
        }
        setStepHeight(height.getValue());
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(StepFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StepFeature.height);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, StepFeature.disableOnSneak);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(StepFeature feature, dev.echo.client.setting.BooleanSetting preparedDisableOnSneak, dev.echo.client.setting.FloatSetting preparedHeight) {
        StepFeature.disableOnSneak = preparedDisableOnSneak;
        StepFeature.height = preparedHeight;
    }
}

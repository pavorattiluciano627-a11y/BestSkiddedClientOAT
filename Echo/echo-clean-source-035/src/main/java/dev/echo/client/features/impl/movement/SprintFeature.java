package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.KeyPressedQueryEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.SprintOverrideHandler;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.strings.Concat;
import net.minecraft.client.Minecraft;

/** Automatic sprint-key input, respecting the shared per-tick suppression request. */
public class SprintFeature extends Feature {
    private static final CharSequence EMPTY_SUFFIX = Concat.strings();
    private static final CharSequence OMNI_SUFFIX = Concat.strings("Omni".split(""));
    private static BooleanSetting omni;

    public SprintFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Sprint"), dev.echo.client.bootstrap.CatalogText.literal("Auto sprint"), dev.echo.client.features.Category.MOVEMENT));
    }

    public boolean isOmniEnabled() {
        return isEnabled() && omni.getValue();
    }

    @Override
    public CharSequence getSuffix() {
        return isOmniEnabled() ? OMNI_SUFFIX : EMPTY_SUFFIX;
    }

    @Override
    public void onDisable() {
        int controlState = DiveBombFeature.readSharedControlState();
        try {
            Class.forName(Minecraft.class.getName(), true, Minecraft.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        Minecraft observedClient = client;
        if (controlState == 0 && observedClient.player != null) {
            SprintOverrideHandler.resetOverrides();
            try {
                Class.forName(Minecraft.class.getName(), true, Minecraft.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Resolved class initialization failed", failure);
            }
            observedClient = client;
            observedClient.player.setSprinting(false);
        } else if (controlState != 0) {
            observedClient.player.setSprinting(false);
        }
        super.onDisable();
    }

    @Subscribe
    public void onSprintKeyQuery(KeyPressedQueryEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (SprintOverrideHandler.isSuppressSprint()) {
            event.setPressed(false);
            return;
        }
        if (EntityInteractions.hasMovementInput()) {
            event.setPressed(true);
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SprintFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SprintFeature.omni);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SprintFeature feature, dev.echo.client.setting.BooleanSetting preparedOmni) {
        SprintFeature.omni = preparedOmni;
    }
}

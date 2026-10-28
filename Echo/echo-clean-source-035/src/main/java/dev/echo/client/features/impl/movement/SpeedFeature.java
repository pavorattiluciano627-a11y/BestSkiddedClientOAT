package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.strings.Concat;
import dev.echo.client.utils.interaction.EntityInteractions;
import dev.echo.client.utils.movement.MovementVectors;

/** Five speed modes preserving the original distinct additive/set-velocity operations. */
public class SpeedFeature extends Feature {
    private static final CharSequence GRIM_ENTITY = Concat.strings("Grim Entity".split(""));
    private static final CharSequence MOTION = Concat.strings("Motion".split(""));
    private static final CharSequence STRAFE = Concat.strings("Strafe".split(""));
    private static final CharSequence MINEMEN_CLUB = Concat.strings("MinemenClub".split(""));
    private static final CharSequence LEGIT = Concat.strings("Legit".split(""));
    private static ChoiceSetting mode;
    private static FloatSetting boost;
    private static FloatSetting boatBoost;
    private static FloatSetting motion;
    private static FloatSetting strafe;

    public SpeedFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Speed"), dev.echo.client.bootstrap.CatalogText.literal("Increases movement speed"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        boost.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_ENTITY));
        boatBoost.setVisibilityPredicate(ignored -> mode.isSelected(GRIM_ENTITY));
        motion.setVisibilityPredicate(ignored -> mode.isSelected(MOTION));
        strafe.setVisibilityPredicate(ignored -> mode.isSelected(STRAFE));
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld() || !isEnabled() || !EntityInteractions.hasMovementInput()) {
            return;
        }
        if (mode.isSelected(MINEMEN_CLUB)) {
            if (client.player.onGround()) {
                MovementVectors.setHorizontalSpeed(0.26f);
                event.jump = true;
            }
        } else if (mode.isSelected(LEGIT)) {
            if (client.player.onGround()) {
                event.jump = true;
            }
        } else if (mode.isSelected(GRIM_ENTITY)) {
            if (EntityInteractions.hasNearbyBoat(client.player)) {
                MovementVectors.addHorizontalSpeed(boatBoost.getValue());
            } else if (EntityInteractions.hasNearbyEntity(client.player)) {
                MovementVectors.addHorizontalSpeed(boost.getValue());
            }
        } else if (mode.isSelected(MOTION)) {
            MovementVectors.addHorizontalSpeed(motion.getValue());
        } else if (mode.isSelected(STRAFE)) {
            MovementVectors.setHorizontalSpeed(strafe.getValue());
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(SpeedFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpeedFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpeedFeature.boost);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpeedFeature.boatBoost);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpeedFeature.motion);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, SpeedFeature.strafe);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(SpeedFeature feature, dev.echo.client.setting.FloatSetting preparedStrafe, dev.echo.client.setting.FloatSetting preparedBoatBoost, dev.echo.client.setting.FloatSetting preparedMotion, dev.echo.client.setting.FloatSetting preparedBoost, dev.echo.client.setting.ChoiceSetting preparedMode) {
        SpeedFeature.strafe = preparedStrafe;
        SpeedFeature.boatBoost = preparedBoatBoost;
        SpeedFeature.motion = preparedMotion;
        SpeedFeature.boost = preparedBoost;
        SpeedFeature.mode = preparedMode;
    }
}

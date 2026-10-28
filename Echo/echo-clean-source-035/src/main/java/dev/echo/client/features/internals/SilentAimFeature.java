package dev.echo.client.features.internals;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.movement.RotationContext;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.AimState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import net.minecraft.util.math.MathHelper;

/** Applies the configured movement correction independently of toggle state. */
public class SilentAimFeature extends Feature {
    private static final CharSequence MODE_NONE = CatalogText.literal("None");
    private static final CharSequence MODE_SILENT = CatalogText.literal("Silent");
    private static final CharSequence MODE_STRICT = CatalogText.literal("Strict");
    private static final int[][] DIRECTIONS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1},
            {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static ChoiceSetting mode;
    private static BooleanSetting renderRotationsSetting;

    public SilentAimFeature() {
        super(new FeatureInfo(CatalogText.literal("Silent Aim"),
                CatalogText.literal("Silent movement correction for rotations"), Category.INTERNALS));
    }

    /** This internal feature subscribes during post-initialization, not enabling. */
    @Override
    public void postInitialize() {
        EventBus.register(this);
    }

    /** The original internal feature intentionally ignores toggle requests. */
    @Override
    public void toggle() {
    }

    /** The original body is empty; correction is controlled by the mode setting. */
    @Override
    public void setEnabled(boolean requested) {
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isKeybindable() {
        return false;
    }

    public boolean isCorrectionEnabled() {
        return !mode.isSelected(MODE_NONE);
    }

    public boolean shouldCorrectMovement() {
        if (mode.isSelected(MODE_SILENT)) {
            return true;
        }
        if (!mode.isSelected(MODE_STRICT)) {
            return false;
        }
        Feature owner = rotations().snapshot().owner();
        return owner != null && owner.requiresMovementCorrection();
    }

    public static boolean renderRotations() {
        try {
            Class.forName(BooleanSetting.class.getName(), true, BooleanSetting.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        return renderRotationsSetting.getValue();
    }

    @Subscribe
    public void onRotationQuery(RotationEvent event) {
        if (event.getContext() != RotationContext.MOVEMENT || !isCorrectionEnabled()
                || !rotations().isSilentRotationActive()) {
            return;
        }
        event.setYaw(rotations().effectiveRotation().yaw());
        event.setPitch(rotations().effectiveRotation().pitch());
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onPlayerInput(PlayerInputEvent event) {
        if (client.player == null || !shouldCorrectMovement() || !rotations().isSilentRotationActive()) {
            return;
        }
        if (rotations().snapshot().state() == AimState.OVERRIDE) {
            return;
        }
        int forward = (int) PlayerInputEvent.movementMultiplier(event.forward, event.backward);
        int strafe = (int) PlayerInputEvent.movementMultiplier(event.left, event.right);
        if (forward == 0 && strafe == 0) {
            return;
        }
        int steps = Math.round(MathHelper.wrapDegrees(
                rotations().effectiveRotation().yaw() - client.player.getYaw()) / 45.0F);
        for (int index = 0; index < DIRECTIONS.length; index++) {
            if (DIRECTIONS[index][0] != forward || DIRECTIONS[index][1] != strafe) {
                continue;
            }
            int[] corrected = DIRECTIONS[Math.floorMod(index + steps, DIRECTIONS.length)];
            event.forward = corrected[0] > 0;
            event.backward = corrected[0] < 0;
            event.left = corrected[1] > 0;
            event.right = corrected[1] < 0;
            return;
        }
    }

    /** Reads the original shared option object at the catalog evaluation point. */
    public static CharSequence bootstrapSilentMode() {
        return MODE_SILENT;
    }

    public static CharSequence bootstrapNoneMode() {
        return MODE_NONE;
    }

    public static CharSequence bootstrapStrictMode() {
        return MODE_STRICT;
    }

    /** Publication happens only after the catalog has constructed both settings. */
    public static void assignBootstrapSettings(SilentAimFeature feature,
                                               BooleanSetting preparedRenderRotations,
                                               ChoiceSetting preparedMode) {
        renderRotationsSetting = preparedRenderRotations;
        mode = preparedMode;
    }

    /** Preserves declaring-class collection order and equals-based deduplication. */
    public static void appendBootstrapSettings(SilentAimFeature feature) {
        CatalogSettings.append(feature, mode);
        CatalogSettings.append(feature, renderRotationsSetting);
    }
}

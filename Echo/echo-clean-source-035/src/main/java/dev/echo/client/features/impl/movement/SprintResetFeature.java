package dev.echo.client.features.impl.movement;

import dev.echo.client.bootstrap.CatalogSettings;
import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.interaction.EntityAttackEvent;
import dev.echo.client.events.lifecycle.WorldTickContextEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.handlers.SprintOverrideHandler;
import dev.echo.client.input.KeyStateOverrides;
import dev.echo.client.input.KeybindingCache;
import dev.echo.client.mixin.InputAccessor;
import dev.echo.client.render.MinecraftRenderState;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.IntSetting;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/** Applies a timed W-tap, S-tap, sprint suppression or sneak after an eligible attack. */
public class SprintResetFeature extends Feature {
    private static final ChoiceSetting sprintResetMode = new ChoiceSetting(CatalogText.literal("Mode"),
        Mode.W_TAP.label, Mode.W_TAP.label, Mode.S_TAP.label, Mode.NO_STOP.label, Mode.SNEAK.label);
    private static IntSetting reactionTime;
    private static IntSetting duration;
    private static IntSetting durationBetweenReset;
    private static BooleanSetting groundOnly;
    private int lastResetTick;
    private boolean sprintSuppressionActive;

    public SprintResetFeature() {
        super(new FeatureInfo(CatalogText.literal("Sprint Reset"),
            CatalogText.literal("Assists with combo attacks"), Category.MOVEMENT));
    }

    @Override
    public void postInitialize() {
        lastResetTick = -1;
        reactionTime.setAllowOutOfRange(true);
        duration.setAllowOutOfRange(true);
        durationBetweenReset.setAllowOutOfRange(true);
        reactionTime.description(CatalogText.literal(
            "Ticks after the hit before the reset starts. NoStop and Sneak wait at least 1 tick."));
        groundOnly.setVisibilityPredicate(ignored -> selectedMode().stopsMovement());
        groundOnly.description(CatalogText.literal("Do not reset while airborne"));
        reactionTime.tooltip("Timing");
        duration.tooltip("Timing");
        durationBetweenReset.tooltip("Timing");
        sprintResetMode.onChange(this::resetSprintResetState);
    }

    private Mode selectedMode() {
        for (Mode mode : Mode.values()) {
            if (sprintResetMode.isSelected(mode.label)) {
                return mode;
            }
        }
        return Mode.W_TAP;
    }

    @Override
    public CharSequence getSuffix() {
        return selectedMode().label;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetSprintResetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetSprintResetState();
    }

    @Subscribe
    public void onWorldTickContext(WorldTickContextEvent event) {
        resetSprintResetState();
    }

    private void resetSprintResetState() {
        lastResetTick = -1;
        releaseSprintSuppression();
    }

    private void releaseSprintSuppression() {
        if (!sprintSuppressionActive) {
            return;
        }
        sprintSuppressionActive = false;
        SprintOverrideHandler.setSuppressSprint(false);
    }

    public boolean isValidTarget(LivingEntity entity) {
        return TargetSelection.isValidTarget(entity);
    }

    @Subscribe(priority = EventPriority.LOWEST)
    public void onEntityAttack(EntityAttackEvent.Pre event) {
        if (isOutsideWorld() || event.cancelled || event.getPlayer() != client.player) {
            return;
        }
        if (lastResetTick > client.player.tickCount) {
            resetSprintResetState();
        }
        if (MinecraftRenderState.getScreen() != null) {
            return;
        }
        Mode mode = selectedMode();
        // The original DCMPL treats NaN as the positive comparison result.
        boolean moving = mode == Mode.NO_STOP ? client.player.isSprinting() : !(client.player.zza <= 0.0);
        if (!moving) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!isValidTarget(target)) {
            return;
        }
        if (lastResetTick != -1 && client.player.tickCount - lastResetTick < durationBetweenReset.getValue()) {
            return;
        }
        lastResetTick = client.player.tickCount;
        applyToCurrentInput(mode);
    }

    @Subscribe
    public void onPlayerInput(PlayerInputEvent event) {
        if (isOutsideWorld()) {
            return;
        }
        if (lastResetTick > client.player.tickCount) {
            resetSprintResetState();
        }
        if (lastResetTick == -1) {
            return;
        }
        if (MinecraftRenderState.getScreen() != null || client.player.isDeadOrDying()) {
            resetSprintResetState();
            return;
        }
        applyReset(selectedMode(), event);
    }

    private void applyReset(Mode mode, PlayerInputEvent input) {
        int elapsed = client.player.tickCount - lastResetTick;
        int delay = mode.requiresOneTickDelay() ? Math.max(1, reactionTime.getValue()) : reactionTime.getValue();
        int end = delay + duration.getValue();
        if (elapsed >= end) {
            restoreInput(mode, input);
            lastResetTick = -1;
            releaseSprintSuppression();
            return;
        }
        if (elapsed < delay) {
            return;
        }
        boolean skipAirborne = mode.stopsMovement() && groundOnly.getValue() && !client.player.onGround();
        switch (mode) {
            case W_TAP -> {
                input.forward = skipAirborne && isForwardPressed();
                if (!skipAirborne) {
                    KeyStateOverrides.setPressed(client.options.keyUp, false);
                }
            }
            case S_TAP -> {
                if (skipAirborne) {
                    input.forward = isForwardPressed();
                } else {
                    input.forward = false;
                    input.backward = true;
                    KeyStateOverrides.setPressed(client.options.keyUp, false);
                    KeyStateOverrides.setPressed(client.options.keyDown, true);
                }
            }
            case NO_STOP -> {
                sprintSuppressionActive = true;
                SprintOverrideHandler.setSuppressSprint(true);
            }
            case SNEAK -> {
                input.sneak = true;
                KeyStateOverrides.setPressed(client.options.keyShift, true);
            }
        }
    }

    private void restoreInput(Mode mode, PlayerInputEvent input) {
        switch (mode) {
            case W_TAP -> input.forward = isForwardPressed();
            case S_TAP -> {
                input.forward = isForwardPressed();
                input.backward = isBackwardPressed();
            }
            case SNEAK -> input.sneak = isSneakPressed();
        }
    }

    private void applyToCurrentInput(Mode mode) {
        if (client.player.input == null) {
            return;
        }
        InputAccessor accessor = (InputAccessor) client.player.input;
        Input keys = accessor.getPlayerInput();
        PlayerInputEvent input = new PlayerInputEvent(keys.forward(), keys.backward(), keys.left(), keys.right(),
            keys.jump(), keys.shift(), keys.sprint());
        applyReset(mode, input);
        accessor.setPlayerInput(new Input(input.forward, input.backward, input.left, input.right,
            input.jump, input.sneak, input.sprint));
        accessor.setMovementVector(new Vec2(PlayerInputEvent.movementMultiplier(input.left, input.right),
            PlayerInputEvent.movementMultiplier(input.forward, input.backward)));
    }

    private boolean isForwardPressed() {
        return KeybindingCache.isPressed(client.options.keyUp);
    }

    private boolean isBackwardPressed() {
        return KeybindingCache.isPressed(client.options.keyDown);
    }

    private boolean isSneakPressed() {
        return KeybindingCache.isPressed(client.options.keyShift);
    }

    public static void appendBootstrapSettings(SprintResetFeature feature) {
        CatalogSettings.append(feature, sprintResetMode);
        CatalogSettings.append(feature, reactionTime);
        CatalogSettings.append(feature, duration);
        CatalogSettings.append(feature, durationBetweenReset);
        CatalogSettings.append(feature, groundOnly);
    }

    public static void assignBootstrapSettings(SprintResetFeature feature, IntSetting preparedDurationBetweenReset,
            IntSetting preparedDuration, BooleanSetting preparedGroundOnly, IntSetting preparedReactionTime) {
        durationBetweenReset = preparedDurationBetweenReset;
        duration = preparedDuration;
        groundOnly = preparedGroundOnly;
        reactionTime = preparedReactionTime;
    }

    /** Original SignalProvider enum order, display identities and mode predicates. */
    enum Mode {
        W_TAP(CatalogText.literal("W-Tap")),
        S_TAP(CatalogText.literal("S-Tap")),
        NO_STOP(CatalogText.literal("NoStop")),
        SNEAK(CatalogText.literal("Sneak"));

        final CharSequence label;

        Mode(CharSequence label) {
            this.label = label;
        }

        boolean requiresOneTickDelay() {
            return this == NO_STOP || this == SNEAK;
        }

        boolean stopsMovement() {
            return this == W_TAP || this == S_TAP;
        }
    }
}

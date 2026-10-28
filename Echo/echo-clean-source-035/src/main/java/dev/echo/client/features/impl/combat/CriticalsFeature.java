package dev.echo.client.features.impl.combat;

import dev.echo.client.bootstrap.CatalogText;
import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.events.interaction.AttackAttemptEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.handlers.SprintOverrideHandler;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.FloatRangeSetting;
import dev.echo.client.utils.simulation.PlayerPrediction;
import dev.echo.client.utils.simulation.SimulatedPlayer;
import dev.echo.client.utils.targeting.EntityRaycasts;
import dev.echo.client.utils.targeting.TargetSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

/** Release/Unsprint timing and the original two-packet Vanilla critical mode. */
public class CriticalsFeature extends Feature {
    private static final CharSequence RELEASE = CatalogText.literal("Release");
    private static final CharSequence DELAY = CatalogText.literal("Delay");
    private static final CharSequence UNSPRINT = CatalogText.literal("Unsprint");
    private static final CharSequence GROUND = CatalogText.literal("Ground");
    private static final CharSequence VANILLA = CatalogText.literal("Vanilla");
    private static ChoiceSetting mode;
    private static ChoiceSetting resume;
    private static FloatRangeSetting resumeDelay;
    private boolean sprintSuppressed;
    private int resumeTicks;

    public CriticalsFeature() {
        super(new dev.echo.client.features.FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Criticals"), dev.echo.client.bootstrap.CatalogText.literal("Ensures critical hits"), dev.echo.client.features.Category.COMBAT));
    }

    @Override
    public void postInitialize() {
        sprintSuppressed = false;
        resumeTicks = -1;
        mode.description(CatalogText.literal("Releases forward key to land criticals"));
        resume.setVisibilityPredicate(ignored -> mode.isSelected(RELEASE));
        resume.description(CatalogText.literal("When forward returns after the hit"));
        resumeDelay.setVisibilityPredicate(ignored -> {
            String[] controlState = SpearReachFeature.readSharedControlState();
            ChoiceSetting condition = mode;
            CharSequence option = RELEASE;
            if (controlState == null) {
                if (!condition.isSelected(option)) return false;
                condition = resume;
                option = DELAY;
            }
            return condition.isSelected(option);
        });
        resumeDelay.tooltip("Timing");
    }

    @Override
    public void onDisable() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) {
            if (sprintSuppressed) {
                SprintOverrideHandler.setSuppressSprint(false);
                sprintSuppressed = false;
            }
            resumeTicks = -1;
        }
        super.onDisable();
    }

    @Override
    public CharSequence getSuffix() { return mode.getValue(); }

    private static boolean isCurrentAttackReady() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        float difference = client.player.getAttackCooldownProgress(0.5f) - 0.9f;
        return controlState != null ? difference != 0.0f : difference > 0.0f;
    }

    private static boolean isNextAttackReady() {
        String[] controlState = SpearReachFeature.readSharedControlState();
        try {
            Class.forName(MinecraftClient.class.getName(), true, MinecraftClient.class.getClassLoader());
        } catch (ClassNotFoundException failure) {
            throw new IllegalStateException("Resolved class initialization failed", failure);
        }
        float difference = client.player.getAttackCooldownProgress(1.5f) - 0.9f;
        return controlState != null ? difference != 0.0f : difference > 0.0f;
    }

    private static boolean willRemainAirborneWhileFalling(PlayerInputEvent input) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean onGround = client.player.isOnGround();
        if (controlState != null) return onGround;
        if (onGround) return false;
        if (client.player.isGliding()) return false;
        if (client.player.isTouchingWater()) return false;
        // Allocation is deliberately outside the original handled-failure region.
        SimulatedPlayer simulation = PlayerPrediction.forWorld(client.world);
        try {
            simulation.copyFrom(client.player, client.player.getVelocity(), client.player.getYaw(),
                    client.player.getPitch(),
                    PlayerInputEvent.movementMultiplier(input.forward, input.backward) * 0.98f,
                    PlayerInputEvent.movementMultiplier(input.left, input.right) * 0.98f, false);
            double initialY = simulation.getY();
            simulation.tickPrediction();
            if (simulation.isOnGround()) return false;
            if (simulation.isTouchingWater()) return false;
            if (simulation.isClimbing()) return false;
            if (simulation.getY() >= initialY) return false;
            simulation.tickPrediction();
            return !simulation.isOnGround();
        } catch (Throwable predictionFailure) {
            return false;
        }
    }

    private static boolean hasTargetInReach() {
        Vec3d eyePosition = client.player.getEyePos();
        String[] controlState = SpearReachFeature.readSharedControlState();
        double range = client.player.getEntityInteractionRange();
        for (Entity entity : client.world.getEntities()) {
            if (controlState == null) {
                if (entity == client.player) continue;
                if (!(entity instanceof LivingEntity)) continue;
            }
            LivingEntity target = (LivingEntity) entity;
            if (controlState == null && !TargetSelection.isValidTarget(target)) continue;
            double difference = target.getBoundingBox().squaredMagnitude(eyePosition) - range * range;
            if (controlState != null) return difference != 0.0;
            if (difference <= 0.0) return true;
        }
        return false;
    }

    @Subscribe
    private void onClientTick(ClientTickEvent event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        if (controlState == null) {
            if (isOutsideWorld()) return;
            if (resumeTicks > 0) --resumeTicks;
            if (mode.isSelected(UNSPRINT)) return;
            if (!sprintSuppressed) return;
        } else if (resumeTicks == 0) {
            return;
        }
        SprintOverrideHandler.setSuppressSprint(false);
        sprintSuppressed = false;
    }

    private void updateSprintSuppression(PlayerInputEvent input) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean shouldSuppress = isNextAttackReady();
        if (controlState == null) {
            shouldSuppress = shouldSuppress && hasTargetInReach() && willRemainAirborneWhileFalling(input);
            if (shouldSuppress) {
                SprintOverrideHandler.setSuppressSprint(true);
                sprintSuppressed = true;
                return;
            }
            if (!sprintSuppressed) return;
        } else if (!shouldSuppress) {
            return;
        }
        SprintOverrideHandler.setSuppressSprint(false);
        sprintSuppressed = false;
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void onPlayerInput(PlayerInputEvent input) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean outsideWorld = isOutsideWorld();
        if (controlState != null) {
            if (outsideWorld) {
                updateSprintSuppression(input);
                return;
            }
            if (mode.isSelected(RELEASE)) {
                resumeTicks = -1;
                return;
            }
            if (client.player.isTouchingWater()) input.forward = false;
            return;
        }
        if (outsideWorld) return;
        if (mode.isSelected(UNSPRINT)) {
            updateSprintSuppression(input);
            return;
        }
        if (!mode.isSelected(RELEASE)) return;
        if (client.player.isOnGround() || client.player.getVelocity().y > 0.0
                || client.player.isGliding() || client.player.isTouchingWater()) {
            resumeTicks = -1;
            return;
        }
        if (resumeTicks == 0) return;
        if (!isNextAttackReady()) return;
        if (!hasTargetInReach()) return;
        if (!willRemainAirborneWhileFalling(input)) return;
        input.forward = false;
    }

    @Subscribe
    public void onAttackAttempt(AttackAttemptEvent event) {
        String[] controlState = SpearReachFeature.readSharedControlState();
        boolean outsideWorld = isOutsideWorld();
        boolean sendPackets;
        if (controlState != null) {
            sendPackets = isCurrentAttackReady();
        } else {
            if (outsideWorld) return;
            if (!(event.getTarget() instanceof LivingEntity)) return;
            boolean critical = EntityRaycasts.isCriticalState();
            if (isCurrentAttackReady() && critical && !resume.isSelected(GROUND)) {
                resumeTicks = resume.isSelected(DELAY) ? Math.max(0, Math.round(resumeDelay.sampleGaussian())) : 0;
            }
            if (critical) return;
            sendPackets = mode.isSelected(VANILLA);
        }
        if (sendPackets) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 0.2, client.player.getZ(), false, false));
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 0.1, client.player.getZ(), false, false));
        }
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(CriticalsFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, CriticalsFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, CriticalsFeature.resume);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, CriticalsFeature.resumeDelay);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(CriticalsFeature feature, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.ChoiceSetting preparedResume, dev.echo.client.setting.FloatRangeSetting preparedResumeDelay) {
        CriticalsFeature.mode = preparedMode;
        CriticalsFeature.resume = preparedResume;
        CriticalsFeature.resumeDelay = preparedResumeDelay;
    }
}

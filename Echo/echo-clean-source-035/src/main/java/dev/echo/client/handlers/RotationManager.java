package dev.echo.client.handlers;

import dev.echo.client.events.EventPriority;
import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.MouseMovementEvent;
import dev.echo.client.events.lifecycle.ClientTickEvent;
import dev.echo.client.events.movement.MovementPacketEvent;
import dev.echo.client.events.movement.RotationEvent;
import dev.echo.client.features.Feature;
import dev.echo.client.features.internals.SilentAimFeature;
import dev.echo.client.utils.Rotation;
import dev.echo.client.utils.targeting.AimPointSettings;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Request arbitration, visible/silent rotation application and temporary overrides.
 * Original key/cache and opaque-control applicability remains recorded in the
 * provider receipt; all meaningful state and callbacks are ordinary source here.
 */
public final class RotationManager extends ClientHandler {
    private static final float VISUAL_SMOOTHING = 0.55F;
    private static final float RETURN_TOLERANCE = 1.0F;
    private static final int OVERRIDE_VISUAL_TICKS = 3;
    private final MouseSensitivity sensitivity = new MouseSensitivity();
    private Rotation currentRotation = new Rotation(0.0F, 0.0F);
    private Rotation previousRotation = currentRotation;
    private RotationRequest activeRequest;
    private RotationScope activeScope;
    private Rotation savedRotation;
    private boolean savedSilentActive;
    private boolean scopeUsed;
    private boolean silentActive;
    private int updateTick;
    private int pendingMouseX;
    private int pendingMouseY;
    private float mouseRemainderX;
    private float mouseRemainderY;
    private float serverYaw;
    private float serverPitch;
    private float serverYawDelta;
    private float consumedYawDelta;
    private boolean serverRotationChanged;
    private boolean visualActive;
    private float visualYaw;
    private float visualPitch;
    private float previousVisualPitch;
    private Rotation heldVisualRotation;
    private int visualHoldTicks;
    private boolean snapVisuals;
    private boolean hasServerRotation;
    private Object trackedPlayer;
    private Object trackedWorld;

    /** Acquires or reuses a request, preempting only strictly lower priorities. */
    public Optional<RotationRequest> request(Feature owner, RotationPriority priority) {
        if (!hasPlayerAndWorld() || activeScope != null) return Optional.empty();
        synchronizeContext();
        if (activeRequest != null && activeRequest.isActive()) {
            if (activeRequest.owner() == owner) return Optional.of(activeRequest);
            if (!priority.higherThan(activeRequest.priority())) return Optional.empty();
            activeRequest.invalidate();
            resetMouseInjection();
        }
        activeRequest = new RotationRequest(this, owner, priority);
        return Optional.of(activeRequest);
    }

    /** Returns the player/current/previous rotations and current ownership state. */
    public RotationState snapshot() {
        Rotation player = playerRotation();
        RotationRequest request = activeRequest != null && activeRequest.isActive() ? activeRequest : null;
        AimState state = activeScope != null ? AimState.OVERRIDE
                : request == null ? AimState.IDLE : request.returning() ? AimState.RETURNING : AimState.AIMING;
        return new RotationState(player, silentActive ? currentRotation : player,
                silentActive ? previousRotation : player, silentActive,
                request == null ? null : request.owner(), request == null ? null : request.target(), state);
    }

    /** Tests the original server-rotation freshness predicate used by consumers. */
    public boolean isRotationReady() {
        return !serverRotationChanged || serverYawDelta <= 2.0F
                || Math.abs(serverYawDelta - consumedYawDelta) >= 1.0E-4F;
    }

    /** Consumes the current tracked yaw change without altering the server angles. */
    public void acknowledgeRotation() {
        if (serverRotationChanged) consumedYawDelta = serverYawDelta;
        serverRotationChanged = false;
    }

    /** Returns the last packet rotation, or null before a rotation has been observed. */
    public Rotation serverRotation() {
        return hasServerRotation ? new Rotation(serverYaw, serverPitch) : null;
    }

    /** Aims an owner's request at a world-space point. */
    public boolean aim(Feature owner, RotationPriority priority, Vec3 point, RotationSettings settings) {
        Optional<RotationRequest> acquired = request(owner, priority);
        return acquired.isPresent() && acquired.get().aim(point, settings);
    }

    /** Aims an owner's request at a selected visible point on an entity. */
    public boolean aim(Feature owner, RotationPriority priority, LivingEntity target,
                       AimPointSettings targeting, RotationSettings settings) {
        Optional<RotationRequest> acquired = request(owner, priority);
        return acquired.isPresent() && acquired.get().aim(target, targeting, settings);
    }

    public boolean isSilentRotationActive() { return silentActive; }
    public Rotation effectiveRotation() { return silentActive ? currentRotation : playerRotation(); }

    /** Releases ownership and reports whether a silent return is still in progress. */
    public boolean beginReturn(Feature owner) {
        if (!isOwnedBy(owner)) return false;
        release(owner);
        return isOwnedBy(owner);
    }

    public boolean isOwnedBy(Feature owner) {
        return activeRequest != null && activeRequest.isActive() && activeRequest.owner() == owner;
    }

    public boolean canRequest(Feature owner, RotationPriority priority) {
        if (activeScope != null) return false;
        return activeRequest == null || !activeRequest.isActive() || activeRequest.owner() == owner
                || priority.higherThan(activeRequest.priority());
    }

    public boolean isAiming() {
        return activeRequest != null && activeRequest.isActive() && !activeRequest.returning();
    }

    public void release(Feature owner) {
        if (isOwnedBy(owner)) release(activeRequest);
    }

    public void cancel(Feature owner) {
        if (isOwnedBy(owner)) clearRequest();
    }

    /** Creates a scoped override without discarding an underlying lower-priority request. */
    public Optional<RotationScope> override(Feature owner, RotationPriority priority, Rotation rotation) {
        if (!hasPlayerAndWorld()) return Optional.empty();
        synchronizeContext();
        if (activeScope != null || (activeRequest != null && activeRequest.isActive()
                && !priority.higherThan(activeRequest.priority()))) return Optional.empty();
        savedRotation = currentRotation;
        savedSilentActive = silentActive;
        activeScope = new RotationScope(this);
        prepareSilentRotation();
        applyQuantized(rotation.relativeTo(currentRotation));
        return Optional.of(activeScope);
    }

    /** Invalidates request/scope ownership and clears all transient rotation state. */
    public void reset() {
        if (activeRequest != null) activeRequest.invalidate();
        activeRequest = null;
        if (activeScope != null) activeScope.invalidate();
        activeScope = null;
        silentActive = false;
        savedRotation = null;
        savedSilentActive = false;
        scopeUsed = false;
        sensitivity.reset();
        visualActive = false;
        visualHoldTicks = 0;
        resetMouseInjection();
        resetServerTracking(playerRotation());
    }

    boolean isCurrent(RotationRequest request) { return activeRequest == request && request != null; }

    boolean aimAt(RotationRequest request, Vec3 point, RotationSettings settings) {
        if (!canUpdate(request)) return false;
        updateTarget(request, point, null, settings);
        return true;
    }

    boolean aimAt(RotationRequest request, LivingEntity entity, AimPointSettings targeting, RotationSettings settings) {
        if (!canUpdate(request)) return false;
        Vec3 point = request.targetPoint(entity, targeting, targetTickDelta(settings)).orElse(null);
        if (point == null) return false;
        updateTarget(request, point, entity, settings);
        return true;
    }

    private boolean canUpdate(RotationRequest request) {
        if (!hasPlayerAndWorld()) return false;
        synchronizeContext();
        return request == activeRequest && request.isActive();
    }

    private void updateTarget(RotationRequest request, Vec3 point, LivingEntity entity, RotationSettings settings) {
        if (settings.visibility() == RotationVisibility.VISIBLE && silentActive) stopSilentRotation();
        Rotation current = settings.visibility() == RotationVisibility.SILENT && silentActive ? currentRotation : playerRotation();
        if (request.settings() != null && request.settings().visibility() != settings.visibility()) resetMouseInjection();
        request.settings(settings);
        request.target(Rotation.lookingAt(CLIENT.player.getEyePosition(targetTickDelta(settings)), point).relativeTo(current), entity);
        request.refreshedTick(updateTick);
        request.returning(false);
    }

    void refresh(RotationRequest request) {
        if (request == activeRequest && request.isActive()) request.refreshedTick(updateTick);
    }

    void release(RotationRequest request) {
        if (request != activeRequest || !request.isActive()) return;
        if (request.settings() == null || request.settings().visibility() == RotationVisibility.VISIBLE) {
            clearRequest();
            return;
        }
        request.returning(true);
        request.refreshedTick(updateTick);
    }

    void markScopeUsed(RotationScope scope) {
        if (scope == activeScope) scopeUsed = true;
    }

    void closeScope(RotationScope scope) {
        if (scope == activeScope) finishScope(false);
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onMouseMovement(MouseMovementEvent.Pre event) {
        if (activeRequest == null || !activeRequest.isActive() || activeRequest.returning()
                || activeRequest.settings() == null || activeRequest.settings().visibility() != RotationVisibility.VISIBLE) return;
        if (activeRequest.settings().transition() == AimTransition.SNAP) {
            event.deltaX = pendingMouseX;
            event.deltaY = pendingMouseY;
        } else {
            event.deltaX += pendingMouseX;
            event.deltaY += pendingMouseY;
        }
        pendingMouseX = 0;
        pendingMouseY = 0;
    }

    @Subscribe(priority = EventPriority.LOWEST)
    private void afterMouseMovement(MouseMovementEvent.Post event) {
        if (!hasPlayerAndWorld()) { reset(); return; }
        synchronizeContext();
        updateTick++;
        if (activeScope != null || activeRequest == null || !activeRequest.isActive()) return;
        if (!activeRequest.returning() && activeRequest.refreshedTick() < updateTick - 1) release(activeRequest);
        updateRotation();
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void beforeMovementPacket(MovementPacketEvent.Pre event) {
        if (silentActive) {
            event.setYaw(currentRotation.yaw());
            event.setPitch(currentRotation.pitch());
        }
        recordServerRotation(new Rotation((float) event.getYaw(), (float) event.getPitch()));
    }

    @Subscribe
    private void afterMovementPacket(MovementPacketEvent.Post event) {
        if (scopeUsed && activeScope != null) finishScope(true);
    }

    @Subscribe
    private void onRotation(RotationEvent event) {
        switch (event.getContext()) {
            case MOVEMENT -> { return; }
            case HEAD, BODY -> {
                if (visualActive && SilentAimFeature.renderRotations()) {
                    event.setYaw(visualYaw);
                    event.setPitch(visualPitch);
                }
            }
            case RENDER_PITCH -> {
                if (visualActive && SilentAimFeature.renderRotations()) {
                    event.setPitch(Mth.clamp(Mth.lerp(event.getTickDelta(), previousVisualPitch, visualPitch), -90.0F, 90.0F));
                }
            }
            default -> {
                if (silentActive) {
                    event.setYaw(currentRotation.yaw());
                    event.setPitch(currentRotation.pitch());
                }
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    private void onTick(ClientTickEvent event) {
        if (!hasPlayerAndWorld()) { visualActive = false; return; }
        boolean holding = visualHoldTicks > 0;
        Rotation target = holding ? heldVisualRotation : effectiveRotation();
        if (holding) visualHoldTicks--;
        if (activeRequest != null && activeRequest.settings() != null) snapVisuals = activeRequest.settings().transition() == AimTransition.SNAP;
        if (!visualActive) {
            if (!silentActive && !holding) return;
            visualYaw = playerRotation().yaw();
            visualPitch = playerRotation().pitch();
            previousVisualPitch = visualPitch;
            visualActive = true;
        }
        float yawDelta = Mth.wrapDegrees(target.yaw() - visualYaw);
        float pitchDelta = target.pitch() - visualPitch;
        previousVisualPitch = visualPitch;
        float blend = snapVisuals ? 1.0F : VISUAL_SMOOTHING;
        visualYaw += yawDelta * blend;
        visualPitch += pitchDelta * blend;
        if (!silentActive && !holding && Math.abs(yawDelta) < RETURN_TOLERANCE && Math.abs(pitchDelta) < RETURN_TOLERANCE) visualActive = false;
    }

    private void updateRotation() {
        RotationSettings settings = activeRequest.settings();
        Rotation current = settings != null && settings.visibility() == RotationVisibility.SILENT && silentActive ? currentRotation : playerRotation();
        Rotation desired = activeRequest.returning() ? playerRotation().relativeTo(current) : activeRequest.desiredRotation();
        if (settings == null || desired == null) return;
        float elapsed = activeRequest.elapsedSeconds(System.nanoTime());
        Rotation stepped = activeRequest.transition(current, desired, settings, elapsed);
        if (settings.visibility() == RotationVisibility.SILENT || activeRequest.returning()) {
            applyQuantized(stepped);
        } else {
            boolean fineScale = CLIENT.options.getCameraType().isFirstPerson() && CLIENT.player.isScoping();
            float step = MouseSensitivity.angularStep(fineScale);
            if (step > 1.0E-5F) {
                boolean invertX = CLIENT.options.invertMouseX().get();
                boolean invertY = CLIENT.options.invertMouseY().get();
                float pendingYaw = pendingMouseX * (invertX ? -step : step);
                float pendingPitch = pendingMouseY * (invertY ? -step : step);
                float mouseX = (Mth.wrapDegrees(stepped.yaw() - current.yaw()) - pendingYaw) / step + mouseRemainderX;
                float mouseY = (stepped.pitch() - current.pitch() - pendingPitch) / step + mouseRemainderY;
                if (Math.abs(Mth.wrapDegrees(desired.yaw() - current.yaw())) < step * 0.5F) mouseX = 0.0F;
                // The original second tolerance comparison reads current yaw.
                if (Math.abs(desired.pitch() - current.yaw()) < step * 0.5F) mouseY = 0.0F;
                int wholeX = Math.round(mouseX);
                int wholeY = Math.round(mouseY);
                mouseRemainderX = mouseX - wholeX;
                mouseRemainderY = mouseY - wholeY;
                pendingMouseX += invertX ? -wholeX : wholeX;
                pendingMouseY += invertY ? -wholeY : wholeY;
            }
        }
        if (activeRequest.returning() && Math.abs(Mth.wrapDegrees(desired.yaw() - effectiveRotation().yaw())) < RETURN_TOLERANCE
                && Math.abs(desired.pitch() - effectiveRotation().pitch()) < RETURN_TOLERANCE) clearRequest();
    }

    private void applyQuantized(Rotation requested) {
        prepareSilentRotation();
        float step = MouseSensitivity.angularStep(false);
        MouseDelta counts = sensitivity.quantize(Mth.wrapDegrees(requested.yaw() - currentRotation.yaw()),
                requested.pitch() - currentRotation.pitch(), false, false, false);
        if (counts.horizontalCounts() == 0 && counts.verticalCounts() == 0) return;
        updateCurrent(new Rotation(currentRotation.yaw() + counts.horizontalCounts() * step,
                currentRotation.pitch() + counts.verticalCounts() * step));
        silentActive = true;
    }

    private void clearRequest() {
        if (activeRequest != null) activeRequest.invalidate();
        activeRequest = null;
        resetMouseInjection();
        if (silentActive) stopSilentRotation();
    }

    private void finishScope(boolean retainVisuals) {
        if (activeScope == null) return;
        activeScope.invalidate();
        activeScope = null;
        scopeUsed = false;
        if (retainVisuals) {
            heldVisualRotation = currentRotation;
            visualHoldTicks = OVERRIDE_VISUAL_TICKS;
        }
        if (savedSilentActive && savedRotation != null) {
            updateCurrent(savedRotation);
            silentActive = true;
        } else {
            stopSilentRotation();
        }
        if (activeRequest != null) activeRequest.resetClock();
        savedRotation = null;
        savedSilentActive = false;
    }

    private void stopSilentRotation() {
        if (!silentActive || CLIENT.player == null) { silentActive = false; return; }
        float playerYaw = CLIENT.player.getYRot();
        float nearestYaw = currentRotation.yaw() + Mth.wrapDegrees(playerYaw - currentRotation.yaw());
        float offset = nearestYaw - playerYaw;
        if (offset != 0.0F) {
            CLIENT.player.yRotO += offset;
            CLIENT.player.yBob += offset;
            CLIENT.player.yBobO += offset;
            CLIENT.player.yBodyRot += offset;
            CLIENT.player.yBodyRotO += offset;
            CLIENT.player.yHeadRot += offset;
            CLIENT.player.yHeadRotO += offset;
            CLIENT.player.setYRot(nearestYaw);
        }
        updateCurrent(new Rotation(nearestYaw, CLIENT.player.getXRot()));
        silentActive = false;
    }

    private void prepareSilentRotation() {
        if (silentActive || CLIENT.player == null) return;
        updateCurrent(playerRotation());
        sensitivity.reset();
    }

    private void resetMouseInjection() {
        pendingMouseX = 0;
        pendingMouseY = 0;
        mouseRemainderX = 0.0F;
        mouseRemainderY = 0.0F;
    }

    private void resetServerTracking(Rotation rotation) {
        serverYaw = rotation.yaw();
        serverPitch = rotation.pitch();
        consumedYawDelta = 0.0F;
        serverRotationChanged = false;
        hasServerRotation = false;
    }

    private void recordServerRotation(Rotation rotation) {
        if (rotation.yaw() != serverYaw || rotation.pitch() != serverPitch) {
            serverYawDelta = Math.abs(rotation.yaw() - serverYaw);
            serverRotationChanged = true;
        }
        serverYaw = rotation.yaw();
        serverPitch = rotation.pitch();
        hasServerRotation = true;
    }

    private void updateCurrent(Rotation rotation) {
        previousRotation = currentRotation;
        currentRotation = new Rotation(rotation.yaw(), PitchLimits.clampDegrees(rotation.pitch()));
    }

    private Rotation playerRotation() {
        return CLIENT.player == null ? new Rotation(0.0F, 0.0F) : new Rotation(CLIENT.player.getYRot(), CLIENT.player.getXRot());
    }

    private boolean hasPlayerAndWorld() { return CLIENT.player != null && CLIENT.level != null; }

    private float targetTickDelta(RotationSettings settings) {
        return settings.visibility() == RotationVisibility.SILENT ? 1.0F : CLIENT.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    private void synchronizeContext() {
        if (trackedPlayer != CLIENT.player || trackedWorld != CLIENT.level) {
            trackedPlayer = CLIENT.player;
            trackedWorld = CLIENT.level;
            reset();
        }
        trackedPlayer = CLIENT.player;
        trackedWorld = CLIENT.level;
    }
}

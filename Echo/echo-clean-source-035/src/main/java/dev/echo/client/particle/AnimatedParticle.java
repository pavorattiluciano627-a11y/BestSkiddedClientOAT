package dev.echo.client.particle;

import dev.echo.client.events.Event;
import dev.echo.client.features.impl.render.ParticlesFeature;
import dev.echo.client.utils.RandomMath;

/** Ambient ember animation with the original shared random stream and wall-time integration. */
public class AnimatedParticle extends Particle {
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private final float lifetimeSeconds;
    private final float baseSize;
    private final float riseSpeed;
    private final double driftX;
    private final double driftZ;
    private final float xWobbleFrequency;
    private final float xWobblePhase;
    private final float zWobbleFrequency;
    private final float zWobblePhase;
    private final float pulseFrequency;
    private final float pulsePhase;
    private final float colorVariation;
    private long lastUpdateNanos;
    private float elapsedSeconds;
    private static String[] animationMarker;

    static {
        if (getAnimationMarker() != null) {
            setAnimationMarker(new String[3]);
        }
    }

    public AnimatedParticle(double x, double y, double z) {
        super(0);
        this.x = x;
        getAnimationMarker();
        this.y = y;
        this.z = z;
        lifetimeSeconds = (float) RandomMath.randomFloat(4.0F, 10.0F);
        baseSize = (float) RandomMath.randomFloat(0.045F, 0.12F);
        riseSpeed = (float) RandomMath.randomFloat(0.1F, 0.35F);
        double angle = RandomMath.randomDouble(0.0, Math.PI * 2.0);
        double horizontalSpeed = RandomMath.randomDouble(0.02, 0.12);
        driftX = Math.cos(angle) * horizontalSpeed;
        driftZ = Math.sin(angle) * horizontalSpeed;
        xWobbleFrequency = (float) RandomMath.randomFloat(0.6F, 2.0F);
        xWobblePhase = (float) RandomMath.randomFloat(0.0F, TWO_PI);
        zWobbleFrequency = (float) RandomMath.randomFloat(0.6F, 2.0F);
        zWobblePhase = (float) RandomMath.randomFloat(0.0F, TWO_PI);
        pulseFrequency = (float) RandomMath.randomFloat(3.0F, 8.0F);
        pulsePhase = (float) RandomMath.randomFloat(0.0F, TWO_PI);
        colorVariation = (float) RandomMath.randomFloat(0.0F, 1.0F);
        if (Event.getEventCounterFallbackValue() == 0) {
            setAnimationMarker(new String[4]);
        }
    }

    @Override
    public boolean isDead() {
        String[] marker = getAnimationMarker();
        boolean active = ParticlesFeature.isActive();
        if (marker != null) {
            return active;
        }
        if (!active) {
            return true;
        }
        return elapsedSeconds >= lifetimeSeconds;
    }

    @Override
    public float getSize() {
        return baseSize * ParticlesFeature.getSizeMultiplier();
    }

    @Override
    public void tick(float tickProgress) {
        String[] marker = getAnimationMarker();
        float deltaSeconds = RandomMath.frameSeconds(lastUpdateNanos);
        lastUpdateNanos = RandomMath.nanoTime();
        elapsedSeconds += deltaSeconds;
        float speedMultiplier = ParticlesFeature.getSpeedMultiplier();
        x += (driftX + Math.cos(elapsedSeconds * xWobbleFrequency + xWobblePhase) * 0.1)
                * speedMultiplier * deltaSeconds;
        z += (driftZ + Math.sin(elapsedSeconds * zWobbleFrequency + zWobblePhase) * 0.1)
                * speedMultiplier * deltaSeconds;
        y += (riseSpeed + Math.sin(elapsedSeconds * zWobbleFrequency * 0.7F + xWobblePhase) * 0.04)
                * speedMultiplier * deltaSeconds;
        float fadeIn = Math.min(1.0F, elapsedSeconds / 0.75F);
        float fadeOut = RandomMath.clamp((lifetimeSeconds - elapsedSeconds) / 1.5F, 0.0F, 1.0F);
        float pulse = ParticlesFeature.isPulsing()
                ? 0.62F + 0.38F * (float) Math.sin(elapsedSeconds * pulseFrequency + pulsePhase)
                : 1.0F;
        int alpha = Math.round(ParticlesFeature.getAlpha() * fadeIn * fadeOut * pulse);
        int red = clampChannel(ParticlesFeature.getRed() + Math.round(colorVariation * 60.0F));
        int green = clampChannel(ParticlesFeature.getGreen() + Math.round(colorVariation * 45.0F));
        int blue = clampChannel(ParticlesFeature.getBlue() + Math.round(colorVariation * 20.0F));
        color = clampChannel(alpha) << 24 | red << 16 | green << 8 | blue;
        if (marker != null) {
            Event.setEventCounter(Event.getEventCounter() + 1);
        }
    }

    private static int clampChannel(int channel) {
        return RandomMath.clamp(channel, 0, 255);
    }

    public static void setAnimationMarker(String[] marker) {
        animationMarker = marker;
    }

    public static String[] getAnimationMarker() {
        return animationMarker;
    }
}

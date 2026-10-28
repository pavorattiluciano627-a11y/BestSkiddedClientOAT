package dev.echo.client.render;

import dev.echo.client.events.render.WorldRenderEvent;
import dev.echo.client.render.math.RenderMath;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.utils.targeting.TargetSelection;
import java.util.ArrayDeque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

/** Target-centered orbit history with fading ribbons and the original three-part highlight band. */
public class TargetMotionTrail {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final double FULL_CIRCLE = Math.PI * 2;
    private static final double HEIGHT_PERIOD_SECONDS = 1.7;
    private static final double HEIGHT_FRACTION = 0.9;
    private static final long HISTORY_LIFETIME_MILLIS = 420L;
    private static final long SAMPLE_INTERVAL_MILLIS = 20L;
    private static final double BAND_HALF_HEIGHT = 0.025;
    private static final double UPPER_GLOW_HEIGHT = 0.11;
    private static final double LOWER_GLOW_HEIGHT = 0.55;

    // The original setting reference is static; constructing another trail replaces it.
    private static ColorSetting sharedColor;
    private final int segments;
    private final float angularSpeedDegrees;
    private final ArrayDeque<TrailSample> samples = new ArrayDeque<>();
    private LivingEntity selectedTarget;
    private long targetAcquiredMillis;
    private long lastSampleMillis;

    /** The float is multiplied by elapsed seconds before conversion to radians. */
    public TargetMotionTrail(ColorSetting color, int segments, float angularSpeedDegrees) {
        sharedColor = color;
        this.segments = segments;
        this.angularSpeedDegrees = angularSpeedDegrees;
    }

    /** Keeps timestamps intact; the next acquired target starts a new sampling interval. */
    public void clearHistory() {
        selectedTarget = null;
        samples.clear();
    }

    public void render(WorldRenderEvent event) {
        if (CLIENT.player == null || CLIENT.world == null) {
            return;
        }
        LivingEntity target = TargetSelection.recentlyAttacked(400);
        if (target == null) {
            clearHistory();
            return;
        }
        long now = System.currentTimeMillis();
        if (target != selectedTarget) {
            selectedTarget = target;
            samples.clear();
            targetAcquiredMillis = now;
            lastSampleMillis = 0L;
        }
        float tickProgress = event.getTickProgress();
        double previousX = target.lastX;
        double currentX = target.getX();
        double x = previousX + (currentX - previousX) * (double) tickProgress;
        double previousY = target.lastY;
        double currentY = target.getY();
        double y = previousY + (currentY - previousY) * (double) tickProgress;
        double previousZ = target.lastZ;
        double currentZ = target.getZ();
        double z = previousZ + (currentZ - previousZ) * (double) tickProgress;
        double radius = (double) target.getWidth() * 0.85 / Math.cos(Math.PI / (double) segments);
        double targetHeight = target.getHeight();
        double elapsedSeconds = (double) (now - targetAcquiredMillis) / 1000.0;
        double heightPhase = (1.0 - Math.cos(elapsedSeconds / HEIGHT_PERIOD_SECONDS * FULL_CIRCLE)) * 0.5;
        double height = heightPhase * targetHeight * HEIGHT_FRACTION;
        double angle = Math.toRadians(elapsedSeconds * (double) angularSpeedDegrees);

        if (now - lastSampleMillis >= SAMPLE_INTERVAL_MILLIS) {
            samples.addFirst(new TrailSample(height, angle, now));
            lastSampleMillis = now;
        }
        while (!samples.isEmpty() && now - samples.peekLast().createdMillis() > HISTORY_LIFETIME_MILLIS) {
            samples.removeLast();
        }

        DepthTestedWorldGeometry geometry = event.getRenderContext().getDepthTestedGeometry();
        int red = sharedColor.getRed();
        int green = sharedColor.getGreen();
        int blue = sharedColor.getBlue();
        int alpha = sharedColor.getAlpha();
        double previousHeight = height;
        double previousAngle = angle;
        long previousMillis = now;
        for (TrailSample sample : samples) {
            int previousAlpha = RenderMath.trailAlpha(alpha, now, previousMillis);
            int sampleAlpha = RenderMath.trailAlpha(alpha, now, sample.createdMillis());
            if (previousAlpha >= 4 || sampleAlpha >= 4) {
                drawRibbon(geometry, x, y, z, radius,
                        previousHeight, previousAngle, RenderMath.packArgb(red, green, blue, previousAlpha),
                        sample.heightOffset(), sample.angleRadians(), RenderMath.packArgb(red, green, blue, sampleAlpha));
            }
            previousHeight = sample.heightOffset();
            previousAngle = sample.angleRadians();
            previousMillis = sample.createdMillis();
        }

        int coreColor = RenderMath.packArgb(red, green, blue, Math.round(alpha * 0.9F));
        int upperColor = RenderMath.packArgb(red, green, blue, Math.round(alpha * 0.4F));
        int lowerColor = RenderMath.packArgb(red, green, blue, Math.round(alpha * 0.25F));
        int transparentColor = RenderMath.packArgb(red, green, blue, 0);
        double lowerHeight = Math.max(0.0, height - LOWER_GLOW_HEIGHT);
        drawRibbon(geometry, x, y, z, radius,
                height - BAND_HALF_HEIGHT, angle, lowerColor,
                lowerHeight, angle, transparentColor);
        drawRibbon(geometry, x, y, z, radius,
                height + BAND_HALF_HEIGHT + UPPER_GLOW_HEIGHT, angle, transparentColor,
                height + BAND_HALF_HEIGHT, angle, upperColor);
        drawRibbon(geometry, x, y, z, radius,
                height + BAND_HALF_HEIGHT, angle, coreColor,
                height - BAND_HALF_HEIGHT, angle, coreColor);
    }

    private void drawRibbon(WorldGeometry geometry, double x, double y, double z, double radius,
                            double firstHeight, double firstAngle, int firstColor,
                            double secondHeight, double secondAngle, int secondColor) {
        if (Math.abs(firstHeight - secondHeight) < 1.0E-4
                && Math.abs(firstAngle - secondAngle) < 1.0E-4) {
            return;
        }
        double firstY = y + firstHeight;
        double secondY = y + secondHeight;
        for (int segment = 0; segment < segments; segment++) {
            double firstStartAngle = firstAngle + FULL_CIRCLE * segment / segments;
            double firstEndAngle = firstAngle + FULL_CIRCLE * (segment + 1) / segments;
            double secondStartAngle = secondAngle + FULL_CIRCLE * segment / segments;
            double secondEndAngle = secondAngle + FULL_CIRCLE * (segment + 1) / segments;
            double firstStartX = x + Math.cos(firstStartAngle) * radius;
            double firstStartZ = z + Math.sin(firstStartAngle) * radius;
            double firstEndX = x + Math.cos(firstEndAngle) * radius;
            double firstEndZ = z + Math.sin(firstEndAngle) * radius;
            double secondStartX = x + Math.cos(secondStartAngle) * radius;
            double secondStartZ = z + Math.sin(secondStartAngle) * radius;
            double secondEndX = x + Math.cos(secondEndAngle) * radius;
            double secondEndZ = z + Math.sin(secondEndAngle) * radius;
            geometry.drawTriangle(firstStartX, firstY, firstStartZ, firstColor,
                    secondStartX, secondY, secondStartZ, secondColor,
                    firstEndX, firstY, firstEndZ, firstColor);
            geometry.drawTriangle(firstEndX, firstY, firstEndZ, firstColor,
                    secondStartX, secondY, secondStartZ, secondColor,
                    secondEndX, secondY, secondEndZ, secondColor);
        }
    }
}

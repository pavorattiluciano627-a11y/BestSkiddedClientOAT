package dev.echo.client.utils.trajectory;

import net.minecraft.util.Mth;

/**
 * Structured regression body, pending the exact key/branch invariants listed
 * for AssetService.processBatch in needs.json. Excluded from production.
 */
public final class MotionRegression {
    private MotionRegression() {
    }

    public static LinearFit fit(double[] times, double[] values) {
        int count = times.length;
        double sumTime = 0.0;
        double sumValue = 0.0;
        double sumTimeSquared = 0.0;
        double sumTimeValue = 0.0;
        for (int index = 0; index < count; index++) {
            sumTime += times[index];
            sumValue += values[index];
            sumTimeSquared += times[index] * times[index];
            sumTimeValue += times[index] * values[index];
        }
        double denominator = count * sumTimeSquared - sumTime * sumTime;
        double slope = Math.abs(denominator) < 1.0E-9 ? 0.0
                : (count * sumTimeValue - sumTime * sumValue) / denominator;
        double intercept = (sumValue - slope * sumTime) / count;
        double squaredError = 0.0;
        for (int index = 0; index < count; index++) {
            squaredError += Mth.square(intercept + slope * times[index] - values[index]);
        }
        return new LinearFit(intercept, slope, squaredError / count);
    }
}

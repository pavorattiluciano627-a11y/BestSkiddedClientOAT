package dev.echo.client.features.impl.render;

import dev.echo.client.Echo;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.render.DepthTestedWorldGeometry;
import dev.echo.client.render.WorldRenderContext;
import dev.echo.client.setting.BooleanSetting;
import dev.echo.client.setting.ChoiceSetting;
import dev.echo.client.setting.ColorSetting;
import dev.echo.client.setting.FloatSetting;
import dev.echo.client.utils.strings.Concat;
import java.awt.Color;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A 96-segment cone with two facing passes, per-vertex rainbow colors, and an optional opaque rim. */
public class ChinaHatFeature extends Feature {
    public static ChinaHatFeature instance;
    private static final double FULL_CIRCLE = Math.PI * 2;
    private static final int SEGMENTS = 96;
    private static ChoiceSetting mode;
    private static ColorSetting color;
    private static FloatSetting opacity;
    private static FloatSetting spinSpeed;
    private static FloatSetting radius;
    private static FloatSetting height;
    private static BooleanSetting outline;

    public ChinaHatFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("China Hat"), dev.echo.client.bootstrap.CatalogText.literal("Wears a translucent cone hat on your head"), dev.echo.client.features.Category.RENDER));
    }

    @Override
    public void postInitialize() {
        instance = this;
        color.setVisibilityPredicate(ignored -> mode.isSelected("Static"));
        opacity.setVisibilityPredicate(ignored -> mode.isSelected("Rainbow"));
        spinSpeed.setVisibilityPredicate(ignored -> mode.isSelected("Rainbow"));
        color.tooltip("Appearance");
        opacity.tooltip("Appearance");
        spinSpeed.tooltip("Timing");
        outline.tooltip("Appearance");
    }

    public void render(Matrix4f transform) {
        WorldRenderContext context = Echo.worldRenderer;
        if (context == null) {
            return;
        }
        DepthTestedWorldGeometry geometry = context.getDepthTestedGeometry();
        Vec3d camera = context.cameraPosition;
        float ringRadius = radius.getValue();
        float coneHeight = height.getValue();
        boolean rainbow = mode.isSelected("Rainbow");
        int alpha = rainbow ? Math.round(opacity.getValue() * 255.0f) : color.getAlpha();
        float hueOffset = rainbow
                ? (float) ((System.currentTimeMillis() % 400000L) / 1000.0 * spinSpeed.getValue())
                : 0.0f;
        boolean drawOutline = outline.getValue();
        Vector3f transformed = new Vector3f();
        transform.transformPosition(0.0f, coneHeight, 0.0f, transformed);
        double tipX = camera.x + transformed.x;
        double tipY = camera.y + transformed.y;
        double tipZ = camera.z + transformed.z;
        double[] rimX = new double[SEGMENTS + 1];
        double[] rimY = new double[SEGMENTS + 1];
        double[] rimZ = new double[SEGMENTS + 1];
        int[] rimColors = new int[SEGMENTS + 1];
        for (int index = 0; index <= SEGMENTS; index++) {
            float fraction = (float) index / 96.0f;
            double angle = FULL_CIRCLE * fraction;
            transform.transformPosition((float) (-Math.sin(angle) * ringRadius), 0.0f,
                    (float) (Math.cos(angle) * ringRadius), transformed);
            rimX[index] = camera.x + transformed.x;
            rimY[index] = camera.y + transformed.y;
            rimZ[index] = camera.z + transformed.z;
            rimColors[index] = vertexColor(rainbow, fraction, hueOffset, alpha);
        }

        for (int pass = 0; pass < 2; pass++) {
            boolean positiveFacingPass = pass == 0;
            for (int index = 1; index <= SEGMENTS; index++) {
                double edgeX = rimX[index] - rimX[index - 1];
                double edgeY = rimY[index] - rimY[index - 1];
                double edgeZ = rimZ[index] - rimZ[index - 1];
                double apexX = tipX - rimX[index - 1];
                double apexY = tipY - rimY[index - 1];
                double apexZ = tipZ - rimZ[index - 1];
                double normalX = edgeY * apexZ - edgeZ * apexY;
                double normalY = edgeZ * apexX - edgeX * apexZ;
                double normalZ = edgeX * apexY - edgeY * apexX;
                double toCameraX = camera.x - (tipX + rimX[index - 1] + rimX[index]) / 3.0;
                double toCameraY = camera.y - (tipY + rimY[index - 1] + rimY[index]) / 3.0;
                double toCameraZ = camera.z - (tipZ + rimZ[index - 1] + rimZ[index]) / 3.0;
                boolean positiveFacing = normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ > 0.0;
                if (positiveFacing != positiveFacingPass) {
                    continue;
                }
                int tipColor = vertexColor(rainbow, ((float) index - 0.5f) / 96.0f, hueOffset, alpha);
                geometry.drawTriangle(tipX, tipY, tipZ, tipColor,
                        rimX[index - 1], rimY[index - 1], rimZ[index - 1], rimColors[index - 1],
                        rimX[index], rimY[index], rimZ[index], rimColors[index]);
            }
        }
        if (drawOutline) {
            for (int index = 1; index <= SEGMENTS; index++) {
                geometry.drawLine(rimX[index - 1], rimY[index - 1], rimZ[index - 1],
                        rimX[index], rimY[index], rimZ[index], rimColors[index - 1] | 0xff000000);
            }
        }
    }

    private static int vertexColor(boolean rainbow, float fraction, float hueOffset, int alpha) {
        if (!rainbow) {
            return packArgb(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
        int rgb = Color.HSBtoRGB(fraction + hueOffset, 1.0f, 1.0f);
        return alpha << 24 | rgb & 0xffffff;
    }

    private static int packArgb(int red, int green, int blue, int alpha) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | blue & 255;
    }

    /** Appends this declaring class's settings in the recorded classfile order. */
    public static void appendBootstrapSettings(ChinaHatFeature feature) {
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.mode);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.color);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.opacity);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.spinSpeed);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.radius);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.height);
        dev.echo.client.bootstrap.CatalogSettings.append(feature, ChinaHatFeature.outline);
    }

    /** Publishes prepared settings in metadata vector order after all constructors succeed. */
    public static void assignBootstrapSettings(ChinaHatFeature feature, dev.echo.client.setting.FloatSetting preparedHeight, dev.echo.client.setting.ChoiceSetting preparedMode, dev.echo.client.setting.FloatSetting preparedSpinSpeed, dev.echo.client.setting.ColorSetting preparedColor, dev.echo.client.setting.BooleanSetting preparedOutline, dev.echo.client.setting.FloatSetting preparedRadius, dev.echo.client.setting.FloatSetting preparedOpacity) {
        ChinaHatFeature.height = preparedHeight;
        ChinaHatFeature.mode = preparedMode;
        ChinaHatFeature.spinSpeed = preparedSpinSpeed;
        ChinaHatFeature.color = preparedColor;
        ChinaHatFeature.outline = preparedOutline;
        ChinaHatFeature.radius = preparedRadius;
        ChinaHatFeature.opacity = preparedOpacity;
    }
}

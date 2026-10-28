package dev.echo.client.setting;

import java.awt.Color;
import java.util.function.Predicate;

public class ColorSetting extends Setting {
    private int red;
    private int green;
    private int blue;
    private int alpha;
    private float hue;
    private float saturation;
    private float brightness;
    private boolean alphaMode = true;

    public ColorSetting(CharSequence label, int red, int green, int blue, int alpha) {
        super(label);
        this.red = clampByte(red);
        this.green = clampByte(green);
        this.blue = clampByte(blue);
        this.alpha = clampByte(alpha);
        updateHsv();
    }

    public ColorSetting(CharSequence label, int red, int green, int blue, int alpha,
                        Predicate<Object> visibleWhen) {
        super(label, visibleWhen);
        this.red = clampByte(red);
        this.green = clampByte(green);
        this.blue = clampByte(blue);
        this.alpha = clampByte(alpha);
        updateHsv();
    }

    public ColorSetting(CharSequence label, int red, int green, int blue, int alpha, boolean alphaMode) {
        this(label, red, green, blue, alpha);
        this.alphaMode = alphaMode;
    }

    public ColorSetting(CharSequence label, int red, int green, int blue, int alpha,
                        boolean alphaMode, Predicate<Object> visibleWhen) {
        this(label, red, green, blue, alpha, visibleWhen);
        this.alphaMode = alphaMode;
    }

    public ColorSetting(CharSequence label, int red, int green, int blue, int alpha,
                        Predicate<Object> visibleWhen, boolean alphaMode) {
        this(label, red, green, blue, alpha, alphaMode, visibleWhen);
    }

    public ColorSetting(CharSequence label, Color color) {
        this(label, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public ColorSetting(CharSequence label, Color color, boolean alphaMode) {
        this(label, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), alphaMode);
    }

    public ColorSetting(CharSequence label, int argb) {
        super(label);
        setArgb(argb);
    }

    public ColorSetting(CharSequence label, int argb, Predicate<Object> visibleWhen) {
        super(label, visibleWhen);
        setArgb(argb);
    }

    public ColorSetting(CharSequence label, int argb, boolean alphaMode) {
        super(label);
        this.alphaMode = alphaMode;
        setArgb(argb);
    }

    public ColorSetting(CharSequence label, int argb, Predicate<Object> visibleWhen, boolean alphaMode) {
        super(label, visibleWhen);
        this.alphaMode = alphaMode;
        setArgb(argb);
    }

    public ColorSetting(String label, int red, int green, int blue, int alpha) {
        this((CharSequence) label, red, green, blue, alpha);
    }

    public ColorSetting(String label, int red, int green, int blue, int alpha, Predicate<Object> visibleWhen) {
        this((CharSequence) label, red, green, blue, alpha, visibleWhen);
    }

    public ColorSetting(String label, int argb) { this((CharSequence) label, argb); }

    public ColorSetting(String label, int argb, Predicate<Object> visibleWhen) {
        this((CharSequence) label, argb, visibleWhen);
    }

    public ColorSetting(String label, int red, int green, int blue, int alpha, boolean alphaMode) {
        this((CharSequence) label, red, green, blue, alpha, alphaMode);
    }

    public ColorSetting(String label, int red, int green, int blue, int alpha,
                        Predicate<Object> visibleWhen, boolean alphaMode) {
        this((CharSequence) label, red, green, blue, alpha, visibleWhen, alphaMode);
    }

    public ColorSetting(String label, int argb, boolean alphaMode) { this((CharSequence) label, argb, alphaMode); }

    public ColorSetting(String label, int argb, Predicate<Object> visibleWhen, boolean alphaMode) {
        this((CharSequence) label, argb, visibleWhen, alphaMode);
    }

    private static int clampByte(int channel) { return Math.max(0, Math.min(255, channel)); }
    public void setRed(int value) { red = clampByte(value); updateHsv(); notifyChanged(); }
    public void setRedSilently(int value) { red = clampByte(value); updateHsv(); }
    public void setGreen(int value) { green = clampByte(value); updateHsv(); notifyChanged(); }
    public void setGreenSilently(int value) { green = clampByte(value); updateHsv(); }
    public void setBlue(int value) { blue = clampByte(value); updateHsv(); notifyChanged(); }
    public void setBlueSilently(int value) { blue = clampByte(value); updateHsv(); }
    public void setAlpha(int value) { alpha = clampByte(value); notifyChanged(); }
    public void setAlphaSilently(int value) { alpha = clampByte(value); }

    public void setArgb(int argb) {
        alpha = argb >> 24 & 255;
        red = argb >> 16 & 255;
        green = argb >> 8 & 255;
        blue = argb & 255;
        updateHsv();
        notifyChanged();
    }

    public void setRgb(int rgb) {
        red = rgb >> 16 & 255;
        green = rgb >> 8 & 255;
        blue = rgb & 255;
        updateHsv();
        notifyChanged();
    }

    public int getRgb() { return red << 16 | green << 8 | blue; }
    public int getArgb() { return alpha << 24 | red << 16 | green << 8 | blue; }
    public float[] getHsv() { return new float[] {hue, saturation, brightness}; }

    public void setHsv(float hue, float saturation, float brightness) {
        applyHsv(hue, saturation, brightness);
        notifyChanged();
    }

    public void setHsvSilently(float hue, float saturation, float brightness) {
        applyHsv(hue, saturation, brightness);
    }

    private void applyHsv(float hue, float saturation, float brightness) {
        this.hue = wrapHue(hue);
        this.saturation = clampUnit(saturation);
        this.brightness = clampUnit(brightness);
        int rgb = hsvToRgb(this.hue, this.saturation, this.brightness);
        red = rgb >> 16 & 255;
        green = rgb >> 8 & 255;
        blue = rgb & 255;
    }

    // These three original convenience setters deliberately dispatch two notifications.
    public void setHue(float value) { setHsv(value, saturation, brightness); notifyChanged(); }
    public void setSaturation(float value) { setHsv(hue, value, brightness); notifyChanged(); }
    public void setBrightness(float value) { setHsv(hue, saturation, value); notifyChanged(); }

    private void updateHsv() {
        float[] hsv = rgbToHsv(red, green, blue);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    private static float clampUnit(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }

    private static float wrapHue(float value) {
        if (Float.isNaN(value)) return 0.0f;
        float wrapped = value % 1.0f;
        if (wrapped < 0.0f) wrapped += 1.0f;
        if (wrapped == 0.0f && value > 0.0f) return Math.nextDown(1.0f);
        // The source fcmpl branch also takes this arm when infinity % 1 produces NaN.
        if (!(wrapped < 1.0f)) return Math.nextDown(1.0f);
        return wrapped;
    }

    private static float[] rgbToHsv(int red, int green, int blue) {
        float r = red / 255.0f;
        float g = green / 255.0f;
        float b = blue / 255.0f;
        float maximum = Math.max(r, Math.max(g, b));
        float minimum = Math.min(r, Math.min(g, b));
        float difference = maximum - minimum;
        float h = 0.0f;
        float s = maximum == 0.0f ? 0.0f : difference / maximum;
        if (difference != 0.0f) {
            if (maximum == r) h = (g - b) / difference + (g < b ? 6.0f : 0.0f);
            else if (maximum == g) h = (b - r) / difference + 2.0f;
            else h = (r - g) / difference + 4.0f;
            h /= 6.0f;
        }
        return new float[] {h, s, maximum};
    }

    private static int hsvToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0.0f) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float boundedHue = !(hue < 1.0f) ? 0.9999f : hue < 0.0f ? 0.0f : hue;
            float sector = (boundedHue - (float) Math.floor(boundedHue)) * 6.0f;
            float fraction = sector - (float) Math.floor(sector);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * fraction);
            float t = brightness * (1.0f - saturation * (1.0f - fraction));
            switch ((int) sector) {
                case 0 -> { r = toByte(brightness); g = toByte(t); b = toByte(p); }
                case 1 -> { r = toByte(q); g = toByte(brightness); b = toByte(p); }
                case 2 -> { r = toByte(p); g = toByte(brightness); b = toByte(t); }
                case 3 -> { r = toByte(p); g = toByte(q); b = toByte(brightness); }
                case 4 -> { r = toByte(t); g = toByte(p); b = toByte(brightness); }
                case 5 -> { r = toByte(brightness); g = toByte(p); b = toByte(q); }
            }
        }
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    private static int toByte(float value) { return (int) (value * 255.0f + 0.5f); }
    public int getRed() { return red; }
    public int getGreen() { return green; }
    public int getBlue() { return blue; }
    public int getAlpha() { return alpha; }
    public float getHue() { return hue; }
    public float getSaturation() { return saturation; }
    public float getBrightness() { return brightness; }
    public float getRedFraction() { return red / 255.0f; }
    public float getGreenFraction() { return green / 255.0f; }
    public float getBlueFraction() { return blue / 255.0f; }
    public float getAlphaFraction() { return alpha / 255.0f; }
    public boolean isAlphaMode() { return alphaMode; }
    public void setAlphaMode(boolean enabled) { alphaMode = enabled; }
    @Override public String getTypeId() { return "clr"; }
}

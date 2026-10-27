package dev.eliasnvx.catgirl.client.render;

/**
 * Packed ARGB colors: the later versions' {@code FastColor.ARGB32} methods (1.20.1 only has some of them), plus the float
 * channels 1.20.1's models are drawn with.
 */
public final class ARGB32 {

    private ARGB32() {
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int color(int red, int green, int blue) {
        return color(0xFF, red, green, blue);
    }

    /** {@code rgb} with this alpha (0-255). */
    public static int color(int alpha, int rgb) {
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    public static int opaque(int rgb) {
        return rgb | 0xFF000000;
    }

    /** Channel by channel, alpha too. */
    public static int multiply(int a, int b) {
        return color(alpha(a) * alpha(b) / 255, red(a) * red(b) / 255, green(a) * green(b) / 255, blue(a) * blue(b) / 255);
    }

    public static float redF(int color) {
        return red(color) / 255.0F;
    }

    public static float greenF(int color) {
        return green(color) / 255.0F;
    }

    public static float blueF(int color) {
        return blue(color) / 255.0F;
    }

    public static float alphaF(int color) {
        return alpha(color) / 255.0F;
    }
}

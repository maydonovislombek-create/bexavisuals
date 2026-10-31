package com.bexa.visuals.util;

import java.awt.Color;

/** Barcha ranglar ARGB (int) formatida: 0xAARRGGBB. */
public final class ColorUtils {
    private ColorUtils() {}

    // Mavzu ranglari: qora + qizil + yashil
    public static final int BG_DARK = 0xF00B0B0E;
    public static final int BG_PANEL = 0xE6121216;
    public static final int BG_ROW = 0xFF1A1A20;
    public static int RED = 0xFFE53935;
    public static int RED_DARK = 0xFF8E1B1B;
    public static int GREEN = 0xFF2ECC71;
    /** Kamalak aylanish vaqti (soniya). Theme Editor o'zgartiradi. */
    public static float rainbowSpeed = 4f;
    public static final int GRAY = 0xFF3A3A44;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF9A9AA5;

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    public static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    public static int alpha(int c) { return (c >> 24) & 0xFF; }
    public static int red(int c) { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c) { return c & 0xFF; }

    public static float af(int c) { return alpha(c) / 255f; }
    public static float rf(int c) { return red(c) / 255f; }
    public static float gf(int c) { return green(c) / 255f; }
    public static float bf(int c) { return blue(c) / 255f; }

    public static int withAlpha(int c, int a) { return (clamp255(a) << 24) | (c & 0x00FFFFFF); }

    public static int withAlpha(int c, float a) { return withAlpha(c, Math.round(a * 255f)); }

    /** Mavjud alfani ko'paytiradi (fade uchun). */
    public static int mulAlpha(int c, float m) {
        return withAlpha(c, Math.round(alpha(c) * Math.max(0f, Math.min(1f, m))));
    }

    public static int hsb(float h, float s, float b, float a) {
        return withAlpha(Color.HSBtoRGB(h, s, b), a);
    }

    public static float[] toHSB(int argb) {
        return Color.RGBtoHSB(red(argb), green(argb), blue(argb), null);
    }

    /** Kamalak rangi. speedSec = to'liq aylanish vaqti (soniya). */
    public static int rainbow(float speedSec, int offsetMs, float brightness, float alpha) {
        long period = Math.max(1L, (long) (speedSec * 1000L));
        double t = ((System.currentTimeMillis() + offsetMs) % period) / (double) period;
        if (t < 0) t += 1.0;
        return hsb((float) t, 0.85f, brightness, alpha);
    }

    public static int rainbow(float speedSec, int offsetMs) {
        return rainbow(speedSec, offsetMs, 1f, 1f);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return argb(
                Math.round(alpha(c1) + (alpha(c2) - alpha(c1)) * t),
                Math.round(red(c1) + (red(c2) - red(c1)) * t),
                Math.round(green(c1) + (green(c2) - green(c1)) * t),
                Math.round(blue(c1) + (blue(c2) - blue(c1)) * t));
    }

    /** Ikki rang orasida oldinga-orqaga yumshoq almashinuv. */
    public static int gradient(int c1, int c2, float speedSec, int offsetMs) {
        long period = Math.max(1L, (long) (speedSec * 1000L));
        double t = ((System.currentTimeMillis() + offsetMs) % period) / (double) period;
        if (t < 0) t += 1.0;
        float p = (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
        return lerp(c1, c2, p);
    }

    /** Qizil-yashil mavzu gradienti. */
    public static int themeGradient(float speedSec, int offsetMs) {
        return gradient(RED, GREEN, speedSec, offsetMs);
    }

    /** Salomatlik rangi: 1.0 = yashil, 0.0 = qizil. */
    public static int health(float fraction, float alpha) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        return withAlpha(lerp(0xFFE53935, 0xFF2ECC71, fraction), alpha);
    }

    public static int random(java.util.Random rnd, float alpha) {
        return hsb(rnd.nextFloat(), 0.85f, 1f, alpha);
    }
}

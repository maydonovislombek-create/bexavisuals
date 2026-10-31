package com.bexa.visuals.util;

public final class Easing {
    private Easing() {}

    public static float clamp01(float t) { return Math.max(0f, Math.min(1f, t)); }

    public static float linear(float t) { return clamp01(t); }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float easeInCubic(float t) {
        t = clamp01(t);
        return t * t * t;
    }

    public static float easeInOutQuad(float t) {
        t = clamp01(t);
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }

    public static float easeOutQuart(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u * u;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}

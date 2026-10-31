package com.bexa.visuals.util;

import net.minecraft.client.gui.DrawContext;

import java.util.function.IntUnaryOperator;

/** 2D chizish yordamchilari (faqat DrawContext.fill/fillGradient ishlatiladi: barqaror API). */
public final class RenderUtils {
    private RenderUtils() {}

    private static boolean invisible(int color) { return (color >>> 24) == 0; }

    /** Yumaloq to'rtburchakning berilgan qatori uchun chetdan siljish. */
    private static int inset(int row, int h, int r) {
        r = Math.min(r, h / 2);
        if (r <= 0) return 0;
        double dy;
        if (row < r) dy = r - row - 0.5;
        else if (row >= h - r) dy = row - (h - r) + 0.5;
        else return 0;
        double dx = Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
        return r - (int) Math.round(dx);
    }

    public static void roundedRect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || invisible(color)) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(i, h, r);
            c.fill(x + in, y + i, x + w - in, y + i + 1, color);
            c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    public static void roundedOutline(DrawContext c, int x, int y, int w, int h, int r, int t, int color) {
        if (w <= 0 || h <= 0 || invisible(color)) return;
        if (h <= 2 * t || w <= 2 * t) { roundedRect(c, x, y, w, h, r, color); return; }
        r = Math.min(r, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            int oi = inset(row, h, r);
            if (row < t || row >= h - t) {
                c.fill(x + oi, y + row, x + w - oi, y + row + 1, color);
            } else {
                int ii = Math.max(oi, t + inset(row - t, h - 2 * t, Math.max(0, r - t)));
                c.fill(x + oi, y + row, x + ii, y + row + 1, color);
                c.fill(x + w - ii, y + row, x + w - oi, y + row + 1, color);
            }
        }
    }

    /** Yumshoq yorug'lik (glow): tashqi tomonga so'nuvchi konturlar. */
    public static void glow(DrawContext c, int x, int y, int w, int h, int r, int color, int layers) {
        for (int i = 1; i <= layers; i++) {
            int a = Math.round(ColorUtils.alpha(color) / (float) (i * i * 0.6f + 0.4f));
            roundedOutline(c, x - i, y - i, w + 2 * i, h + 2 * i, r + i, 1, ColorUtils.withAlpha(color, a));
        }
    }

    public static void circle(DrawContext c, int cx, int cy, int r, int color) {
        if (r <= 0 || invisible(color)) return;
        double rr = r + 0.5;
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(Math.max(0.0, rr * rr - dy * dy)));
            c.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    public static void ring(DrawContext c, int cx, int cy, int r, int t, int color) {
        if (r <= 0 || invisible(color)) return;
        double ro = r + 0.5, ri = Math.max(0, r - t) + 0.5;
        for (int dy = -r; dy <= r; dy++) {
            int ho = (int) Math.round(Math.sqrt(Math.max(0.0, ro * ro - dy * dy)));
            if (Math.abs(dy) < ri) {
                int hi = (int) Math.round(Math.sqrt(Math.max(0.0, ri * ri - dy * dy)));
                c.fill(cx - ho, cy + dy, cx - hi, cy + dy + 1, color);
                c.fill(cx + hi, cy + dy, cx + ho, cy + dy + 1, color);
            } else {
                c.fill(cx - ho, cy + dy, cx + ho, cy + dy + 1, color);
            }
        }
    }

    public static void diamond(DrawContext c, int cx, int cy, int r, int color) {
        if (invisible(color)) return;
        for (int dy = -r; dy <= r; dy++) {
            int half = r - Math.abs(dy);
            c.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Gorizontal bo'ylab o'zgaruvchi rang (colorAt: 0..1 -> ARGB) bilan to'ldirish. */
    public static void hStrips(DrawContext c, int x, int y, int w, int h, int step, java.util.function.Function<Float, Integer> colorAt) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < w; i += step) {
            int col = colorAt.apply(i / (float) w);
            c.fill(x + i, y, Math.min(x + i + step, x + w), y + h, col);
        }
    }
}

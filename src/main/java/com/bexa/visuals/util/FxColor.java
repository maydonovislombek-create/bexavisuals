package com.bexa.visuals.util;

import com.bexa.visuals.setting.ColorSetting;

/** Effektlar uchun umumiy rang hisoblash (alfa'siz, to'liq ko'rinadigan RGB qaytaradi). */
public final class FxColor {
    private FxColor() {}

    public static final String[] FX_MODES = {"Theme", "Rainbow", "Static", "Gradient", "Random"};

    /**
     * @param f    0..1 (chiziq/burchak bo'ylab o'rin)
     * @param t    vaqt (soniya)
     * @param base "Random"/"Health" rejimlari uchun tayyor rang
     */
    public static int rgb(String mode, float f, double t, int base, ColorSetting c1, ColorSetting c2) {
        float m = 0.5f + 0.5f * (float) Math.sin(Math.PI * 2 * f + t * 2.0);
        switch (mode) {
            case "Rainbow":
                return ColorUtils.hsb(f + (float) (t * 0.25), 0.85f, 1f, 1f);
            case "Static":
                return ColorUtils.withAlpha(c1.getColor((int) (f * 1500)), 255);
            case "Gradient":
                return ColorUtils.lerp(ColorUtils.withAlpha(c1.getColor((int) (f * 1500)), 255),
                        ColorUtils.withAlpha(c2.getColor((int) (f * 1500)), 255), m);
            case "Random":
            case "Health":
                return ColorUtils.withAlpha(base, 255);
            default: // Theme
                return ColorUtils.lerp(ColorUtils.RED, ColorUtils.GREEN, m);
        }
    }

    public static int withA(int rgb, float alpha) {
        return ColorUtils.withAlpha(rgb, Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f));
    }
}

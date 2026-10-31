package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;

/** BexaVisuals ranglarini (asosiy, ikkinchi, kamalak tezligi) jonli o'zgartiradi. */
public class ThemeEditor extends Module {
    private static final int DEFAULT_RED = ColorUtils.RED, DEFAULT_GREEN = ColorUtils.GREEN;

    private final ColorSetting primary = add(new ColorSetting("Primary (Red)", ColorUtils.RED));
    private final ColorSetting secondary = add(new ColorSetting("Secondary (Green)", ColorUtils.GREEN));
    private final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow Speed", 4, 1, 10, 0.5));

    public ThemeEditor() {
        super("Theme Editor", "Live-edit BexaVisuals' accent colors", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        ColorUtils.RED = ColorUtils.withAlpha(primary.getColor(), 255);
        ColorUtils.RED_DARK = ColorUtils.lerp(ColorUtils.RED, 0xFF000000, 0.45f);
        ColorUtils.GREEN = ColorUtils.withAlpha(secondary.getColor(), 255);
        ColorUtils.rainbowSpeed = rainbowSpeed.getF();
    }

    @Override
    protected void onDisable() {
        ColorUtils.RED = DEFAULT_RED;
        ColorUtils.GREEN = DEFAULT_GREEN;
        ColorUtils.RED_DARK = 0xFF8E1B1B;
        ColorUtils.rainbowSpeed = 4f;
    }
}

package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;

/**
 * Gamma sozlamasini maksimal darajaga olib chiqib, dunyoni yorug' qiladi
 * (SimpleOptionMixin orqali). Ba'zi shader/resurs to'plamlari gammaga
 * boshqacha ta'sir qilishi mumkin.
 */
public class FullBright extends Module {
    public static boolean gamma;
    public static final double GAMMA = 1024.0;

    public FullBright() {
        super("Full Bright", "Makes the world fully bright by maxing out gamma", Category.VISUALS);
    }

    @Override
    protected void onEnable() { gamma = true; }

    @Override
    protected void onDisable() { gamma = false; }
}

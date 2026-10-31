package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.MultiBoolSetting;

/** Bir nechta render-o'chirish o'tkazgichlari: No Hurt Cam, No Fire Overlay, No Weather. */
public class NoRender extends Module {
    public static volatile boolean hurtCam, fire, weather;

    private final MultiBoolSetting toggles = add(new MultiBoolSetting(
            "Disable", "No Hurt Cam", true, "No Fire Overlay", true, "No Weather", false));

    public NoRender() {
        super("No Render", "Disables selected visual overlays (hurt cam, fire, weather)", Category.VISUALS);
    }

    private void sync() {
        boolean on = isEnabled();
        hurtCam = on && toggles.get("No Hurt Cam");
        fire = on && toggles.get("No Fire Overlay");
        weather = on && toggles.get("No Weather");
    }

    @Override
    public void onTick() { sync(); }

    @Override
    protected void onEnable() { sync(); }

    @Override
    protected void onDisable() { hurtCam = fire = weather = false; }
}

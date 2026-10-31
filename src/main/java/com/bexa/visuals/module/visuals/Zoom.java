package com.bexa.visuals.module.visuals;

import com.bexa.visuals.BexaVisualsClient;
import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.Animation;
import net.minecraft.client.option.SimpleOption;

/** Right-click-style zoom: tugma bosib turilganda FOV pasayadi. */
public class Zoom extends Module {
    private final NumberSetting zoomFov = add(new NumberSetting("Zoom FOV", 20, 5, 60, 1));
    private final NumberSetting smoothing = add(new NumberSetting("Smoothing", 10, 2, 30, 1));

    private final Animation anim = new Animation(0f, 10f);
    private int savedFov = -1;
    private boolean zooming;

    public Zoom() {
        super("Zoom", "Hold the zoom key to smoothly zoom in", Category.VISUALS);
        setEnabled(true);
    }

    @Override
    protected void onDisable() { restore(); }

    private void restore() {
        if (savedFov >= 0 && mc.options != null) {
            mc.options.getFov().setValue(savedFov);
        }
        savedFov = -1;
        zooming = false;
        anim.snap(0f);
    }

    @Override
    public void onTick() {
        if (mc.options == null || BexaVisualsClient.zoomKey == null) return;
        boolean down = mc.currentScreen == null && BexaVisualsClient.zoomKey.isPressed();
        SimpleOption<Integer> fov = mc.options.getFov();
        if (down && !zooming) {
            savedFov = fov.getValue();
            zooming = true;
        }
        anim.setTarget(down ? 1f : 0f);
        float v = anim.update();
        if (savedFov >= 0) {
            float target = savedFov + (zoomFov.getF() - savedFov) * v;
            fov.setValue(Math.round(target));
        }
        if (!down && v <= 0.001f && zooming) restore();
    }
}

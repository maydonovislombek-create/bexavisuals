package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.NumberSetting;
import net.minecraft.client.option.SimpleOption;

/** FOV'ga doimiy ofset qo'shadi (yugurish/tezlik effektlaridan mustaqil holda). */
public class FovChanger extends Module {
    private final NumberSetting offset = add(new NumberSetting("FOV Offset", 10, -30, 30, 1));

    private int baseFov = -1;

    public FovChanger() {
        super("FOV Changer", "Adds a constant offset to your field of view", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        if (mc.options != null) baseFov = mc.options.getFov().getValue();
    }

    @Override
    protected void onDisable() {
        if (baseFov >= 0 && mc.options != null) mc.options.getFov().setValue(baseFov);
        baseFov = -1;
    }

    @Override
    public void onTick() {
        if (mc.options == null) return;
        if (baseFov < 0) baseFov = mc.options.getFov().getValue();
        SimpleOption<Integer> fov = mc.options.getFov();
        int target = Math.max(30, Math.min(110, baseFov + Math.round(offset.getF())));
        if (fov.getValue() != target) fov.setValue(target);
    }
}

package com.bexa.visuals.module.esp;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.MultiBoolSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.FxColor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Ko'zdan nishonga qarab chiziq. */
public class Tracers extends Module {
    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", FxColor.FX_MODES));
    private final ColorSetting color1 = add(new ColorSetting("Color", ColorUtils.RED));
    private final ColorSetting color2 = add(new ColorSetting("Color 2", ColorUtils.GREEN));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.65, 0.1, 1.0, 0.05));
    private final NumberSetting width = add(new NumberSetting("Width", 1.0, 0.3, 3.0, 0.1));
    private final NumberSetting maxDist = add(new NumberSetting("Max Distance", 36, 8, 128, 1));
    private final MultiBoolSetting targets = add(new MultiBoolSetting("Targets", "Players", true, "Mobs", true, "Animals", false));
    private final com.bexa.visuals.setting.BoolSetting throughWalls = add(new com.bexa.visuals.setting.BoolSetting("Through Walls", true));

    public Tracers() {
        super("Tracers", "Draws a line from your view to nearby entities", Category.ESP);
        color1.visibleWhen(() -> colorMode.getIndex() == 2 || colorMode.getIndex() == 3);
        color2.visibleWhen(() -> colorMode.getIndex() == 3);
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (mc.player == null) return;
        List<LivingEntity> list = EspTargets.collect(targets.get("Players"), targets.get("Mobs"), targets.get("Animals"), maxDist.get());
        if (list.isEmpty()) return;
        Vec3d eye = mc.player.getCameraPosVec(1f);
        if (!Render3D.begin(stack, cam, throughWalls.get())) return;
        try {
            double t = System.nanoTime() / 1.0E9;
            for (LivingEntity e : list) {
                int base = colorMode.is("Random") ? ColorUtils.hsb((e.getId() % 360) / 360f, 0.85f, 1f, 1f)
                        : colorMode.is("Health") ? ColorUtils.health(e.getHealth() / Math.max(1f, e.getMaxHealth()), 1f) : 0;
                int c = FxColor.withA(FxColor.rgb(colorMode.get(), 0.5f, t, base, color1, color2), alpha.getF());
                Render3D.line(eye.x, eye.y, eye.z, ColorUtils.mulAlpha(c, 0.15f),
                        e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ(), c, width.get() * 0.01, 1f);
            }
        } finally {
            Render3D.end();
        }
    }
}

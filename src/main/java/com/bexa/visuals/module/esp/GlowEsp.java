package com.bexa.visuals.module.esp;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.MultiBoolSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Chams: jonzotlarni devor ortidan ko'rinadigan shaffof rangli silueti. */
public class GlowEsp extends Module {
    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", "Theme", "Rainbow", "Health"));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.35, 0.05, 0.8, 0.05));
    private final NumberSetting maxDist = add(new NumberSetting("Max Distance", 48, 8, 128, 1));
    private final MultiBoolSetting targets = add(new MultiBoolSetting("Targets", "Players", true, "Mobs", true, "Animals", false));

    public GlowEsp() {
        super("Glow", "Translucent silhouette visible through walls", Category.ESP);
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        List<LivingEntity> list = EspTargets.collect(targets.get("Players"), targets.get("Mobs"), targets.get("Animals"), maxDist.get());
        if (list.isEmpty()) return;
        if (!Render3D.begin(stack, cam, true)) return;
        try {
            double t = System.nanoTime() / 1.0E9;
            for (LivingEntity e : list) {
                int c = switch (colorMode.get()) {
                    case "Rainbow" -> ColorUtils.rainbow(4f, e.getId() * 90);
                    case "Health" -> ColorUtils.health(e.getHealth() / Math.max(1f, e.getMaxHealth()), 1f);
                    default -> ColorUtils.themeGradient(3f, e.getId() * 120);
                };
                int fc = ColorUtils.mulAlpha(c, alpha.getF());
                Box b = e.getBoundingBox();
                Render3D.box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, fc, fc);
            }
        } finally {
            Render3D.end();
        }
    }
}

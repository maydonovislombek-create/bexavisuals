package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Easing;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

/** Urilgan nuqtada kengayib so'nadigan pufakcha/halqa effekti. */
public class HitBubble extends Module {
    private final ModeSetting shape = add(new ModeSetting("Shape", "Ring", "Ring", "Sphere", "Burst"));
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.GREEN));
    private final NumberSetting size = add(new NumberSetting("Size", 0.45, 0.1, 1.5, 0.05));
    private final NumberSetting lifetime = add(new NumberSetting("Lifetime", 0.5, 0.1, 1.5, 0.05));

    private static class Fx { double x, y, z; long start; }

    private final ArrayList<Fx> list = new ArrayList<>();

    public HitBubble() {
        super("Hit Bubble", "Spawns an expanding bubble where you hit an entity", Category.VISUALS);
    }

    @Override
    protected void onDisable() { list.clear(); }

    @Override
    public void onAttack(Entity entity, Vec3d pos) {
        Fx f = new Fx();
        f.x = pos.x; f.y = pos.y; f.z = pos.z;
        f.start = System.nanoTime();
        if (list.size() > 24) list.remove(0);
        list.add(f);
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (list.isEmpty()) return;
        long now = System.nanoTime();
        float life = lifetime.getF();
        if (!Render3D.begin(stack, cam, false)) return;
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                Fx f = list.get(i);
                float age = (now - f.start) / 1.0E9f;
                if (age >= life) { list.remove(i); continue; }
                float t = age / life;
                float ease = Easing.easeOutCubic(t);
                float a = (1f - t) * (1f - t);
                int c = ColorUtils.mulAlpha(color.getColor(), a);
                double r = size.get() * (0.15 + ease);
                switch (shape.getIndex()) {
                    case 1 -> Render3D.sphere(f.x, f.y, f.z, r * 0.6, 8, 14, fc -> c, 0.5f);
                    case 2 -> {
                        Render3D.belt(f.x, f.y, f.z, r, 0.02 + 0.01 * (1 - t), 32, 0, Math.PI * 2, fc -> c, 0f);
                        Render3D.belt(f.x, f.y, f.z, r * 0.55, 0.015, 24, 0, Math.PI * 2, fc -> ColorUtils.mulAlpha(c, 0.7f), 0f);
                    }
                    default -> Render3D.belt(f.x, f.y, f.z, r, 0.03 * (1 - t * 0.6), 40, 0, Math.PI * 2, fc -> c, 0f);
                }
            }
        } finally {
            Render3D.end();
        }
    }
}

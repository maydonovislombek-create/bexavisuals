package com.bexa.visuals.module.players;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Siz zarba bergan jonzot o'lganda chaqnovchi effekt. */
public class KillEffect extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.RED));
    private final NumberSetting lifetime = add(new NumberSetting("Lifetime", 0.6, 0.2, 1.5, 0.05));
    private final NumberSetting trackTime = add(new NumberSetting("Track Time", 3.0, 0.5, 8.0, 0.5));

    private static class Tracked { double x, y, z; long lastHit; boolean existed; }
    private static class Fx { double x, y, z; long start; }

    private final Map<Integer, Tracked> tracked = new HashMap<>();
    private final ArrayList<Fx> fx = new ArrayList<>();

    public KillEffect() {
        super("Kill Effect", "A burst effect when an entity you hit dies", Category.PLAYERS);
    }

    @Override
    protected void onDisable() { tracked.clear(); fx.clear(); }

    @Override
    public void onAttack(Entity entity, Vec3d pos) {
        Tracked t = tracked.computeIfAbsent(entity.getId(), k -> new Tracked());
        t.x = entity.getX(); t.y = entity.getY() + entity.getHeight() * 0.5; t.z = entity.getZ();
        t.lastHit = System.nanoTime();
        t.existed = true;
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        long now = System.nanoTime();
        var it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if ((now - e.getValue().lastHit) / 1.0E9 > trackTime.get()) { it.remove(); continue; }
            Entity ent = mc.world.getEntityById(e.getKey());
            if (ent != null) {
                e.getValue().x = ent.getX();
                e.getValue().y = ent.getY() + ent.getHeight() * 0.5;
                e.getValue().z = ent.getZ();
            } else {
                Fx f = new Fx();
                f.x = e.getValue().x; f.y = e.getValue().y; f.z = e.getValue().z;
                f.start = now;
                fx.add(f);
                it.remove();
            }
        }
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (fx.isEmpty()) return;
        long now = System.nanoTime();
        float life = lifetime.getF();
        if (!Render3D.begin(stack, cam, false)) return;
        try {
            for (int i = fx.size() - 1; i >= 0; i--) {
                Fx f = fx.get(i);
                float age = (now - f.start) / 1.0E9f;
                if (age >= life) { fx.remove(i); continue; }
                float t = age / life;
                float a = (1f - t) * (1f - t);
                int c = ColorUtils.mulAlpha(color.getColor(), a);
                double r = 0.15 + t * 1.1;
                Render3D.sphere(f.x, f.y, f.z, r * 0.5, 8, 14, fc -> c, 0.4f);
                Render3D.belt(f.x, f.y, f.z, r, 0.05 * (1 - t), 36, 0, Math.PI * 2, fc -> c, 0f);
                int n = 10;
                for (int k = 0; k < n; k++) {
                    double a2 = k * Math.PI * 2 / n + t * 3.0;
                    double rr = r * 1.2;
                    double x2 = f.x + Math.cos(a2) * rr, z2 = f.z + Math.sin(a2) * rr;
                    double y2 = f.y + Math.sin(t * 6 + k) * 0.2;
                    Render3D.line(f.x, f.y, f.z, ColorUtils.mulAlpha(c, 0.2f), x2, y2, z2, c, 0.012, 1f);
                }
            }
        } finally {
            Render3D.end();
        }
    }
}

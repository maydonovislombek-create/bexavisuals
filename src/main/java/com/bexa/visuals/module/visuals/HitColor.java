package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Urilgan jonzotni qisqa vaqt rangli overlay bilan ajratadi. */
public class HitColor extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.RED));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.45, 0.1, 1.0, 0.05));
    private final NumberSetting duration = add(new NumberSetting("Duration", 0.35, 0.1, 1.5, 0.05));

    private final Map<Integer, Long> hits = new HashMap<>();

    public HitColor() {
        super("Hit Color", "Flashes a color overlay on entities you hit", Category.VISUALS);
    }

    @Override
    protected void onDisable() { hits.clear(); }

    @Override
    public void onAttack(Entity entity, Vec3d pos) {
        if (entity instanceof LivingEntity) hits.put(entity.getId(), System.nanoTime());
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (hits.isEmpty() || mc.world == null) return;
        long now = System.nanoTime();
        float dur = duration.getF();
        boolean any = false;
        Iterator<Map.Entry<Integer, Long>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            if ((now - it.next().getValue()) / 1.0E9f > dur) it.remove(); else any = true;
        }
        if (!any) return;

        if (!Render3D.begin(stack, cam, false)) return;
        try {
            for (Map.Entry<Integer, Long> e : hits.entrySet()) {
                Entity ent = mc.world.getEntityById(e.getKey());
                if (ent == null) continue;
                float age = (now - e.getValue()) / 1.0E9f / dur;
                float a = (1f - age) * alpha.getF();
                int c = ColorUtils.mulAlpha(color.getColor(), a);
                Box b = ent.getBoundingBox().expand(0.03);
                Render3D.box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, c, c);
            }
        } finally {
            Render3D.end();
        }
    }
}

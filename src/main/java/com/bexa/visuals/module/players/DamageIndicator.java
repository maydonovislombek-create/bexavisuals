package com.bexa.visuals.module.players;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Projection;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Jonzotlar jarohat olganda tepasida suzuvchi raqam ko'rsatadi. */
public class DamageIndicator extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.RED));
    private final NumberSetting lifetime = add(new NumberSetting("Lifetime", 0.7, 0.3, 2.0, 0.1));
    private final BoolSetting onlyNearby = add(new BoolSetting("Only Nearby (24 blocks)", true));

    private static class Popup { double x, y, z; long start; int amount; }

    private final Map<Integer, Float> lastHealth = new HashMap<>();
    private final ArrayList<Popup> popups = new ArrayList<>();

    public DamageIndicator() {
        super("Damage Indicator", "Floating damage numbers when an entity loses health", Category.PLAYERS);
    }

    @Override
    protected void onDisable() { lastHealth.clear(); popups.clear(); }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        double maxSq = onlyNearby.get() ? 24 * 24 : Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
            if (mc.player.squaredDistanceTo(e) > maxSq) continue;
            float hp = le.getHealth();
            Float prev = lastHealth.put(e.getId(), hp);
            if (prev != null && hp < prev - 0.01f) {
                Popup p = new Popup();
                p.x = e.getX(); p.y = e.getY() + e.getHeight() + 0.1; p.z = e.getZ();
                p.start = System.nanoTime();
                p.amount = Math.round(prev - hp);
                if (p.amount > 0) {
                    if (popups.size() > 60) popups.remove(0);
                    popups.add(p);
                }
            }
        }
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (popups.isEmpty()) return;
        long now = System.nanoTime();
        float life = lifetime.getF();
        for (int i = popups.size() - 1; i >= 0; i--) {
            Popup p = popups.get(i);
            float age = (now - p.start) / 1.0E9f;
            if (age >= life) { popups.remove(i); continue; }
            float t = age / life;
            double y = p.y + t * 0.6;
            if (!Projection.toScreen(p.x, y, p.z)) continue;
            int c = ColorUtils.mulAlpha(color.getColor(), 1f - t);
            String txt = "-" + p.amount;
            int x = (int) Projection.OUT[0] - mc.textRenderer.getWidth(txt) / 2;
            ctx.drawText(mc.textRenderer, txt, x, (int) Projection.OUT[1], c, true);
        }
    }
}

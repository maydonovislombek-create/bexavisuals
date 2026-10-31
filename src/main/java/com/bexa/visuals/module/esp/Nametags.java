package com.bexa.visuals.module.esp;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.MultiBoolSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Projection;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

import java.util.List;

/** Jonzotlar ustida nom/sog'liq/masofa yorlig'i. */
public class Nametags extends Module {
    private final MultiBoolSetting show = add(new MultiBoolSetting("Show", "Health", true, "Distance", true));
    private final NumberSetting maxDist = add(new NumberSetting("Max Distance", 40, 8, 128, 1));
    private final com.bexa.visuals.setting.MultiBoolSetting targets = add(new MultiBoolSetting("Targets", "Players", true, "Mobs", true, "Animals", false));

    public Nametags() {
        super("Nametags", "Floating name/health/distance tags above entities", Category.ESP);
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.player == null) return;
        List<LivingEntity> list = EspTargets.collect(targets.get("Players"), targets.get("Mobs"), targets.get("Animals"), maxDist.get());
        for (LivingEntity e : list) {
            if (!Projection.toScreen(e.getX(), e.getY() + e.getHeight() + 0.3, e.getZ())) continue;
            String name = e.getDisplayName() != null ? e.getDisplayName().getString() : e.getName().getString();
            StringBuilder sb = new StringBuilder(name);
            if (show.get("Health")) sb.append(" §7| §c").append(Math.round(e.getHealth())).append("♥");
            if (show.get("Distance")) sb.append(" §7| ").append(Math.round(mc.player.distanceTo(e))).append("m");
            String text = sb.toString();
            int tw = mc.textRenderer.getWidth(text);
            int x = (int) Projection.OUT[0] - tw / 2, y = (int) Projection.OUT[1] - 10;
            RenderUtils.roundedRect(ctx, x - 3, y - 2, tw + 6, 11, 3, ColorUtils.withAlpha(0xFF000000, 130));
            ctx.drawText(mc.textRenderer, text, x, y, ColorUtils.WHITE, false);
        }
    }
}

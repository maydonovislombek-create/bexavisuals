package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Faol effektlar (potion) ro'yxati. */
public class PotionHud extends Module {
    public PotionHud() {
        super("Potion HUD", "Lists your active status effects with remaining time", Category.MISC);
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.player == null) return;
        var effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;
        int x = mc.getWindow().getScaledWidth() - 110, y = mc.getWindow().getScaledHeight() / 2 - (effects.size() * 14) / 2;
        for (StatusEffectInstance fx : effects) {
            String name = fx.getEffectType().value().getName().getString();
            int sec = fx.getDuration() / 20;
            String txt = name + " " + (fx.getAmplifier() + 1) + " (" + sec + "s)";
            int col;
            try {
                col = fx.getEffectType().value().getColor() | 0xFF000000;
            } catch (Throwable t) {
                col = ColorUtils.GREEN;
            }
            RenderUtils.roundedRect(ctx, x, y, 106, 12, 3, 0xA0121216);
            ctx.fill(x, y, x + 3, y + 12, col);
            ctx.drawText(mc.textRenderer, txt, x + 6, y + 2, ColorUtils.WHITE, false);
            y += 14;
        }
    }
}

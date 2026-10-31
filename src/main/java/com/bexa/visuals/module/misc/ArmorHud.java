package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/** Kiyilgan zirh buyumlari ro'yxati (ikonka + bardoshlik chizig'i). */
public class ArmorHud extends Module {
    private boolean itemRenderBroken;

    public ArmorHud() {
        super("Armor HUD", "Shows worn armor pieces with durability bars", Category.MISC);
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.player == null) return;
        Iterable<ItemStack> armor = mc.player.getArmorItems();
        int x = 6, y = mc.getWindow().getScaledHeight() / 2 - 40, slot = 20;
        for (ItemStack stack : armor) {
            if (stack.isEmpty()) { y += slot; continue; }
            RenderUtils.roundedRect(ctx, x, y, 18, 18, 3, 0xA0121216);
            if (!itemRenderBroken) {
                try {
                    ctx.drawItem(stack, x + 1, y + 1);
                    ctx.drawItemInSlot(mc.textRenderer, stack, x + 1, y + 1);
                } catch (Throwable t) {
                    itemRenderBroken = true;
                }
            }
            if (stack.isDamageable()) {
                float frac = 1f - stack.getDamage() / (float) stack.getMaxDamage();
                int c = ColorUtils.health(frac, 1f);
                RenderUtils.roundedRect(ctx, x + 1, y + 16, Math.round(16 * frac), 2, 1, c);
            }
            y += slot;
        }
    }
}

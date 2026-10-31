package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;

/** Standart nishon o'rniga maxsus chiziladigan nishon. */
public class CustomCrosshair extends Module {
    /** InGameHudMixin standart nishonni yashirish uchun o'qiydi. */
    public static boolean hideVanilla;

    private final ModeSetting shape = add(new ModeSetting("Shape", "Cross", "Cross", "Dot", "Circle", "T-Shape"));
    private final NumberSetting size = add(new NumberSetting("Size", 6, 2, 16, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", 3, 0, 12, 1));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 2, 1, 5, 1));
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.WHITE));
    private final BoolSetting dynamicGap = add(new BoolSetting("Dynamic Gap", true));
    private final BoolSetting outline = add(new BoolSetting("Outline", true));

    public CustomCrosshair() {
        super("Crosshair", "A custom-drawn crosshair replacing the vanilla one", Category.VISUALS);
    }

    @Override
    protected void onEnable() { hideVanilla = true; }

    @Override
    protected void onDisable() { hideVanilla = false; }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.options.hudHidden || mc.player == null) return;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;

        int t = thickness.getI();
        double g = gap.get();
        if (dynamicGap.get()) {
            double speed = mc.player.getVelocity().horizontalLength();
            g += Math.min(10.0, speed * 40.0);
        }
        int s = size.getI();
        int col = color.getColor();
        int outlineCol = ColorUtils.withAlpha(0xFF000000, 0.55f);

        switch (shape.getIndex()) {
            case 0 -> { // Cross
                bar(ctx, cx - (int) g - s, cy - t / 2, s, t, col, outlineCol);
                bar(ctx, cx + (int) g, cy - t / 2, s, t, col, outlineCol);
                bar(ctx, cx - t / 2, cy - (int) g - s, t, s, col, outlineCol);
                bar(ctx, cx - t / 2, cy + (int) g, t, s, col, outlineCol);
            }
            case 1 -> RenderUtils.circle(ctx, cx, cy, Math.max(1, t), col); // Dot
            case 2 -> RenderUtils.ring(ctx, cx, cy, s, t, col); // Circle
            default -> { // T-Shape
                bar(ctx, cx - s, cy - t / 2, s * 2, t, col, outlineCol);
                bar(ctx, cx - t / 2, cy, t, s, col, outlineCol);
            }
        }
    }

    private void bar(DrawContext ctx, int x, int y, int w, int h, int col, int outlineCol) {
        if (outline.get()) RenderUtils.roundedRect(ctx, x - 1, y - 1, w + 2, h + 2, 0, outlineCol);
        RenderUtils.roundedRect(ctx, x, y, w, h, 0, col);
    }
}

package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class Watermark extends Module {
    private final ModeSetting colorMode = add(new ModeSetting("Color", "Theme", "Theme", "Rainbow", "Static"));
    private final ColorSetting color = add(new ColorSetting("Static Color", ColorUtils.RED));
    private final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.6, 2.0, 0.1));
    private final BoolSetting showFps = add(new BoolSetting("Show FPS", true));
    private final BoolSetting background = add(new BoolSetting("Background", true));

    public Watermark() {
        super("Watermark", "Animated BexaVisuals watermark on the HUD", Category.MISC);
        setEnabled(true);
    }

    private int colorAt(int i) {
        if (colorMode.is("Rainbow")) return ColorUtils.rainbow(4f, i * 90);
        if (colorMode.is("Static")) return ColorUtils.withAlpha(color.getColor(i * 90), 255);
        return ColorUtils.themeGradient(3f, i * 140);
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        String name = "BexaVisuals";
        String suffix = showFps.get() ? "  |  " + mc.getCurrentFps() + " FPS" : "";
        int nameW = tr.getWidth(name);
        int totalW = nameW + tr.getWidth(suffix);
        float sc = scale.getF();

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(6f, 6f, 0f);
        ms.scale(sc, sc, 1f);
        if (background.get()) {
            RenderUtils.roundedRect(ctx, -4, -3, totalW + 8, 15, 4, 0xB00B0B0E);
            RenderUtils.roundedOutline(ctx, -4, -3, totalW + 8, 15, 4, 1, ColorUtils.withAlpha(colorAt(0), 0.55f));
        }
        int x = 0;
        for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            ctx.drawText(tr, ch, x, 1, colorAt(i), true);
            x += tr.getWidth(ch);
        }
        if (!suffix.isEmpty()) ctx.drawText(tr, suffix, nameW, 1, ColorUtils.TEXT_DIM, true);
        ms.pop();
    }
}

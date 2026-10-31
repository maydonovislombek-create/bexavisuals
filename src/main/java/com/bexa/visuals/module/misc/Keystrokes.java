package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** WASD + sichqoncha tugmalarini ko'rsatuvchi klaviatura indikatori. */
public class Keystrokes extends Module {
    public Keystrokes() {
        super("Keystrokes", "Shows WASD and mouse button presses on screen", Category.MISC);
    }

    private boolean key(int code) {
        if (mc.getWindow() == null) return false;
        return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), code) == GLFW.GLFW_PRESS;
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        int bx = mc.getWindow().getScaledWidth() - 76, by = mc.getWindow().getScaledHeight() - 80;
        int s = 20;
        key(ctx, bx + s, by, s, s, "W", key(GLFW.GLFW_KEY_W));
        key(ctx, bx, by + s, s, s, "A", key(GLFW.GLFW_KEY_A));
        key(ctx, bx + s, by + s, s, s, "S", key(GLFW.GLFW_KEY_S));
        key(ctx, bx + 2 * s, by + s, s, s, "D", key(GLFW.GLFW_KEY_D));

        boolean l = com.bexa.visuals.util.Cps.leftDown, r = com.bexa.visuals.util.Cps.rightDown;
        key(ctx, bx - 18, by + s, 14, s, "", l);
        key(ctx, bx + 3 * s + 4, by + s, 14, s, "", r);
    }

    private void key(DrawContext ctx, int x, int y, int w, int h, String label, boolean down) {
        RenderUtils.roundedRect(ctx, x, y, w, h, 3, down ? ColorUtils.withAlpha(ColorUtils.themeGradient(3f, x * 10), 220) : 0xB0121216);
        RenderUtils.roundedOutline(ctx, x, y, w, h, 3, 1, ColorUtils.withAlpha(0xFF2A2A32, 255));
        if (!label.isEmpty()) {
            int tw = mc.textRenderer.getWidth(label);
            ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, ColorUtils.WHITE, false);
        }
    }
}

package com.bexa.visuals.module.misc;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.MultiBoolSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Cps;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/** FPS, Ping, CPS va koordinatalarni ko'rsatuvchi HUD. */
public class InfoHud extends Module {
    private final MultiBoolSetting show = add(new MultiBoolSetting(
            "Show", "FPS", true, "Ping", true, "CPS", true, "Coordinates", true));

    public InfoHud() {
        super("Info HUD", "FPS, ping, CPS and coordinates overlay", Category.MISC);
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (mc.player == null || mc.options.hudHidden) return;
        List<String> lines = new ArrayList<>();
        if (show.get("FPS")) lines.add("FPS: " + mc.getCurrentFps());
        if (show.get("Ping")) {
            int ping = -1;
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            lines.add("Ping: " + (ping >= 0 ? ping + "ms" : "--"));
        }
        if (show.get("CPS")) lines.add("CPS: " + Cps.left() + " / " + Cps.right());
        if (show.get("Coordinates")) {
            lines.add(String.format("XYZ: %.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (lines.isEmpty()) return;

        int lh = 11;
        int w = 0;
        for (String l : lines) w = Math.max(w, mc.textRenderer.getWidth(l));
        int x = mc.getWindow().getScaledWidth() - w - 14;
        int y = 6;
        RenderUtils.roundedRect(ctx, x - 4, y - 3, w + 8, lines.size() * lh + 4, 4, 0xB00B0B0E);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(mc.textRenderer, lines.get(i), x, y + i * lh, ColorUtils.withAlpha(ColorUtils.themeGradient(4f, i * 300), 255), true);
        }
    }
}

package com.bexa.visuals.module.esp;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.MultiBoolSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.FxColor;
import com.bexa.visuals.util.Projection;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Jonzotlar atrofida quti (3D yoki 2D) va ixtiyoriy sog'liq chizig'i. */
public class EspModule extends Module {
    private final ModeSetting display = add(new ModeSetting("Display", "Outline Box", "Box", "Outline Box", "2D Box"));
    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", FxColor.FX_MODES));
    private final ColorSetting color1 = add(new ColorSetting("Color", ColorUtils.RED));
    private final ColorSetting color2 = add(new ColorSetting("Color 2", ColorUtils.GREEN));
    private final BoolSetting healthBar = add(new BoolSetting("Health Bar", true));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.8, 0.1, 1.0, 0.05));
    private final NumberSetting width = add(new NumberSetting("Width", 1.5, 0.5, 4.0, 0.1));
    private final NumberSetting maxDist = add(new NumberSetting("Max Distance", 48, 8, 128, 1));
    private final MultiBoolSetting targets = add(new MultiBoolSetting("Targets", "Players", true, "Mobs", true, "Animals", false));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));

    private static final double[][] CORNERS = {
            {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1},
            {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}
    };

    public EspModule() {
        super("ESP", "Boxes around players and mobs, with an optional health bar", Category.ESP);
        color1.visibleWhen(() -> colorMode.getIndex() == 2 || colorMode.getIndex() == 3);
        color2.visibleWhen(() -> colorMode.getIndex() == 3);
    }

    private List<LivingEntity> list() {
        return EspTargets.collect(targets.get("Players"), targets.get("Mobs"), targets.get("Animals"), maxDist.get());
    }

    private int colorFor(LivingEntity e, float t) {
        double tm = System.nanoTime() / 1.0E9;
        int base = switch (colorMode.get()) {
            case "Random" -> ColorUtils.hsb((e.getId() % 360) / 360f, 0.85f, 1f, 1f);
            case "Health" -> ColorUtils.health(e.getHealth() / Math.max(1f, e.getMaxHealth()), 1f);
            default -> 0;
        };
        return FxColor.withA(FxColor.rgb(colorMode.get(), t, tm, base, color1, color2), alpha.getF());
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (display.is("2D Box")) return; // 2D rejimi HUD bosqichida chiziladi
        List<LivingEntity> list = list();
        if (list.isEmpty()) return;
        if (!Render3D.begin(stack, cam, throughWalls.get())) return;
        try {
            for (LivingEntity e : list) {
                Box b = e.getBoundingBox();
                int c1 = colorFor(e, 0f), c2 = colorFor(e, 1f);
                if (display.is("Box")) {
                    int fill1 = ColorUtils.mulAlpha(c1, 0.22f), fill2 = ColorUtils.mulAlpha(c2, 0.22f);
                    Render3D.box(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, fill1, fill2);
                }
                Render3D.boxLines(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, width.get() * 0.006, c1);
            }
        } finally {
            Render3D.end();
        }
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        List<LivingEntity> list = list();
        if (list.isEmpty()) return;
        boolean twoD = display.is("2D Box");
        for (LivingEntity e : list) {
            if (twoD) drawBox2D(ctx, e);
            else if (healthBar.get()) drawHealth(ctx, e);
        }
    }

    private void drawBox2D(DrawContext ctx, LivingEntity e) {
        Box b = e.getBoundingBox();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean any = false;
        for (double[] c : CORNERS) {
            double wx = b.minX + (b.maxX - b.minX) * c[0];
            double wy = b.minY + (b.maxY - b.minY) * c[1];
            double wz = b.minZ + (b.maxZ - b.minZ) * c[2];
            if (!Projection.toScreen(wx, wy, wz)) continue;
            any = true;
            minX = Math.min(minX, Projection.OUT[0]);
            minY = Math.min(minY, Projection.OUT[1]);
            maxX = Math.max(maxX, Projection.OUT[0]);
            maxY = Math.max(maxY, Projection.OUT[1]);
        }
        if (!any || maxX <= minX || maxY <= minY) return;

        int c = colorFor(e, 0f);
        RenderUtils.roundedOutline(ctx, (int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY), 2, Math.max(1, width.getI()), c);

        if (healthBar.get()) {
            float hp = e.getHealth() / Math.max(1f, e.getMaxHealth());
            int bw = 3, bh = (int) (maxY - minY);
            int bx = (int) minX - bw - 3;
            RenderUtils.roundedRect(ctx, bx, (int) minY, bw, bh, 1, ColorUtils.withAlpha(0xFF000000, 150));
            int filled = Math.round(bh * hp);
            RenderUtils.roundedRect(ctx, bx, (int) minY + (bh - filled), bw, filled, 1, ColorUtils.health(hp, 1f));
        }
    }

    private void drawHealth(DrawContext ctx, LivingEntity e) {
        if (!Projection.toScreen(e.getX(), e.getY() + e.getHeight() + 0.25, e.getZ())) return;
        float hp = e.getHealth() / Math.max(1f, e.getMaxHealth());
        int w = 26, h = 3;
        int x = (int) Projection.OUT[0] - w / 2, y = (int) Projection.OUT[1];
        RenderUtils.roundedRect(ctx, x, y, w, h, 1, ColorUtils.withAlpha(0xFF000000, 150));
        RenderUtils.roundedRect(ctx, x, y, Math.round(w * hp), h, 1, ColorUtils.health(hp, 1f));
    }
}

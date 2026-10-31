package com.bexa.visuals.module.players;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** O'yinchilar ortidan qoluvchi rangli iz. */
public class Trails extends Module {
    private static final int MAX_POINTS = 60;

    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", "Theme", "Rainbow", "Per-Player"));
    private final NumberSetting width = add(new NumberSetting("Width", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting length = add(new NumberSetting("Length (sec)", 1.2, 0.3, 4.0, 0.1));
    private final BoolSetting others = add(new BoolSetting("Other Players", true));

    private final Map<Integer, ArrayDeque<double[]>> data = new HashMap<>();

    public Trails() {
        super("Trails", "Leaves a colored trail behind players as they move", Category.PLAYERS);
    }

    @Override
    protected void onDisable() { data.clear(); }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        long now = System.nanoTime();
        record(mc.player, now);
        if (others.get()) {
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player) record(p, now);
            }
        }
        double keepNs = length.get() * 1.0E9;
        for (ArrayDeque<double[]> d : data.values()) {
            while (!d.isEmpty() && now - (long) d.peekFirst()[3] > keepNs) d.pollFirst();
        }
    }

    private void record(PlayerEntity p, long now) {
        ArrayDeque<double[]> d = data.computeIfAbsent(p.getId(), k -> new ArrayDeque<>());
        d.addLast(new double[]{p.getX(), p.getY() + 0.05, p.getZ(), now});
        while (d.size() > MAX_POINTS) d.pollFirst();
    }

    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (data.isEmpty()) return;
        if (!Render3D.begin(stack, cam, false)) return;
        try {
            long now = System.nanoTime();
            double keepNs = length.get() * 1.0E9;
            for (Map.Entry<Integer, ArrayDeque<double[]>> e : data.entrySet()) {
                ArrayDeque<double[]> d = e.getValue();
                if (d.size() < 2) continue;
                int n = Math.min(d.size(), Render3D.MAX - 1);
                int i = 0;
                int baseColor = colorMode.is("Per-Player") ? ColorUtils.hsb((e.getKey() % 360) / 360f, 0.85f, 1f, 1f) : 0;
                for (double[] p : d) {
                    if (i >= n) break;
                    Render3D.X[i] = p[0];
                    Render3D.Y[i] = p[1];
                    Render3D.Z[i] = p[2];
                    i++;
                }
                final int fn = n;
                final int fbase = baseColor;
                Render3D.ribbon(n, false, width.get() * 0.03, 1f, f -> {
                    float age = 1f - f;
                    float alpha = 1f - age;
                    int c = switch (colorMode.get()) {
                        case "Rainbow" -> ColorUtils.rainbow(3f, (int) (f * 1200));
                        case "Per-Player" -> fbase;
                        default -> ColorUtils.themeGradient(2.5f, (int) (f * 1400));
                    };
                    return ColorUtils.mulAlpha(c, alpha);
                });
            }
        } finally {
            Render3D.end();
        }
    }
}

package com.bexa.visuals.util;

import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Jonzot pozitsiyasini kadrlar bo'yicha yumshatadi (20 tick/s sakrashini yo'qotadi). */
public final class EntitySmoother {
    private EntitySmoother() {}

    private static final Map<Integer, double[]> MAP = new HashMap<>(); // x, y, z, lastFrame
    private static long lastNanos = System.nanoTime();
    private static float dt = 0.016f;
    private static int frame = 0;

    public static void beginFrame() {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - lastNanos) / 1.0E9f);
        lastNanos = now;
        frame++;
        if ((frame & 127) == 0) {
            Iterator<Map.Entry<Integer, double[]>> it = MAP.entrySet().iterator();
            while (it.hasNext()) {
                if (frame - (int) it.next().getValue()[3] > 200) it.remove();
            }
        }
    }

    /** Qaytadigan massiv: [x, y, z]. Massivni o'zgartirmang. */
    public static double[] get(Entity e) {
        double[] p = MAP.get(e.getId());
        if (p == null) {
            p = new double[]{e.getX(), e.getY(), e.getZ(), frame};
            MAP.put(e.getId(), p);
            return p;
        }
        if ((int) p[3] != frame) {
            double k = 1.0 - Math.exp(-dt * 22.0);
            double dx = e.getX() - p[0], dy = e.getY() - p[1], dz = e.getZ() - p[2];
            if (dx * dx + dy * dy + dz * dz > 100.0) { // teleport
                p[0] = e.getX(); p[1] = e.getY(); p[2] = e.getZ();
            } else {
                p[0] += dx * k; p[1] += dy * k; p[2] += dz * k;
            }
            p[3] = frame;
        }
        return p;
    }
}

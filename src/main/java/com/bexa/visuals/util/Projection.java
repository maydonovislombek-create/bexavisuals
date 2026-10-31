package com.bexa.visuals.util;

import com.bexa.visuals.module.visuals.AspectRatio;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Dunyo nuqtasini ekran (GUI-scale) koordinatasiga o'giradi. Natija OUT[0], OUT[1] da. */
public final class Projection {
    private Projection() {}

    public static final float[] OUT = new float[2];

    public static boolean toScreen(double x, double y, double z) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.options == null) return false;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || !cam.isReady()) return false;
        Vec3d p = cam.getPos();

        double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
        double cp = Math.cos(pitch);
        double fx = -Math.sin(yaw) * cp, fy = -Math.sin(pitch), fz = Math.cos(yaw) * cp;
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;

        double dx = x - p.x, dy = y - p.y, dz = z - p.z;
        double lz = dx * fx + dy * fy + dz * fz;
        if (lz < 0.05) return false;
        double lx = dx * rx + dz * rz;
        double ly = dx * ux + dy * uy + dz * uz;

        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        float aspect = AspectRatio.target > 0f ? AspectRatio.target
                : mc.getWindow().getFramebufferWidth() / (float) Math.max(1, mc.getWindow().getFramebufferHeight());
        double ndcX = lx / (lz * tanHalf * aspect);
        double ndcY = ly / (lz * tanHalf);
        if (Math.abs(ndcX) > 1.6 || Math.abs(ndcY) > 1.6) return false;
        OUT[0] = (float) ((ndcX + 1.0) * 0.5 * mc.getWindow().getScaledWidth());
        OUT[1] = (float) ((1.0 - ndcY) * 0.5 * mc.getWindow().getScaledHeight());
        return true;
    }
}

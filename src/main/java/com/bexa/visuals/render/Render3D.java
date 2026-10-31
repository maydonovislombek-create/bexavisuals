package com.bexa.visuals.render;

import com.bexa.visuals.util.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Dunyo (3D) chizish yadrosi. Hamma narsa bitta QUADS buferiga yig'iladi va end() da bir marta chiziladi.
 * Koordinatalar dunyo koordinatalarida beriladi; kameraga nisbatan siljitish ichkarida bajariladi.
 * Hot-path'da yangi obyekt yaratilmaydi (scratch massivlar statik).
 */
public final class Render3D {
    private Render3D() {}

    @FunctionalInterface
    public interface ColorFn { int at(float f); }

    public static final int MAX = 512;
    /** Poliliniya/ko'pburchak nuqtalari uchun umumiy "scratch" massivlar. */
    public static final double[] X = new double[MAX], Y = new double[MAX], Z = new double[MAX];

    private static final double[] OX = new double[MAX], OZ = new double[MAX];
    private static final int[] COL = new int[MAX];
    private static final Matrix4f IDENTITY = new Matrix4f();

    private static BufferBuilder buf;
    private static Matrix4f matrix = IDENTITY;
    private static double camX, camY, camZ;
    private static int vertices;
    private static boolean active;

    /** Chizishni boshlaydi. false qaytsa, end() chaqirish shart emas. */
    public static boolean begin(MatrixStack stack, Vec3d cam, boolean throughWalls) {
        if (active || cam == null) return false;
        Matrix4f stackMatrix = stack != null ? stack.peek().getPositionMatrix() : IDENTITY;
        // Agar RenderSystem model-view matritsasi allaqachon kamera burilishini o'z ichiga olgan bo'lsa,
        // vertexlarga qayta qo'llamaymiz (ikki marta burilib ketmasligi uchun).
        Matrix4f mv = RenderSystem.getModelViewMatrix();
        boolean mvHasCamera = mv != null && !mv.equals(IDENTITY, 1.0E-4f);
        matrix = mvHasCamera ? IDENTITY : stackMatrix;

        camX = cam.x;
        camY = cam.y;
        camZ = cam.z;
        vertices = 0;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (throughWalls) RenderSystem.disableDepthTest(); else RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        active = true;
        return true;
    }

    public static void end() {
        if (!active) return;
        active = false;
        try {
            BuiltBuffer built = buf.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            buf = null;
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    // ---------------- primitivlar ----------------
    private static void v(double x, double y, double z, int c) {
        buf.vertex(matrix, (float) (x - camX), (float) (y - camY), (float) (z - camZ))
                .color(ColorUtils.red(c), ColorUtils.green(c), ColorUtils.blue(c), ColorUtils.alpha(c));
        vertices++;
    }

    public static void quad(double x1, double y1, double z1, int c1,
                            double x2, double y2, double z2, int c2,
                            double x3, double y3, double z3, int c3,
                            double x4, double y4, double z4, int c4) {
        if (!active) return;
        v(x1, y1, z1, c1);
        v(x2, y2, z2, c2);
        v(x3, y3, z3, c3);
        v(x4, y4, z4, c4);
    }

    /** Uchburchak (QUADS buferida to'rtinchi vertex takrorlanadi). */
    public static void tri(double x1, double y1, double z1, int c1,
                           double x2, double y2, double z2, int c2,
                           double x3, double y3, double z3, int c3) {
        quad(x1, y1, z1, c1, x2, y2, z2, c2, x3, y3, z3, c3, x3, y3, z3, c3);
    }

    /** Ikki rangli yassi disk (markaz -> chet). */
    public static void disc(double cx, double y, double cz, double r, int segs, int centerColor, int edgeColor) {
        if (!active || r <= 0) return;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            tri(cx, y, cz, centerColor,
                    cx + Math.cos(a0) * r, y, cz + Math.sin(a0) * r, edgeColor,
                    cx + Math.cos(a1) * r, y, cz + Math.sin(a1) * r, edgeColor);
        }
    }

    /** Chetida rang funksiyasi bo'yicha o'zgaruvchi disk. */
    public static void discFn(double cx, double y, double cz, double r, int segs, ColorFn cf, float centerMul, float edgeMul) {
        if (!active || r <= 0) return;
        int cc = ColorUtils.mulAlpha(cf.at(0f), centerMul);
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
            int c0 = ColorUtils.mulAlpha(cf.at(i / (float) segs), edgeMul);
            int c1 = ColorUtils.mulAlpha(cf.at((i + 1) / (float) segs), edgeMul);
            tri(cx, y, cz, cc,
                    cx + Math.cos(a0) * r, y, cz + Math.sin(a0) * r, c0,
                    cx + Math.cos(a1) * r, y, cz + Math.sin(a1) * r, c1);
        }
    }

    /** X/Y/Z massivlaridagi yopiq ko'pburchakni markazdan to'ldiradi. */
    public static void fan(int n, double cx, double cy, double cz, ColorFn cf, float centerMul, float edgeMul) {
        if (!active || n < 3) return;
        int cc = ColorUtils.mulAlpha(cf.at(0f), centerMul);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            int ci = ColorUtils.mulAlpha(cf.at(i / (float) n), edgeMul);
            int cj = ColorUtils.mulAlpha(cf.at((i + 1) / (float) n), edgeMul);
            tri(cx, cy, cz, cc, X[i], Y[i], Z[i], ci, X[j], Y[j], Z[j], cj);
        }
    }

    /**
     * X/Y/Z massivlaridagi nuqtalar bo'ylab gorizontal tasma (ribbon) chizadi.
     * Burchaklarda miter (kesishma) hisoblanadi, shuning uchun ko'pburchaklar toza ko'rinadi.
     *
     * @param softness 1 = qattiq chetlar; 0 = chetlari shaffof (yumshoq nur)
     */
    public static void ribbon(int n, boolean closed, double halfW, float softness, ColorFn cf) {
        if (!active || n < 2 || n > MAX || halfW <= 0) return;

        for (int i = 0; i < n; i++) {
            int pi = closed ? (i - 1 + n) % n : Math.max(i - 1, 0);
            int ni = closed ? (i + 1) % n : Math.min(i + 1, n - 1);
            double e1x = X[i] - X[pi], e1z = Z[i] - Z[pi];
            double e2x = X[ni] - X[i], e2z = Z[ni] - Z[i];
            double l1 = Math.hypot(e1x, e1z), l2 = Math.hypot(e2x, e2z);
            double n1x = 0, n1z = 0, n2x = 0, n2z = 0;
            if (l1 > 1e-9) { n1x = -e1z / l1; n1z = e1x / l1; }
            if (l2 > 1e-9) { n2x = -e2z / l2; n2z = e2x / l2; }
            if (l1 <= 1e-9) { n1x = n2x; n1z = n2z; }
            if (l2 <= 1e-9) { n2x = n1x; n2z = n1z; }
            double ax = n1x + n2x, az = n1z + n2z;
            double al = Math.hypot(ax, az);
            if (al < 1e-6) {
                OX[i] = n1x * halfW;
                OZ[i] = n1z * halfW;
            } else {
                ax /= al;
                az /= al;
                double dot = ax * n1x + az * n1z;
                double miter = Math.min(2.0, 1.0 / Math.max(0.5, dot));
                OX[i] = ax * halfW * miter;
                OZ[i] = az * halfW * miter;
            }
            COL[i] = cf.at(closed ? i / (float) n : i / (float) (n - 1));
        }

        boolean hard = softness >= 0.999f;
        int segs = closed ? n : n - 1;
        for (int i = 0; i < segs; i++) {
            int j = (i + 1) % n;
            double lx0 = X[i] + OX[i], lz0 = Z[i] + OZ[i], rx0 = X[i] - OX[i], rz0 = Z[i] - OZ[i];
            double lx1 = X[j] + OX[j], lz1 = Z[j] + OZ[j], rx1 = X[j] - OX[j], rz1 = Z[j] - OZ[j];
            int c0 = COL[i], c1 = COL[j];
            if (hard) {
                quad(lx0, Y[i], lz0, c0, rx0, Y[i], rz0, c0, rx1, Y[j], rz1, c1, lx1, Y[j], lz1, c1);
            } else {
                int e0 = ColorUtils.mulAlpha(c0, softness), e1 = ColorUtils.mulAlpha(c1, softness);
                quad(lx0, Y[i], lz0, e0, X[i], Y[i], Z[i], c0, X[j], Y[j], Z[j], c1, lx1, Y[j], lz1, e1);
                quad(X[i], Y[i], Z[i], c0, rx0, Y[i], rz0, e0, rx1, Y[j], rz1, e1, X[j], Y[j], Z[j], c1);
            }
        }
    }

    // =====================================================================
    //                       4-QISM: 3D primitivlar
    // =====================================================================
    private static final double TAU = Math.PI * 2;

    /** Kameraga qaragan (billboard) chiziq. softness 1 = qattiq, 0 = chetlari shaffof (nur). */
    public static void line(double x1, double y1, double z1, int c1,
                            double x2, double y2, double z2, int c2,
                            double halfW, float softness) {
        if (!active) return;
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double mx = (x1 + x2) * 0.5 - camX, my = (y1 + y2) * 0.5 - camY, mz = (z1 + z2) * 0.5 - camZ;
        double px = dy * mz - dz * my, py = dz * mx - dx * mz, pz = dx * my - dy * mx;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-9) return;
        double sc = halfW / pl;
        px *= sc; py *= sc; pz *= sc;
        if (softness >= 0.999f) {
            quad(x1 + px, y1 + py, z1 + pz, c1, x1 - px, y1 - py, z1 - pz, c1,
                    x2 - px, y2 - py, z2 - pz, c2, x2 + px, y2 + py, z2 + pz, c2);
        } else {
            int e1 = ColorUtils.mulAlpha(c1, softness), e2 = ColorUtils.mulAlpha(c2, softness);
            quad(x1 + px, y1 + py, z1 + pz, e1, x1, y1, z1, c1, x2, y2, z2, c2, x2 + px, y2 + py, z2 + pz, e2);
            quad(x1, y1, z1, c1, x1 - px, y1 - py, z1 - pz, e1, x2 - px, y2 - py, z2 - pz, e2, x2, y2, z2, c2);
        }
    }

    /**
     * Vertikal "kamar" (silindr tasma): har tomondan ko'rinadi. Pastki/ustki qirralar edgeMul bilan so'nadi.
     * f = burchak bo'yicha 0..1 (yoy uzunligiga nisbatan).
     */
    public static void belt(double cx, double cy, double cz, double r, double halfH, int n,
                            double start, double sweep, ColorFn cf, float edgeMul) {
        if (!active || n < 1 || r <= 0) return;
        for (int i = 0; i < n; i++) {
            double a0 = start + sweep * i / n, a1 = start + sweep * (i + 1) / n;
            int c0 = cf.at(i / (float) n), c1 = cf.at((i + 1) / (float) n);
            int e0 = ColorUtils.mulAlpha(c0, edgeMul), e1 = ColorUtils.mulAlpha(c1, edgeMul);
            double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r;
            double x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;
            quad(x0, cy - halfH, z0, e0, x1, cy - halfH, z1, e1, x1, cy, z1, c1, x0, cy, z0, c0);
            quad(x0, cy, z0, c0, x1, cy, z1, c1, x1, cy + halfH, z1, e1, x0, cy + halfH, z0, e0);
        }
    }

    /** Ixtiyoriy tekislikdagi ellips (u, v - o'zaro perpendikulyar birlik vektorlar), billboard chiziqlardan. */
    public static void ellipse3(double cx, double cy, double cz,
                                double ux, double uy, double uz, double vx, double vy, double vz,
                                double ru, double rv, int n, double start, double sweep,
                                double halfW, float softness, ColorFn cf) {
        if (!active || n < 1) return;
        double px = 0, py = 0, pz = 0;
        int pc = 0;
        for (int i = 0; i <= n; i++) {
            double a = start + sweep * i / n;
            double ca = Math.cos(a) * ru, sa = Math.sin(a) * rv;
            double x = cx + ux * ca + vx * sa, y = cy + uy * ca + vy * sa, z = cz + uz * ca + vz * sa;
            int c = cf.at(i / (float) n);
            if (i > 0) line(px, py, pz, pc, x, y, z, c, halfW, softness);
            px = x; py = y; pz = z; pc = c;
        }
    }

    /** Qutining 6 ta yoqi: pastki yuz cBot, ustki yuz cTop, yon yoqlar pastdan yuqoriga gradient. */
    public static void box(double x1, double y1, double z1, double x2, double y2, double z2, int cBot, int cTop) {
        if (!active) return;
        quad(x1, y1, z1, cBot, x2, y1, z1, cBot, x2, y1, z2, cBot, x1, y1, z2, cBot);
        quad(x1, y2, z1, cTop, x2, y2, z1, cTop, x2, y2, z2, cTop, x1, y2, z2, cTop);
        quad(x1, y1, z1, cBot, x2, y1, z1, cBot, x2, y2, z1, cTop, x1, y2, z1, cTop);
        quad(x1, y1, z2, cBot, x2, y1, z2, cBot, x2, y2, z2, cTop, x1, y2, z2, cTop);
        quad(x1, y1, z1, cBot, x1, y1, z2, cBot, x1, y2, z2, cTop, x1, y2, z1, cTop);
        quad(x2, y1, z1, cBot, x2, y1, z2, cBot, x2, y2, z2, cTop, x2, y2, z1, cTop);
    }

    private static final double[] CUBE_X = new double[8], CUBE_Y = new double[8], CUBE_Z = new double[8];
    private static final int[][] CUBE_FACES = {{0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}};
    private static final int[][] CUBE_EDGES = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    /** Aylanuvchi kub: yuzlar faceColor, qirralar edgeCf bo'yicha. */
    public static void cube(double cx, double cy, double cz, double half, double rotY, double rotX,
                            double halfW, int faceColor, ColorFn edgeCf) {
        if (!active) return;
        double cyw = Math.cos(rotY), syw = Math.sin(rotY), cxw = Math.cos(rotX), sxw = Math.sin(rotX);
        for (int i = 0; i < 8; i++) {
            double lx = ((i & 1) == 0 ? -1 : 1) * half;
            double ly = ((i & 2) == 0 ? -1 : 1) * half;
            double lz = ((i & 4) == 0 ? -1 : 1) * half;
            double y1 = ly * cxw - lz * sxw, z1 = ly * sxw + lz * cxw;
            double x2 = lx * cyw + z1 * syw, z2 = -lx * syw + z1 * cyw;
            CUBE_X[i] = cx + x2;
            CUBE_Y[i] = cy + y1;
            CUBE_Z[i] = cz + z2;
        }
        if (ColorUtils.alpha(faceColor) > 0) {
            for (int[] f : CUBE_FACES) {
                quad(CUBE_X[f[0]], CUBE_Y[f[0]], CUBE_Z[f[0]], faceColor,
                        CUBE_X[f[1]], CUBE_Y[f[1]], CUBE_Z[f[1]], faceColor,
                        CUBE_X[f[2]], CUBE_Y[f[2]], CUBE_Z[f[2]], faceColor,
                        CUBE_X[f[3]], CUBE_Y[f[3]], CUBE_Z[f[3]], faceColor);
            }
        }
        for (int[] e : CUBE_EDGES) {
            line(CUBE_X[e[0]], CUBE_Y[e[0]], CUBE_Z[e[0]], edgeCf.at(e[0] / 8f),
                    CUBE_X[e[1]], CUBE_Y[e[1]], CUBE_Z[e[1]], edgeCf.at(e[1] / 8f), halfW, 1f);
        }
    }

    /** Shar (lat-lon to'r). poleMul: qutblarda alfa ko'paytiruvchisi (ekvatorda 1). */
    public static void sphere(double cx, double cy, double cz, double r, int latN, int lonN, ColorFn cf, float poleMul) {
        if (!active || r <= 0) return;
        for (int i = 0; i < latN; i++) {
            double p0 = -Math.PI / 2 + Math.PI * i / latN, p1 = -Math.PI / 2 + Math.PI * (i + 1) / latN;
            double y0 = Math.sin(p0) * r, y1 = Math.sin(p1) * r;
            double r0 = Math.cos(p0) * r, r1 = Math.cos(p1) * r;
            float m0 = poleMul + (1f - poleMul) * (float) Math.cos(p0);
            float m1 = poleMul + (1f - poleMul) * (float) Math.cos(p1);
            for (int j = 0; j < lonN; j++) {
                double a0 = TAU * j / lonN, a1 = TAU * (j + 1) / lonN;
                int ca = cf.at(j / (float) lonN), cb = cf.at((j + 1) / (float) lonN);
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                quad(cx + c0 * r0, cy + y0, cz + s0 * r0, ColorUtils.mulAlpha(ca, m0),
                        cx + c1 * r0, cy + y0, cz + s1 * r0, ColorUtils.mulAlpha(cb, m0),
                        cx + c1 * r1, cy + y1, cz + s1 * r1, ColorUtils.mulAlpha(cb, m1),
                        cx + c0 * r1, cy + y1, cz + s0 * r1, ColorUtils.mulAlpha(ca, m1));
            }
        }
    }

    /** Kristall (oktaedr): tepa uchi cTop, ekvator cMid, pastki uchi cBot. */
    public static void crystal(double cx, double cy, double cz, double r, double h, double rot,
                               int cTop, int cMid, int cBot) {
        if (!active) return;
        for (int k = 0; k < 4; k++) {
            double a0 = rot + k * Math.PI / 2, a1 = rot + (k + 1) * Math.PI / 2;
            double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r;
            double x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;
            tri(cx, cy + h, cz, cTop, x0, cy, z0, cMid, x1, cy, z1, cMid);
            tri(cx, cy - h * 0.7, cz, cBot, x0, cy, z0, cMid, x1, cy, z1, cMid);
        }
    }

    /** Piramida/konus: uchi (ax,ay,az), asosi baseY balandlikda. */
    public static void pyramid(double ax, double ay, double az, double baseY, double r, int n, double rot,
                               int cApex, int cBase) {
        if (!active || n < 3) return;
        int cb2 = ColorUtils.mulAlpha(cBase, 0.6f);
        for (int i = 0; i < n; i++) {
            double a0 = rot + TAU * i / n, a1 = rot + TAU * (i + 1) / n;
            double x0 = ax + Math.cos(a0) * r, z0 = az + Math.sin(a0) * r;
            double x1 = ax + Math.cos(a1) * r, z1 = az + Math.sin(a1) * r;
            tri(ax, ay, az, cApex, x0, baseY, z0, cBase, x1, baseY, z1, cBase);
            tri(ax, baseY, az, cb2, x0, baseY, z0, cBase, x1, baseY, z1, cBase);
        }
    }

    /** Gul bargi (uchburchak-"uçqur" shakl): yaw - aylanish, tilt - egilish. */
    public static void petal(double cx, double cy, double cz, double size, double yaw, double tilt, int color) {
        if (!active) return;
        double ct = Math.cos(tilt), st = Math.sin(tilt), cyw = Math.cos(yaw), syw = Math.sin(yaw);
        double[] lx = {0, -0.6, 0, 0.6};
        double[] lz = {-1.0, 0, 1.2, 0};
        double[] ox = new double[4], oy = new double[4], oz = new double[4];
        for (int i = 0; i < 4; i++) {
            double x = lx[i] * size, z = lz[i] * size;
            double y1 = -z * st, z1 = z * ct;
            ox[i] = cx + x * cyw + z1 * syw;
            oy[i] = cy + y1;
            oz[i] = cz - x * syw + z1 * cyw;
        }
        int tip = ColorUtils.mulAlpha(color, 0.7f);
        quad(ox[0], oy[0], oz[0], color, ox[1], oy[1], oz[1], color, ox[2], oy[2], oz[2], tip, ox[3], oy[3], oz[3], color);
    }

    /** Qutining 12 ta qirrasi (billboard chiziqlar). */
    public static void boxLines(double x1, double y1, double z1, double x2, double y2, double z2, double halfW, int c) {
        line(x1, y1, z1, c, x2, y1, z1, c, halfW, 1f);
        line(x2, y1, z1, c, x2, y1, z2, c, halfW, 1f);
        line(x2, y1, z2, c, x1, y1, z2, c, halfW, 1f);
        line(x1, y1, z2, c, x1, y1, z1, c, halfW, 1f);
        line(x1, y2, z1, c, x2, y2, z1, c, halfW, 1f);
        line(x2, y2, z1, c, x2, y2, z2, c, halfW, 1f);
        line(x2, y2, z2, c, x1, y2, z2, c, halfW, 1f);
        line(x1, y2, z2, c, x1, y2, z1, c, halfW, 1f);
        line(x1, y1, z1, c, x1, y2, z1, c, halfW, 1f);
        line(x2, y1, z1, c, x2, y2, z1, c, halfW, 1f);
        line(x2, y1, z2, c, x2, y2, z2, c, halfW, 1f);
        line(x1, y1, z2, c, x1, y2, z2, c, halfW, 1f);
    }
}

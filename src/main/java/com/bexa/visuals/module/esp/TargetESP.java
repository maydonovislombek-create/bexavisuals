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
import com.bexa.visuals.util.Easing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;

/** Hujum qilingan / nishonga olingan jonzot atrofida 15 xil effekt. */
public class TargetESP extends Module {
    private static final String[] MODES = {
            "Ring", "Circle", "Box", "Outline Box", "Cube", "Orbiting Orbs", "Crystals", "Rotating Rings",
            "Halo", "Pulse Sphere", "Tracer", "Arrow", "Pentagram", "Chains", "Sakura"
    };
    private static final String[] COLOR_MODES = {"Static", "Rainbow", "Gradient", "Health", "Theme"};
    private static final double TAU = Math.PI * 2;

    private final ModeSetting mode = add(new ModeSetting("Mode", "Ring", MODES));
    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", COLOR_MODES));
    private final ColorSetting color1 = add(new ColorSetting("Color", ColorUtils.RED));
    private final ColorSetting color2 = add(new ColorSetting("Color 2", ColorUtils.GREEN));
    private final NumberSetting size = add(new NumberSetting("Size", 1.0, 0.5, 2.5, 0.05));
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 4.0, 0.1));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.85, 0.1, 1.0, 0.05));
    private final NumberSetting width = add(new NumberSetting("Width", 1.5, 0.5, 4.0, 0.1));
    private final NumberSetting fadeTime = add(new NumberSetting("Fade Time", 0.3, 0.05, 1.0, 0.05));
    private final NumberSetting keepTime = add(new NumberSetting("Keep Time", 1.5, 0.0, 10.0, 0.1));
    private final NumberSetting maxDist = add(new NumberSetting("Max Distance", 24, 6, 64, 1));
    private final ModeSetting source = add(new ModeSetting("Target Source", "Both", "Attack", "Crosshair", "Both"));
    private final MultiBoolSetting targets = add(new MultiBoolSetting("Targets", "Players", true, "Mobs", true, "Animals", false));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));

    // ---- nishon holati ----
    private LivingEntity target;
    private long lastSeen;
    private boolean snap;
    private double sx, sy, sz;
    private float tw = 0.6f, th = 1.8f, hpF = 1f, fadeVal;
    private long lastFrame = System.nanoTime();
    private final long t0 = System.nanoTime();

    // ---- kadr davomidagi holat ----
    private Vec3d camPos = Vec3d.ZERO;
    private double fx, fy, fz, mx, my, mz, sc, tm, lw, hw;
    private float curAlpha, curMul = 1f, constF;
    private boolean taper;
    private int colorModeIdx;

    private final Render3D.ColorFn cf = this::colorAt;
    private final Render3D.ColorFn constCf = f -> colorAt(constF);

    public TargetESP() {
        super("Target ESP", "Effects around the entity you attack or aim at", Category.ESP);
        color1.visibleWhen(() -> colorMode.getIndex() == 0 || colorMode.getIndex() == 2);
        color2.visibleWhen(() -> colorMode.getIndex() == 2);

    }

    @Override
    public void onAttack(Entity entity, Vec3d pos) {
        if (source.is("Crosshair")) return;
        if (valid(entity)) setTarget((LivingEntity) entity);
    }

    @Override
    protected void onDisable() {
        target = null;
        fadeVal = 0f;
    }

    // ================= nishon tanlash =================
    private boolean valid(Entity e) {
        if (!(e instanceof LivingEntity le) || e == mc.player || e.isRemoved() || !le.isAlive()) return false;
        if (e instanceof PlayerEntity) return targets.get("Players");
        if (e instanceof Monster) return targets.get("Mobs");
        return targets.get("Animals");
    }

    private void setTarget(LivingEntity e) {
        if (target != e) {
            target = e;
            snap = true;
        }
        lastSeen = System.nanoTime();
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            target = null;
            fadeVal = 0f;
            return;
        }
        if (!source.is("Attack")) {
            Entity c = mc.targetedEntity;
            if (c != null && valid(c)) setTarget((LivingEntity) c);
        }
    }

    // ================= asosiy chizish =================
    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (mc.player == null || mc.world == null) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;
        if (target == null && fadeVal <= 0f) return;

        boolean valid = target != null && valid(target) && target.distanceTo(mc.player) <= maxDist.getF();
        boolean active = valid && (now - lastSeen) / 1.0E9 <= keepTime.get();

        if (valid) {
            float k = 1f - (float) Math.exp(-dt * 16.0);
            if (snap) {
                sx = target.getX();
                sy = target.getY();
                sz = target.getZ();
                hpF = target.getHealth() / Math.max(1f, target.getMaxHealth());
                snap = false;
            } else {
                sx += (target.getX() - sx) * k;
                sy += (target.getY() - sy) * k;
                sz += (target.getZ() - sz) * k;
                hpF += (target.getHealth() / Math.max(1f, target.getMaxHealth()) - hpF) * k;
            }
            tw = target.getWidth();
            th = target.getHeight();
        }

        fadeVal += (active ? 1f : -1f) * dt / Math.max(0.05f, fadeTime.getF());
        fadeVal = Math.max(0f, Math.min(1f, fadeVal));
        if (fadeVal <= 0.003f) {
            if (!active) target = null;
            return;
        }

        float ease = Easing.easeOutCubic(fadeVal);
        camPos = cam;
        fx = sx;
        fy = sy;
        fz = sz;
        mx = fx;
        my = fy + th * 0.5;
        mz = fz;
        sc = size.get() * (0.7 + 0.3 * ease);
        tm = ((now - t0) / 1.0E9) * speed.get();
        lw = 0.011 * width.get();
        hw = 0.02 * width.get();
        curAlpha = alpha.getF() * ease;
        curMul = 1f;
        taper = false;
        colorModeIdx = colorMode.getIndex();

        if (!Render3D.begin(stack, cam, throughWalls.get())) return;
        try {
            switch (mode.getIndex()) {
                case 0 -> drawRing();
                case 1 -> drawCircle();
                case 2 -> drawBox();
                case 3 -> drawOutlineBox();
                case 4 -> drawCube();
                case 5 -> drawOrbs();
                case 6 -> drawCrystals();
                case 7 -> drawRotatingRings();
                case 8 -> drawHalo();
                case 9 -> drawPulseSphere();
                case 10 -> drawTracer();
                case 11 -> drawArrow();
                case 12 -> drawPentagram();
                case 13 -> drawChains();
                default -> drawSakura();
            }
        } finally {
            Render3D.end();
        }
    }

    // ================= rang =================
    private static int opaque(int c) { return ColorUtils.withAlpha(c, 255); }

    private int colorAt(float f) {
        int c;
        float m = 0.5f + 0.5f * (float) Math.sin(TAU * f + tm * 2.0);
        switch (colorModeIdx) {
            case 1 -> c = ColorUtils.hsb(f + (float) (tm * 0.15), 0.85f, 1f, 1f);
            case 2 -> c = ColorUtils.lerp(opaque(color1.getColor((int) (f * 1500))), opaque(color2.getColor((int) (f * 1500))), m);
            case 3 -> c = ColorUtils.health(hpF, 1f);
            case 4 -> c = ColorUtils.lerp(ColorUtils.RED, ColorUtils.GREEN, m);
            default -> c = opaque(color1.getColor((int) (f * 1500)));
        }
        float al = curAlpha * curMul * (taper ? (0.12f + 0.88f * f) : 1f);
        return ColorUtils.withAlpha(c, Math.round(Math.max(0f, Math.min(1f, al)) * 255f));
    }

    private void ptsCircle(double r, int n, double y, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + TAU * i / n;
            Render3D.X[i] = mx + Math.cos(a) * r;
            Render3D.Y[i] = y;
            Render3D.Z[i] = mz + Math.sin(a) * r;
        }
    }

    // ================= 15 ta rejim =================

    /** 1. Ring: tana bo'ylab yuqoriga-pastga yuruvchi aylanuvchi kamar (iz bilan). */
    private void drawRing() {
        double rr = tw * 0.5 + 0.30 * sc;
        double hh = 0.055 * sc * (width.get() / 1.5);
        for (int g = 0; g < 4; g++) {
            double ph = 0.5 + 0.5 * Math.sin(tm * 1.8 - g * 0.35);
            double yy = fy + 0.06 + (th - 0.12) * ph;
            curMul = 1f - 0.27f * g;
            Render3D.belt(mx, yy, mz, rr, hh * (1.0 - 0.12 * g), 64, tm * 1.2, TAU, cf, 0f);
        }
        double ph0 = 0.5 + 0.5 * Math.sin(tm * 1.8);
        curMul = 0.28f;
        Render3D.belt(mx, fy + 0.06 + (th - 0.12) * ph0, mz, rr, hh * 3.0, 64, tm * 1.2, TAU, cf, 0f);
        curMul = 1f;
    }

    /** 2. Circle: yerdagi to'ldirilgan doira, halqa va aylanuvchi yoylar. */
    private void drawCircle() {
        double rr = tw * 0.5 + 0.45 * sc, y = fy + 0.03;
        Render3D.discFn(mx, y, mz, rr, 48, cf, 0.05f, 0.35f);
        ptsCircle(rr, 72, y + 0.005, tm * 0.5);
        Render3D.ribbon(72, true, hw * 1.2, 1f, cf);
        taper = true;
        for (int arc = 0; arc < 3; arc++) {
            for (int i = 0; i < 24; i++) {
                float f = i / 23f;
                double a = tm * 1.6 + arc * TAU / 3 + f * 1.1;
                Render3D.X[i] = mx + Math.cos(a) * rr * 0.8;
                Render3D.Y[i] = y + 0.008;
                Render3D.Z[i] = mz + Math.sin(a) * rr * 0.8;
            }
            Render3D.ribbon(24, false, hw * 0.9, 1f, cf);
        }
        taper = false;
    }

    /** 3. Box: yarim shaffof to'ldirilgan quti + ingichka qirralar. */
    private void drawBox() {
        double hx = tw * 0.5 + 0.06;
        curMul = 0.2f;
        int cb = colorAt(0f), ct = colorAt(1f);
        Render3D.box(mx - hx, fy - 0.02, mz - hx, mx + hx, fy + th + 0.06, mz + hx, cb, ct);
        curMul = 1f;
        edges(mx - hx, fy - 0.02, mz - hx, mx + hx, fy + th + 0.06, mz + hx, lw * 0.7);
    }

    /** 4. Outline Box: faqat qalin qirralar. */
    private void drawOutlineBox() {
        double hx = tw * 0.5 + 0.06;
        edges(mx - hx, fy - 0.02, mz - hx, mx + hx, fy + th + 0.06, mz + hx, lw * 1.4);
    }

    private void edges(double x1, double y1, double z1, double x2, double y2, double z2, double halfW) {
        int cb = colorAt(0f), ct = colorAt(1f);
        Render3D.line(x1, y1, z1, cb, x2, y1, z1, cb, halfW, 1f);
        Render3D.line(x2, y1, z1, cb, x2, y1, z2, cb, halfW, 1f);
        Render3D.line(x2, y1, z2, cb, x1, y1, z2, cb, halfW, 1f);
        Render3D.line(x1, y1, z2, cb, x1, y1, z1, cb, halfW, 1f);
        Render3D.line(x1, y2, z1, ct, x2, y2, z1, ct, halfW, 1f);
        Render3D.line(x2, y2, z1, ct, x2, y2, z2, ct, halfW, 1f);
        Render3D.line(x2, y2, z2, ct, x1, y2, z2, ct, halfW, 1f);
        Render3D.line(x1, y2, z2, ct, x1, y2, z1, ct, halfW, 1f);
        Render3D.line(x1, y1, z1, cb, x1, y2, z1, ct, halfW, 1f);
        Render3D.line(x2, y1, z1, cb, x2, y2, z1, ct, halfW, 1f);
        Render3D.line(x2, y1, z2, cb, x2, y2, z2, ct, halfW, 1f);
        Render3D.line(x1, y1, z2, cb, x1, y2, z2, ct, halfW, 1f);
    }

    /** 5. Cube: tanani o'rab aylanuvchi sim-kub. */
    private void drawCube() {
        double half = (Math.max(tw * 0.5, th * 0.35) + 0.12) * sc;
        curMul = 0.12f;
        int face = colorAt(0.5f);
        curMul = 1f;
        Render3D.cube(mx, my, mz, half, tm * 0.9, 0.35 * Math.sin(tm * 0.6), lw * 1.2, face, cf);
    }

    /** 6. Orbiting Orbs: tana atrofida aylanuvchi nurli sharlar. */
    private void drawOrbs() {
        int n = 5;
        double rr = tw * 0.5 + 0.4 * sc;
        for (int i = 0; i < n; i++) {
            double a = tm * 1.6 + i * TAU / n;
            double x = mx + Math.cos(a) * rr, z = mz + Math.sin(a) * rr;
            double y = fy + th * (0.5 + 0.4 * Math.sin(tm * 1.1 + i * 1.3));
            constF = i / (float) n;
            curMul = 1f;
            Render3D.sphere(x, y, z, 0.07 * sc, 5, 8, constCf, 1f);
            curMul = 0.18f;
            Render3D.sphere(x, y, z, 0.07 * sc * 2.4, 5, 8, constCf, 0.3f);
        }
        curMul = 1f;
    }

    /** 7. Crystals: tebranib aylanuvchi kristallar. */
    private void drawCrystals() {
        int n = 6;
        double rr = tw * 0.5 + 0.5 * sc;
        for (int i = 0; i < n; i++) {
            double a = tm * 0.9 + i * TAU / n;
            double x = mx + Math.cos(a) * rr, z = mz + Math.sin(a) * rr;
            double y = my + 0.25 * sc * Math.sin(tm * 1.5 + i * 1.1);
            constF = i / (float) n;
            curMul = 1f;
            int top = colorAt(constF);
            curMul = 0.75f;
            int mid = colorAt(constF);
            curMul = 0.45f;
            int bot = colorAt(constF);
            Render3D.crystal(x, y, z, 0.09 * sc, 0.22 * sc, tm * 2.0 + i, top, mid, bot);
        }
        curMul = 1f;
    }

    /** 8. Rotating Rings: giroskop kabi 3 ta egilgan aylanuvchi halqa. */
    private void drawRotatingRings() {
        double rad = (Math.max(tw * 0.5, th * 0.35) + 0.25) * sc;
        for (int k = 0; k < 3; k++) {
            double phi = tm * (0.9 + 0.35 * k) + k * 1.3;
            double theta = 0.55 + 0.6 * k + 0.25 * Math.sin(tm * 0.7 + k);
            double cph = Math.cos(phi), sph = Math.sin(phi), cth = Math.cos(theta), sth = Math.sin(theta);
            Render3D.ellipse3(mx, my, mz, cph, 0, -sph, cth * sph, -sth, cth * cph,
                    rad, rad, 56, tm * 1.5 * (k % 2 == 0 ? 1 : -1), TAU, lw * 1.4, 1f, cf);
        }
    }

    /** 9. Halo: bosh ustidagi aylanuvchi nurli halqa. */
    private void drawHalo() {
        double yy = fy + th + 0.22 * sc + 0.04 * Math.sin(tm * 2.0);
        double rr = Math.max(0.22, tw * 0.45) * sc;
        double hh = 0.03 * (width.get() / 1.5);
        Render3D.belt(mx, yy, mz, rr, hh, 48, tm * 1.5, TAU, cf, 0f);
        curMul = 0.3f;
        Render3D.belt(mx, yy, mz, rr, hh * 3.2, 48, tm * 1.5, TAU, cf, 0f);
        curMul = 1f;
        for (int i = 0; i < 3; i++) {
            double a = -tm * 2.2 + i * TAU / 3;
            constF = i / 3f;
            Render3D.sphere(mx + Math.cos(a) * rr, yy, mz + Math.sin(a) * rr, 0.028 * sc, 4, 6, constCf, 1f);
        }
    }

    /** 10. Pulse Sphere: tanani o'rab nafas oluvchi shaffof shar. */
    private void drawPulseSphere() {
        double base = (Math.max(tw, th * 0.6) * 0.62 + 0.1) * sc;
        double r = base * (1.0 + 0.12 * Math.sin(tm * 3.0));
        curMul = 0.28f;
        Render3D.sphere(mx, my, mz, r, 10, 20, cf, 0.35f);
        curMul = 0.9f;
        Render3D.belt(mx, my, mz, r, 0.03 * (width.get() / 1.5), 56, tm, TAU, cf, 0f);
        curMul = 1f;
    }

    /** 11. Tracer: kameradan nishongacha gradient chiziq. */
    private void drawTracer() {
        Vec3d look = mc.player.getRotationVec(1.0f);
        double x0 = camPos.x + look.x * 0.9, y0 = camPos.y + look.y * 0.9 - 0.15, z0 = camPos.z + look.z * 0.9;
        int c1 = colorAt(0f), c2 = colorAt(1f);
        Render3D.line(x0, y0, z0, c1, mx, my, mz, c2, lw * 1.3, 1f);
        curMul = 0.3f;
        int g1 = colorAt(0f), g2 = colorAt(1f);
        Render3D.line(x0, y0, z0, g1, mx, my, mz, g2, lw * 4.5, 0f);
        curMul = 1f;
        constF = 1f;
        Render3D.sphere(mx, my, mz, 0.08 * sc, 5, 8, constCf, 1f);
    }

    /** 12. Arrow: bosh ustida tebranuvchi pastga qaragan piramida. */
    private void drawArrow() {
        double ay = fy + th + 0.12 * sc + 0.1 * sc * Math.sin(tm * 3.0);
        double base = ay + 0.45 * sc;
        curMul = 1f;
        int ca = colorAt(0f);
        curMul = 0.75f;
        int cb = colorAt(0.5f);
        curMul = 1f;
        Render3D.pyramid(mx, ay, mz, base, 0.17 * sc, 5, tm * 2.0, ca, cb);
    }

    /** 13. Pentagram: yerdagi aylanuvchi besh uchli yulduz va aylana. */
    private void drawPentagram() {
        double rr = tw * 0.5 + 0.6 * sc, y = fy + 0.03, rot = tm * 0.6;
        ptsCircle(rr, 72, y, rot);
        Render3D.ribbon(72, true, hw * 0.9, 1f, cf);
        for (int k = 0; k < 5; k++) {
            double a0 = rot + k * TAU / 5, a1 = rot + ((k + 2) % 5) * TAU / 5;
            Render3D.X[0] = mx + Math.cos(a0) * rr;
            Render3D.Y[0] = y;
            Render3D.Z[0] = mz + Math.sin(a0) * rr;
            Render3D.X[1] = mx + Math.cos(a1) * rr;
            Render3D.Y[1] = y;
            Render3D.Z[1] = mz + Math.sin(a1) * rr;
            Render3D.ribbon(2, false, hw * 0.8, 1f, cf);
        }
        for (int k = 0; k < 5; k++) {
            double a = rot + k * TAU / 5;
            constF = k / 5f;
            int c = colorAt(constF);
            Render3D.disc(mx + Math.cos(a) * rr, y + 0.01, mz + Math.sin(a) * rr, 0.05 * sc, 8, c, ColorUtils.mulAlpha(c, 0.4f));
        }
    }

    /** 14. Chains: bellar atrofida qarama-qarshi aylanuvchi ikki zanjir. */
    private void drawChains() {
        int n = 22;
        double rr = tw * 0.5 + 0.22 * sc;
        double len = 0.09 * sc, wid = 0.045 * sc;
        for (int ring = 0; ring < 2; ring++) {
            double yy = fy + th * (ring == 0 ? 0.3 : 0.72);
            double dir = ring == 0 ? 1.0 : -1.0;
            for (int i = 0; i < n; i++) {
                double a = dir * tm * 0.7 + i * TAU / n;
                double ca = Math.cos(a), sa = Math.sin(a);
                double lx = mx + ca * rr, lz = mz + sa * rr;
                constF = i / (float) n;
                double vx, vy, vz;
                if (i % 2 == 0) { vx = 0; vy = 1; vz = 0; }
                else { vx = ca; vy = 0; vz = sa; }
                Render3D.ellipse3(lx, yy, lz, -sa, 0, ca, vx, vy, vz, len, wid, 8, 0, TAU, lw * 0.9, 1f, constCf);
            }
        }
    }

    /** 15. Sakura: tushib turuvchi va aylanuvchi gul barglari. */
    private void drawSakura() {
        int n = 16;
        for (int i = 0; i < n; i++) {
            double p = (i * 0.618) % 1.0;
            double g = (tm * 0.22 + p) % 1.0;
            double yy = fy + (th + 0.3) * (1.0 - g);
            double a = tm * 0.7 * (1.0 + 0.2 * Math.sin(i)) + i * 2.399;
            double rho = tw * 0.5 + 0.25 * sc + 0.25 * sc * Math.sin(g * TAU * 2 + i);
            constF = i / (float) n;
            curMul = (float) Math.sin(g * Math.PI);
            int c = colorAt(constF);
            Render3D.petal(mx + Math.cos(a) * rho, yy, mz + Math.sin(a) * rho, 0.075 * sc,
                    a * 2.0 + tm * 2.0, 0.8 * Math.sin(tm * 3.0 + i), c);
        }
        curMul = 1f;
    }
}

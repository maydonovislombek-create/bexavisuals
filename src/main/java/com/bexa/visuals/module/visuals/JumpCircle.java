package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.render.Render3D;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Easing;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Random;

/** Sakrash paytida oyoq ostida kengayib so'nadigan effekt: 20 ta model, 7 ta rang rejimi. */
public class JumpCircle extends Module {
    private static final String[] MODELS = {
            "Flat Ring", "Filled Circle", "Double Ring", "Triple Ring", "Star", "Hexagon", "Square",
            "Spiral", "Sakura", "Spikes", "Dots Orbit", "Lightning Ring", "Heart", "Diamond", "Cross",
            "Vortex", "Sphere", "Rotating Ring", "Dashed Ring", "Wave"
    };
    private static final String[] COLOR_MODES = {"Static", "Rainbow", "Gradient", "Theme", "Pulse", "Health", "Random"};
    private static final double TAU = Math.PI * 2;
    private static final int MAX_EFFECTS = 16;

    private final ModeSetting model = add(new ModeSetting("Model", "Flat Ring", MODELS));
    private final ModeSetting colorMode = add(new ModeSetting("Color Mode", "Theme", COLOR_MODES));
    private final ColorSetting color1 = add(new ColorSetting("Color", ColorUtils.RED));
    private final ColorSetting color2 = add(new ColorSetting("Color 2", ColorUtils.GREEN));
    private final NumberSetting radius = add(new NumberSetting("Radius", 2.2, 0.5, 6.0, 0.1));
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.3, 3.0, 0.1));
    private final NumberSetting lifetime = add(new NumberSetting("Lifetime", 1.1, 0.3, 4.0, 0.1));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 0.14, 0.03, 0.6, 0.01));
    private final NumberSetting rotSpeed = add(new NumberSetting("Rotation Speed", 1.5, 0.0, 8.0, 0.1));
    private final NumberSetting alpha = add(new NumberSetting("Alpha", 0.85, 0.1, 1.0, 0.05));
    private final NumberSetting glow = add(new NumberSetting("Glow", 0.5, 0.0, 1.0, 0.05));
    private final NumberSetting heightOffset = add(new NumberSetting("Height Offset", 0.03, 0.0, 1.5, 0.01));
    private final ModeSetting fade = add(new ModeSetting("Fade Mode", "Fade Out", "Fade Out", "Shrink", "Fade + Shrink", "None"));
    private final BoolSetting onJump = add(new BoolSetting("On Jump", true));
    private final BoolSetting onLand = add(new BoolSetting("On Landing", false));
    private final BoolSetting throughWalls = add(new BoolSetting("Through Walls", false));

    private static class Effect {
        double x, y, z;
        long start;
        int baseColor;
        float rot0;
        int seed;
    }

    private final ArrayList<Effect> effects = new ArrayList<>();
    private final Random rnd = new Random();
    private boolean wasOnGround = true;
    private int airTicks;

    // ---- kadr davomidagi holat (yangi obyekt yaratmaslik uchun maydonlar) ----
    private Effect eff;
    private float age, tt, curAlpha, curMul = 1f, constF;
    private boolean taper;
    private int colorModeIdx;
    private double cx, cy, cz, R, hw, rot;

    private final Render3D.ColorFn cf = this::colorAt;
    private final Render3D.ColorFn constCf = f -> colorAt(constF);

    public JumpCircle() {
        super("Jump Circle", "Expanding circle under your feet when you jump", Category.VISUALS);
        color1.visibleWhen(() -> {
            int i = colorMode.getIndex();
            return i == 0 || i == 2 || i == 4;
        });
        color2.visibleWhen(() -> colorMode.getIndex() == 2);
    }

    @Override
    protected void onEnable() {
        wasOnGround = true;
        airTicks = 0;
    }

    @Override
    protected void onDisable() {
        effects.clear();
    }

    // ================= sakrash aniqlash =================
    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;
        boolean ground = p.isOnGround();
        if (ground) {
            if (!wasOnGround && onLand.get() && airTicks >= 4) spawn(p);
            airTicks = 0;
        } else {
            airTicks++;
            if (wasOnGround && onJump.get() && p.getVelocity().y > 0.1
                    && !p.isTouchingWater() && !p.getAbilities().flying && !p.hasVehicle() && !p.isClimbing()) {
                spawn(p);
            }
        }
        wasOnGround = ground;
    }

    private void spawn(ClientPlayerEntity p) {
        Effect e = new Effect();
        e.x = p.getX();
        e.y = p.getY();
        e.z = p.getZ();
        e.start = System.nanoTime();
        e.rot0 = rnd.nextFloat() * (float) TAU;
        e.seed = rnd.nextInt();
        int mode = colorMode.getIndex();
        if (mode == 5) e.baseColor = ColorUtils.health(p.getHealth() / Math.max(1f, p.getMaxHealth()), 1f);
        else e.baseColor = ColorUtils.random(rnd, 1f);
        if (effects.size() >= MAX_EFFECTS) effects.remove(0);
        effects.add(e);
    }

    // ================= chizish =================
    @Override
    public void onWorldRender(MatrixStack stack, Vec3d cam) {
        if (effects.isEmpty()) return;
        if (!Render3D.begin(stack, cam, throughWalls.get())) return;
        try {
            long now = System.nanoTime();
            float life = lifetime.getF();
            colorModeIdx = colorMode.getIndex();
            for (int i = effects.size() - 1; i >= 0; i--) {
                Effect e = effects.get(i);
                float a = (now - e.start) / 1.0E9f;
                if (a >= life) {
                    effects.remove(i);
                    continue;
                }
                drawEffect(e, a, life);
            }
        } finally {
            Render3D.end();
        }
    }

    private void drawEffect(Effect e, float ageSec, float life) {
        eff = e;
        age = ageSec;
        tt = ageSec / life;

        float expand = Easing.easeOutCubic(Math.min(1f, tt * speed.getF()));
        R = Math.max(0.05, radius.get() * expand);

        String fm = fade.get();
        boolean fadeOut = fm.equals("Fade Out") || fm.equals("Fade + Shrink");
        boolean shrink = fm.equals("Shrink") || fm.equals("Fade + Shrink");
        float fadeMul = fadeOut ? (float) Math.pow(1f - tt, 1.3) : (tt > 0.9f ? (1f - tt) * 10f : 1f);
        float fadeIn = Math.min(1f, tt / 0.06f);
        curAlpha = alpha.getF() * fadeIn * Math.max(0f, fadeMul);
        if (curAlpha <= 0.002f) return;

        double th = thickness.get() * (shrink ? Math.max(0.1, 1.0 - Easing.easeInCubic(tt)) : 1.0);
        hw = Math.max(0.004, th * 0.5);
        rot = e.rot0 + age * rotSpeed.get();
        cx = e.x;
        cy = e.y + heightOffset.get();
        cz = e.z;
        taper = false;
        curMul = 1f;

        // umumiy yumshoq nur halqasi
        if (glow.getF() > 0.02f) {
            curMul = glow.getF() * 0.35f;
            circlePts(R, 72, 0, rot);
            Render3D.ribbon(72, true, hw * 3.5 + 0.04, 0f, cf);
            curMul = 1f;
        }

        switch (model.getIndex()) {
            case 0 -> ringAt(R, hw, 1f, 1f, 0);
            case 1 -> modelFilled();
            case 2 -> { ringAt(R, hw, 1f, 1f, 0); ringAt(R * 0.7, hw * 0.8, 1f, 0.8f, 0.004); }
            case 3 -> { ringAt(R, hw, 1f, 1f, 0); ringAt(R * 0.78, hw * 0.8, 1f, 0.8f, 0.004); ringAt(R * 0.56, hw * 0.65, 1f, 0.6f, 0.008); }
            case 4 -> modelStar();
            case 5 -> modelPolygon(6, rot * 0.4);
            case 6 -> modelPolygon(4, rot * 0.4 + Math.PI / 4);
            case 7 -> modelSpiral();
            case 8 -> modelSakura();
            case 9 -> modelSpikes();
            case 10 -> modelDots();
            case 11 -> modelLightning();
            case 12 -> modelHeart();
            case 13 -> modelDiamond();
            case 14 -> modelCross();
            case 15 -> modelVortex();
            case 16 -> modelSphere();
            case 17 -> modelRotatingRing();
            case 18 -> modelDashed();
            default -> modelWave();
        }
    }

    // ================= rang =================
    private static int opaque(int c) { return ColorUtils.withAlpha(c, 255); }

    private int colorAt(float f) {
        int c;
        float m = 0.5f + 0.5f * (float) Math.sin(TAU * f + age * 3f);
        switch (colorModeIdx) {
            case 1 -> c = ColorUtils.hsb(f + age * 0.25f, 0.85f, 1f, 1f);
            case 2 -> c = ColorUtils.lerp(opaque(color1.getColor((int) (f * 1500))), opaque(color2.getColor((int) (f * 1500))), m);
            case 3 -> c = ColorUtils.lerp(ColorUtils.RED, ColorUtils.GREEN, m);
            case 4 -> {
                float pulse = 0.5f + 0.5f * (float) Math.sin(age * 9f);
                c = ColorUtils.lerp(opaque(color1.getColor((int) (f * 1500))), ColorUtils.WHITE, pulse * 0.5f);
            }
            case 5, 6 -> c = eff.baseColor;
            default -> c = opaque(color1.getColor((int) (f * 1500)));
        }
        float al = curAlpha * curMul * (taper ? (0.12f + 0.88f * f) : 1f);
        return ColorUtils.withAlpha(c, Math.round(Math.max(0f, Math.min(1f, al)) * 255f));
    }

    // ================= geometriya yordamchilari =================
    private void circlePts(double radius, int n, double yOff, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + TAU * i / n;
            Render3D.X[i] = cx + Math.cos(a) * radius;
            Render3D.Y[i] = cy + yOff;
            Render3D.Z[i] = cz + Math.sin(a) * radius;
        }
    }

    private void polyPts(int n, double radius, double rotation) {
        for (int i = 0; i < n; i++) {
            double a = rotation + TAU * i / n;
            Render3D.X[i] = cx + Math.cos(a) * radius;
            Render3D.Y[i] = cy;
            Render3D.Z[i] = cz + Math.sin(a) * radius;
        }
    }

    /** Lokal (radial, tangensial) nuqtani dunyoga aylantirib massivga yozadi. */
    private void localPt(int i, double lx, double lz, double angle, double yOff) {
        double c = Math.cos(angle), s = Math.sin(angle);
        Render3D.X[i] = cx + lx * c - lz * s;
        Render3D.Y[i] = cy + yOff;
        Render3D.Z[i] = cz + lx * s + lz * c;
    }

    private void ringAt(double radius, double halfW, float soft, float mul, double yOff) {
        taper = false;
        curMul = mul;
        circlePts(radius, 72, yOff, rot);
        Render3D.ribbon(72, true, halfW, soft, cf);
        curMul = 1f;
    }

    private void outline(int n, double halfW, float mul) {
        taper = false;
        curMul = mul;
        Render3D.ribbon(n, true, halfW, 1f, cf);
        curMul = 1f;
    }

    private static float rand01(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    // ================= 20 ta model =================
    private void modelFilled() {
        taper = false;
        Render3D.discFn(cx, cy, cz, R, 48, cf, 0.08f, 0.5f);
        ringAt(R, hw * 0.7, 1f, 1f, 0.002);
    }

    private void modelStar() {
        int n = 10;
        for (int i = 0; i < n; i++) {
            double rr = (i % 2 == 0) ? R : R * 0.42;
            double a = rot + i * Math.PI / 5;
            Render3D.X[i] = cx + Math.cos(a) * rr;
            Render3D.Y[i] = cy;
            Render3D.Z[i] = cz + Math.sin(a) * rr;
        }
        taper = false;
        curMul = 1f;
        Render3D.fan(n, cx, cy, cz, cf, 0.08f, 0.35f);
        outline(n, hw * 0.9, 1f);
    }

    private void modelPolygon(int n, double rotation) {
        polyPts(n, R, rotation);
        taper = false;
        Render3D.fan(n, cx, cy, cz, cf, 0.06f, 0.3f);
        outline(n, hw, 1f);
    }

    private void modelSpiral() {
        int n = 120;
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1);
            double a = rot * 1.2 + f * 2.4 * TAU;
            double r = R * f;
            Render3D.X[i] = cx + Math.cos(a) * r;
            Render3D.Y[i] = cy;
            Render3D.Z[i] = cz + Math.sin(a) * r;
        }
        taper = true;
        Render3D.ribbon(n, false, hw * 0.8, 1f, cf);
        taper = false;
    }

    private void modelSakura() {
        int petals = 6;
        double d = R * 0.72, len = R * 0.30, wid = R * 0.16;
        for (int k = 0; k < petals; k++) {
            double ak = rot * 0.5 + k * TAU / petals;
            constF = k / (float) petals;
            localPt(0, d - len, 0, ak, 0);
            localPt(1, d - len * 0.2, wid, ak, 0);
            localPt(2, d + len * 0.6, wid * 0.55, ak, 0);
            localPt(3, d + len, 0, ak, 0);
            localPt(4, d + len * 0.6, -wid * 0.55, ak, 0);
            localPt(5, d - len * 0.2, -wid, ak, 0);
            double pxc = cx + Math.cos(ak) * d, pzc = cz + Math.sin(ak) * d;
            taper = false;
            curMul = 1f;
            Render3D.fan(6, pxc, cy, pzc, constCf, 0.9f, 0.45f);
            Render3D.ribbon(6, true, hw * 0.3, 1f, constCf);
        }
    }

    private void modelSpikes() {
        int cnt = 18;
        double h = (0.25 + R * 0.2) * (1.0 - 0.4 * tt);
        double da = Math.PI / cnt * 0.55;
        ringAt(R, hw * 0.7, 1f, 1f, 0);
        taper = false;
        for (int k = 0; k < cnt; k++) {
            double a = rot * 0.3 + k * TAU / cnt;
            float f = k / (float) cnt;
            curMul = 0.85f;
            int cb = colorAt(f);
            curMul = 0.12f;
            int ct = colorAt(f);
            Render3D.tri(
                    cx + Math.cos(a - da) * R, cy, cz + Math.sin(a - da) * R, cb,
                    cx + Math.cos(a + da) * R, cy, cz + Math.sin(a + da) * R, cb,
                    cx + Math.cos(a) * R, cy + h, cz + Math.sin(a) * R, ct);
        }
        curMul = 1f;
    }

    private void modelDots() {
        int count = 12;
        ringAt(R, hw * 0.25, 0f, 0.4f, 0);
        taper = false;
        for (int i = 0; i < count; i++) {
            double a = rot * 1.4 + i * TAU / count;
            double px = cx + Math.cos(a) * R, pz = cz + Math.sin(a) * R;
            double py = cy + 0.03 + 0.04 * Math.sin(age * 6 + i);
            float f = i / (float) count;
            curMul = 1f;
            int col = colorAt(f);
            Render3D.disc(px, py, pz, hw * 1.3 + 0.035, 10, col, ColorUtils.mulAlpha(col, 0.55f));
            curMul = 0.3f;
            int gl = colorAt(f);
            Render3D.disc(px, py - 0.001, pz, hw * 3.0 + 0.09, 10, gl, ColorUtils.mulAlpha(gl, 0f));
        }
        curMul = 1f;
    }

    private void modelLightning() {
        int n = 56;
        int frame = (int) (age * 18f);
        for (int i = 0; i < n; i++) {
            double a = rot * 0.3 + TAU * i / n;
            double jr = R * (1.0 + (rand01(eff.seed, frame * 131 + i) - 0.5) * 0.28);
            Render3D.X[i] = cx + Math.cos(a) * jr;
            Render3D.Y[i] = cy + (rand01(eff.seed + 7, frame * 17 + i) - 0.5) * 0.05;
            Render3D.Z[i] = cz + Math.sin(a) * jr;
        }
        taper = false;
        curMul = 0.35f;
        Render3D.ribbon(n, true, hw * 2.5, 0f, cf);
        curMul = 1f;
        Render3D.ribbon(n, true, hw * 0.6, 1f, cf);
    }

    private void modelHeart() {
        int n = 100;
        double s = R / 17.0;
        double th = eff.rot0 + rot * 0.25;
        double c = Math.cos(th), sn = Math.sin(th);
        for (int i = 0; i < n; i++) {
            double p = TAU * i / n;
            double hx = 16 * Math.pow(Math.sin(p), 3);
            double hz = -(13 * Math.cos(p) - 5 * Math.cos(2 * p) - 2 * Math.cos(3 * p) - Math.cos(4 * p));
            Render3D.X[i] = cx + (hx * c - hz * sn) * s;
            Render3D.Y[i] = cy;
            Render3D.Z[i] = cz + (hx * sn + hz * c) * s;
        }
        taper = false;
        Render3D.fan(n, cx, cy, cz, cf, 0.08f, 0.3f);
        outline(n, hw * 0.9, 1f);
    }

    private void modelDiamond() {
        double ang = rot * 0.5;
        for (int pass = 0; pass < 2; pass++) {
            double sc = pass == 0 ? 1.0 : 0.55;
            localPt(0, R * sc, 0, ang, 0);
            localPt(1, 0, R * 0.6 * sc, ang, 0);
            localPt(2, -R * sc, 0, ang, 0);
            localPt(3, 0, -R * 0.6 * sc, ang, 0);
            taper = false;
            if (pass == 0) Render3D.fan(4, cx, cy, cz, cf, 0.08f, 0.3f);
            outline(4, hw * (pass == 0 ? 1.0 : 0.7), pass == 0 ? 1f : 0.6f);
        }
    }

    private void modelCross() {
        double w = R * 0.3;
        double[][] p = {
                {-w, R}, {w, R}, {w, w}, {R, w}, {R, -w}, {w, -w},
                {w, -R}, {-w, -R}, {-w, -w}, {-R, -w}, {-R, w}, {-w, w}
        };
        double ang = rot * 0.5;
        for (int i = 0; i < p.length; i++) localPt(i, p[i][0], p[i][1], ang, 0);
        taper = false;
        Render3D.fan(12, cx, cy, cz, cf, 0.08f, 0.3f);
        outline(12, hw * 0.9, 1f);
    }

    private void modelVortex() {
        int arms = 4, n = 64;
        taper = true;
        curMul = 0.95f;
        for (int arm = 0; arm < arms; arm++) {
            for (int i = 0; i < n; i++) {
                float f = i / (float) (n - 1);
                double a = rot * 2.0 + arm * Math.PI / 2 + f * 1.9 * Math.PI;
                double r = R * f;
                Render3D.X[i] = cx + Math.cos(a) * r;
                Render3D.Y[i] = cy;
                Render3D.Z[i] = cz + Math.sin(a) * r;
            }
            Render3D.ribbon(n, false, hw * 0.8, 1f, cf);
        }
        taper = false;
        curMul = 1f;
    }

    private void modelSphere() {
        taper = false;
        for (int j = 0; j < 7; j++) {
            double lat = (j / 7.0) * (Math.PI / 2) * 0.93;
            double rad = R * Math.cos(lat);
            double yy = R * Math.sin(lat) * 0.9;
            curMul = 1f - 0.11f * j;
            circlePts(rad, 56, yy, rot * 0.5);
            Render3D.ribbon(56, true, hw * 0.6, 1f, cf);
        }
        curMul = 1f;
    }

    private void modelRotatingRing() {
        ringAt(R, hw * 0.35, 1f, 0.22f, 0);
        int n = 48;
        for (int i = 0; i < n; i++) {
            float f = i / (float) (n - 1);
            double a = rot * 2.2 + f * 1.6 * Math.PI;
            Render3D.X[i] = cx + Math.cos(a) * R;
            Render3D.Y[i] = cy + 0.004;
            Render3D.Z[i] = cz + Math.sin(a) * R;
        }
        taper = true;
        curMul = 1f;
        Render3D.ribbon(n, false, hw, 1f, cf);
        taper = false;
    }

    private void modelDashed() {
        int count = 14, n = 6;
        taper = false;
        curMul = 1f;
        for (int k = 0; k < count; k++) {
            constF = k / (float) count;
            double start = rot * 0.8 + k * TAU / count;
            double sweep = TAU / count * 0.55;
            for (int i = 0; i < n; i++) {
                double a = start + sweep * i / (n - 1);
                Render3D.X[i] = cx + Math.cos(a) * R;
                Render3D.Y[i] = cy;
                Render3D.Z[i] = cz + Math.sin(a) * R;
            }
            Render3D.ribbon(n, false, hw, 1f, constCf);
        }
    }

    private void modelWave() {
        int n = 80;
        taper = false;
        for (int w = 0; w < 3; w++) {
            double rr = R * (1.0 - 0.22 * w);
            if (rr < 0.05) continue;
            for (int i = 0; i < n; i++) {
                double a = rot * 0.5 + TAU * i / n;
                double wiggle = Math.sin(a * 6 + age * 8 + w * 1.7) * (0.06 + 0.025 * rr) * (1.0 - tt);
                Render3D.X[i] = cx + Math.cos(a) * rr;
                Render3D.Y[i] = cy + wiggle;
                Render3D.Z[i] = cz + Math.sin(a) * rr;
            }
            curMul = 1f - 0.28f * w;
            Render3D.ribbon(n, true, hw * (1.0 - 0.15 * w), 1f, cf);
        }
        curMul = 1f;
    }
}

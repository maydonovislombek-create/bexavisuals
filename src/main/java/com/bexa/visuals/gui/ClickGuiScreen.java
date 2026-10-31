package com.bexa.visuals.gui;

import com.bexa.visuals.BexaVisualsClient;
import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.*;
import com.bexa.visuals.util.Animation;
import com.bexa.visuals.util.ColorUtils;
import com.bexa.visuals.util.Easing;
import com.bexa.visuals.util.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BexaVisuals menyusi. Immediate-mode: har kadrda hamma narsa chiziladi va bosish
 * zonalari ("hits") ro'yxatga yig'iladi. Barcha koordinatalar ekran (GUI-scale) pikselida.
 */
public class ClickGuiScreen extends Screen {
    // ---- saqlanuvchi holat (menyu qayta ochilganda eslab qoladi) ----
    private static float offX, offY;
    private static int page = 0;

    private static final String[] PAGE_NAMES = {"Visuals", "ESP", "Players", "Config", "Misc"};
    private static final Category[] PAGE_CATS = {Category.VISUALS, Category.ESP, Category.PLAYERS, null, Category.MISC};
    private static final int CONFIG_PAGE = 3;

    private static final int ROW_H = 22;
    private static final int PICKER_H = 88;

    // ---- ichki turlar ----
    private interface Click { void run(double mx, double my, int button); }
    private interface Drag { void run(double mx, double my); }
    private record Hit(int x1, int y1, int x2, int y2, Click click, Drag drag) {
        boolean in(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    private static class ModState {
        final Animation toggle, hover, expand;
        boolean expanded;
        ColorSetting openColor;
        ModState(boolean enabled) {
            toggle = new Animation(enabled ? 1f : 0f, 14f);
            hover = new Animation(0f, 14f);
            expand = new Animation(0f, 12f);
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Module, ModState> states = new HashMap<>();
    private final Animation openAnim = new Animation(0f, 9f);
    private final Animation pageAnim = new Animation(0f, 12f);
    private final Animation[] dockHover = new Animation[5];

    private Drag activeDrag;
    private boolean draggingPanel;
    private Module binding;
    private int focus; // 0 yo'q, 1 qidiruv, 2 preset nomi
    private String search = "";
    private String cfgName = "";
    private String selectedPreset;
    private List<String> presets = new ArrayList<>();
    private String status = "";
    private long statusUntil;
    private final long openedAt = System.currentTimeMillis();

    private float scroll, scrollTarget, contentH, viewH;

    // kadr davomida ishlatiladigan o'zgaruvchilar
    private float fade = 1f;
    private int curMX, curMY, clipY1, clipY2;

    public ClickGuiScreen() {
        super(Text.literal("BexaVisuals"));
        for (int i = 0; i < dockHover.length; i++) dockHover[i] = new Animation(0f, 14f);
        pageAnim.snap(0f);
        pageAnim.setTarget(1f);
        refreshPresets();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() {
        BexaVisualsClient inst = BexaVisualsClient.INSTANCE;
        if (inst != null) inst.config.save();
    }

    // ================= yordamchilar =================
    private int a(int color) { return ColorUtils.mulAlpha(color, fade); }

    private void text(DrawContext c, String s, int x, int y, int color) {
        int col = a(color);
        if (ColorUtils.alpha(col) < 5) return;
        c.drawText(textRenderer, s, x, y, col, false);
    }

    private void textRight(DrawContext c, String s, int rightX, int y, int color) {
        text(c, s, rightX - textRenderer.getWidth(s), y, color);
    }

    private boolean hover(int x, int y, int w, int h) {
        return curMX >= x && curMX < x + w && curMY >= Math.max(y, clipY1) && curMY < Math.min(y + h, clipY2)
                && curMY >= y && curMY < y + h;
    }

    private void addHit(int x, int y, int w, int h, Click click, Drag drag) {
        hits.add(new Hit(x, y, x + w, y + h, click, drag));
    }

    /** Faqat ko'rinadigan (scissor ichidagi) qismi bosiladigan zona. */
    private void addContentHit(int x, int y, int w, int h, Click click, Drag drag) {
        int y1 = Math.max(y, clipY1), y2 = Math.min(y + h, clipY2);
        if (y2 <= y1) return;
        hits.add(new Hit(x, y1, x + w, y2, click, drag));
    }

    private void setStatus(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 3500;
    }

    private void refreshPresets() {
        BexaVisualsClient inst = BexaVisualsClient.INSTANCE;
        presets = inst != null ? inst.config.listPresets() : new ArrayList<>();
        if (selectedPreset != null && !presets.contains(selectedPreset)) selectedPreset = null;
    }

    private void switchPage(int p) {
        if (page == p) return;
        page = p;
        scroll = 0;
        scrollTarget = 0;
        pageAnim.snap(0f);
        pageAnim.setTarget(1f);
        focus = 0;
        if (p == CONFIG_PAGE) refreshPresets();
    }

    private static float clamp01(double v) { return (float) Math.max(0.0, Math.min(1.0, v)); }

    // ================= asosiy chizish =================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hits.clear();
        curMX = mouseX;
        curMY = mouseY;

        openAnim.setTarget(1f);
        float o = Easing.easeOutCubic(openAnim.update());
        fade = o;
        pageAnim.update();

        // fon xira qilish
        ctx.fill(0, 0, width, height, ColorUtils.withAlpha(0xFF000000, 0.45f * o));

        int W = Math.min(520, width - 16);
        int H = Math.min(340, height - 70);
        int px = (width - W) / 2 + (int) offX;
        int py = (height - H - 40) / 2 + (int) offY + Math.round((1f - o) * 16f);
        final int cx = px + 14;
        final int cw = W - 28;
        final int cy = py + 62;
        final int ch = H - 62 - 12;
        viewH = ch;

        // ---- panel ----
        RenderUtils.glow(ctx, px, py, W, H, 10, ColorUtils.withAlpha(ColorUtils.RED, a(0x66FFFFFF) >>> 24), 4);
        RenderUtils.roundedRect(ctx, px, py, W, H, 10, a(ColorUtils.BG_PANEL));
        RenderUtils.roundedOutline(ctx, px, py, W, H, 10, 1, a(ColorUtils.withAlpha(ColorUtils.RED_DARK, 200)));

        // sarlavha bo'yicha sudrash
        addHit(px, py, W, 32, (mx, my, b) -> { if (b == 0) draggingPanel = true; }, null);

        drawLogo(ctx, px, py, W);

        // tab nomi
        String tab = search.isEmpty() ? PAGE_NAMES[page] : "Search";
        text(ctx, tab, px + 16, py + 36, ColorUtils.WHITE);
        RenderUtils.roundedRect(ctx, px + 16, py + 48, textRenderer.getWidth(tab), 2, 1, a(ColorUtils.RED));

        drawSearch(ctx, px + W - 16 - 130, py + 33, 130, 16);

        // animatsiyali ajratuvchi chiziq
        for (int sx = px + 14; sx < px + W - 14; sx += 4) {
            int col = ColorUtils.withAlpha(ColorUtils.themeGradient(5f, (sx - px) * 8), 0.6f);
            ctx.fill(sx, py + 55, Math.min(sx + 4, px + W - 14), py + 56, a(col));
        }

        // ---- kontent ----
        clipY1 = cy;
        clipY2 = cy + ch;
        float baseFade = fade;
        fade = baseFade * Easing.easeOutCubic(pageAnim.get());
        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        if (search.isEmpty() && page == CONFIG_PAGE) {
            contentH = drawConfigPage(ctx, cx, cy, cw);
        } else {
            contentH = drawModulePage(ctx, cx, cy, cw);
        }
        ctx.disableScissor();
        fade = baseFade;

        // skroll
        float maxScroll = Math.max(0f, contentH - viewH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * 0.25f;
        scroll = Math.max(0f, Math.min(maxScroll, scroll));
        if (maxScroll > 0) {
            int trackH = ch;
            int thumbH = Math.max(18, (int) (trackH * (viewH / contentH)));
            int thumbY = cy + (int) ((trackH - thumbH) * (scroll / maxScroll));
            RenderUtils.roundedRect(ctx, px + W - 9, thumbY, 3, thumbH, 1, a(ColorUtils.withAlpha(ColorUtils.RED, 150)));
        }

        clipY1 = Integer.MIN_VALUE;
        clipY2 = Integer.MAX_VALUE;

        drawDock(ctx, px, py, W, H);
    }

    // ================= sarlavha =================
    private void drawLogo(DrawContext ctx, int px, int py, int W) {
        String name = "BexaVisuals";
        float s = 1.5f;
        int textW = (int) (textRenderer.getWidth(name) * s);
        int total = 18 + 6 + textW;
        int sx = px + (W - total) / 2;
        int icy = py + 18;
        RenderUtils.diamond(ctx, sx + 9, icy, 9, a(ColorUtils.RED));
        RenderUtils.diamond(ctx, sx + 9, icy, 5, a(0xFF0B0B0E));
        RenderUtils.diamond(ctx, sx + 9, icy, 3, a(ColorUtils.GREEN));

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate((float) (sx + 24), (float) (py + 11), 0f);
        ms.scale(s, s, 1f);
        int x = 0;
        for (int i = 0; i < name.length(); i++) {
            String chs = String.valueOf(name.charAt(i));
            int col = a(ColorUtils.withAlpha(ColorUtils.themeGradient(3f, i * 140), 255));
            if (ColorUtils.alpha(col) >= 5) ctx.drawText(textRenderer, chs, x, 0, col, true);
            x += textRenderer.getWidth(chs);
        }
        ms.pop();
    }

    private void drawSearch(DrawContext ctx, int x, int y, int w, int h) {
        boolean f = focus == 1;
        RenderUtils.roundedRect(ctx, x, y, w, h, 5, a(ColorUtils.BG_ROW));
        RenderUtils.roundedOutline(ctx, x, y, w, h, 5, 1, a(f ? ColorUtils.RED : 0xFF2A2A32));
        // kattalashtirgich belgisi
        RenderUtils.ring(ctx, x + 9, y + 7, 3, 1, a(ColorUtils.TEXT_DIM));
        ctx.fill(x + 12, y + 10, x + 14, y + 12, a(ColorUtils.TEXT_DIM));
        String shown = search;
        while (textRenderer.getWidth(shown) > w - 28 && !shown.isEmpty()) shown = shown.substring(1);
        if (shown.isEmpty() && !f) {
            text(ctx, "Search...", x + 20, y + 4, ColorUtils.TEXT_DIM);
        } else {
            text(ctx, shown, x + 20, y + 4, ColorUtils.WHITE);
            if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cxp = x + 20 + textRenderer.getWidth(shown);
                ctx.fill(cxp, y + 3, cxp + 1, y + 12, a(ColorUtils.WHITE));
            }
        }
        addHit(x, y, w, h, (mx, my, b) -> focus = 1, null);
    }

    // ================= dock =================
    private void drawDock(DrawContext ctx, int px, int py, int W, int H) {
        int btn = 26, gap = 8;
        int dockW = 5 * btn + 4 * gap + 20;
        int dockH = 34;
        int dx = px + (W - dockW) / 2;
        int dy = py + H + 10;
        RenderUtils.glow(ctx, dx, dy, dockW, dockH, 17, ColorUtils.withAlpha(ColorUtils.RED, 60), 2);
        RenderUtils.roundedRect(ctx, dx, dy, dockW, dockH, 17, a(ColorUtils.BG_PANEL));
        RenderUtils.roundedOutline(ctx, dx, dy, dockW, dockH, 17, 1, a(ColorUtils.withAlpha(ColorUtils.RED_DARK, 200)));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            int bx = dx + 10 + i * (btn + gap);
            int by = dy + 4;
            int ccx = bx + btn / 2, ccy = by + btn / 2;
            boolean hov = curMX >= bx && curMX < bx + btn && curMY >= by && curMY < by + btn;
            boolean sel = page == i && search.isEmpty();
            dockHover[i].setTarget(hov || sel ? 1f : 0f);
            float hv = dockHover[i].update();
            RenderUtils.circle(ctx, ccx, ccy, 13, a(ColorUtils.lerp(0xFF1A1A20, 0xFF2A1518, hv)));
            if (sel) RenderUtils.ring(ctx, ccx, ccy, 13, 1, a(ColorUtils.RED));
            else if (hv > 0.02f) RenderUtils.ring(ctx, ccx, ccy, 13, 1, a(ColorUtils.withAlpha(ColorUtils.RED, hv * 0.6f)));
            int icol = sel ? ColorUtils.RED : ColorUtils.lerp(0xFF8A8A95, ColorUtils.WHITE, hv);
            drawIcon(ctx, i, ccx, ccy, a(icol));
            addHit(bx, by, btn, btn, (mx, my, b) -> {
                if (b == 0) {
                    search = "";
                    switchPage(idx);
                }
            }, null);
        }
        // tanlangan sahifa nomi
        if (curMY >= dy && curMY < dy + dockH) {
            for (int i = 0; i < 5; i++) {
                int bx = dx + 10 + i * (btn + gap);
                if (curMX >= bx && curMX < bx + btn) {
                    String n = PAGE_NAMES[i];
                    int tw = textRenderer.getWidth(n);
                    int tx = bx + btn / 2 - tw / 2;
                    RenderUtils.roundedRect(ctx, tx - 4, dy + dockH + 4, tw + 8, 13, 4, a(0xE0121216));
                    text(ctx, n, tx, dy + dockH + 7, ColorUtils.WHITE);
                }
            }
        }
    }

    private void drawIcon(DrawContext ctx, int i, int cx, int cy, int col) {
        switch (i) {
            case 0 -> { // Visuals (ko'z)
                RenderUtils.ring(ctx, cx, cy, 6, 1, col);
                RenderUtils.circle(ctx, cx, cy, 2, col);
            }
            case 1 -> { // ESP (burchak qavslari)
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int x0 = cx + sx * 6, y0 = cy + sy * 6;
                        ctx.fill(Math.min(x0, x0 - sx * 4), y0, Math.max(x0, x0 - sx * 4) + 1, y0 + 1, col);
                        ctx.fill(x0, Math.min(y0, y0 - sy * 4), x0 + 1, Math.max(y0, y0 - sy * 4) + 1, col);
                    }
                }
                RenderUtils.circle(ctx, cx, cy, 1, col);
            }
            case 2 -> { // Players
                RenderUtils.circle(ctx, cx, cy - 3, 3, col);
                RenderUtils.roundedRect(ctx, cx - 5, cy + 1, 10, 5, 2, col);
            }
            case 3 -> { // Config (slayderlar)
                int[] kx = {cx - 3, cx + 3, cx - 1};
                for (int k = -1; k <= 1; k++) {
                    int y = cy + k * 4;
                    ctx.fill(cx - 6, y, cx + 7, y + 1, col);
                    ctx.fill(kx[k + 1] - 1, y - 1, kx[k + 1] + 2, y + 2, col);
                }
            }
            default -> { // Misc (3x3 nuqta)
                for (int x = 0; x < 3; x++)
                    for (int y = 0; y < 3; y++)
                        ctx.fill(cx - 5 + x * 4, cy - 5 + y * 4, cx - 3 + x * 4, cy - 3 + y * 4, col);
            }
        }
    }

    // ================= modul sahifasi =================
    private List<Module> currentModules() {
        List<Module> all = BexaVisualsClient.INSTANCE.modules.getAll();
        List<Module> out = new ArrayList<>();
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            for (Module m : all) if (m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        } else {
            Category cat = PAGE_CATS[page];
            for (Module m : all) if (m.category == cat) out.add(m);
        }
        return out;
    }

    private int drawModulePage(DrawContext ctx, int cx, int cy, int cw) {
        List<Module> list = currentModules();
        int top = cy - (int) scroll;
        if (list.isEmpty()) {
            text(ctx, search.isEmpty() ? "No modules on this page yet." : "No modules match your search.", cx + 4, top + 6, ColorUtils.TEXT_DIM);
            return 24;
        }
        int colW = (cw - 8) / 2;
        int[] colY = {top, top};
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2;
            int x = cx + col * (colW + 8);
            int h = drawModule(ctx, list.get(i), x, colY[col], colW);
            colY[col] += h + 6;
        }
        return Math.max(colY[0], colY[1]) - top;
    }

    private int settingHeight(Setting s, ModState st) {
        if (s instanceof NumberSetting) return 26;
        if (s instanceof ColorSetting) return 18 + (st.openColor == s ? PICKER_H : 0);
        if (s instanceof MultiBoolSetting mb) {
            int n = 0;
            for (String ignored : mb.keys()) n++;
            return 16 + 16 * n;
        }
        return 18;
    }

    private int settingsFullHeight(Module m, ModState st) {
        int h = 8;
        for (Setting s : m.getSettings()) if (s.isVisible()) h += settingHeight(s, st);
        return h;
    }

    private int drawModule(DrawContext ctx, Module m, int x, int y, int w) {
        ModState st = states.computeIfAbsent(m, k -> new ModState(k.isEnabled()));
        st.toggle.setTarget(m.isEnabled() ? 1f : 0f);
        float tg = st.toggle.update();
        boolean hov = hover(x, y, w, ROW_H);
        st.hover.setTarget(hov ? 1f : 0f);
        float hv = st.hover.update();
        st.expand.setTarget(st.expanded ? 1f : 0f);
        float ex = st.expand.update();

        RenderUtils.roundedRect(ctx, x, y, w, ROW_H, 5, a(ColorUtils.lerp(ColorUtils.BG_ROW, 0xFF24242C, hv)));
        if (hv > 0.02f) RenderUtils.roundedOutline(ctx, x, y, w, ROW_H, 5, 1, a(ColorUtils.withAlpha(ColorUtils.RED, hv * 0.35f)));

        text(ctx, m.name, x + 8, y + 7, ColorUtils.lerp(0xFFA0A0AA, ColorUtils.WHITE, tg));

        // toggle
        int tw = 26, th = 12;
        int tx = x + w - tw - 8, ty = y + (ROW_H - th) / 2;
        RenderUtils.roundedRect(ctx, tx, ty, tw, th, 6, a(ColorUtils.lerp(ColorUtils.GRAY, ColorUtils.GREEN, tg)));
        int kx = tx + 2 + Math.round((tw - th) * tg);
        RenderUtils.circle(ctx, kx + 4, ty + th / 2, 4, a(ColorUtils.lerp(0xFFB86060, ColorUtils.WHITE, tg)));

        // o'ng chetdagi yordamchi belgilar
        int rightEdge = tx - 6;
        boolean has = !m.getSettings().isEmpty();
        if (has) {
            int ccx = rightEdge - 4, ccy = y + ROW_H / 2;
            int col = a(ColorUtils.lerp(0xFF70707A, ColorUtils.WHITE, Math.max(hv, ex)));
            boolean up = ex > 0.5f;
            for (int i = 0; i < 4; i++) {
                int yy = up ? ccy + 1 - i : ccy - 2 + i;
                ctx.fill(ccx - 3 + i, yy, ccx + 4 - i, yy + 1, col);
            }
            rightEdge -= 14;
        }
        if (binding == m) {
            textRight(ctx, "[...]", rightEdge, y + 7, ColorUtils.RED);
        } else if (m.getKey() >= 0) {
            String kn = keyName(m.getKey());
            textRight(ctx, "[" + kn + "]", rightEdge, y + 7, ColorUtils.TEXT_DIM);
        }

        final Module mod = m;
        addContentHit(x, y, w, ROW_H, (mx, my, b) -> {
            if (b == 0) mod.toggle();
            else if (b == 1 && !mod.getSettings().isEmpty()) st.expanded = !st.expanded;
            else if (b == 2) binding = mod;
        }, null);

        int total = ROW_H;
        if (ex > 0.01f) {
            int full = settingsFullHeight(m, st);
            int drawn = Math.round(full * Easing.easeOutCubic(ex));
            int top = y + ROW_H + 2;
            ctx.enableScissor(x, top, x + w, top + drawn);
            RenderUtils.roundedRect(ctx, x, top, w, full, 5, a(0xFF15151B));
            boolean interactive = ex > 0.97f && st.expanded;
            int sy = top + 4;
            for (Setting s : m.getSettings()) {
                if (!s.isVisible()) continue;
                sy += drawSetting(ctx, s, st, x, sy, w, interactive);
            }
            ctx.disableScissor();
            total += 2 + drawn;
        }
        return total;
    }

    private static String keyName(int key) {
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
        } catch (Throwable t) {
            return String.valueOf(key);
        }
    }

    // ================= sozlama vidjetlari =================
    private int drawSetting(DrawContext ctx, Setting s, ModState st, int x, int y, int w, boolean interactive) {
        if (s instanceof BoolSetting b) return drawBool(ctx, b, x, y, w, interactive);
        if (s instanceof NumberSetting n) return drawNumber(ctx, n, x, y, w, interactive);
        if (s instanceof ModeSetting m) return drawMode(ctx, m, x, y, w, interactive);
        if (s instanceof ColorSetting c) return drawColor(ctx, c, st, x, y, w, interactive);
        if (s instanceof MultiBoolSetting mb) return drawMulti(ctx, mb, x, y, w, interactive);
        return 18;
    }

    private void miniSwitch(DrawContext ctx, int x, int y, boolean on) {
        RenderUtils.roundedRect(ctx, x, y, 22, 10, 5, a(on ? ColorUtils.GREEN : ColorUtils.GRAY));
        RenderUtils.circle(ctx, on ? x + 17 : x + 5, y + 5, 3, a(on ? ColorUtils.WHITE : 0xFFB86060));
    }

    private int drawBool(DrawContext ctx, BoolSetting s, int x, int y, int w, boolean interactive) {
        text(ctx, s.name, x + 8, y + 5, 0xFFC8C8D0);
        miniSwitch(ctx, x + w - 8 - 22, y + 4, s.get());
        if (interactive) addContentHit(x, y, w, 18, (mx, my, b) -> s.toggle(), null);
        return 18;
    }

    private static String fmt(NumberSetting s) {
        if (s.step >= 1) return String.valueOf(s.getI());
        return Double.toString(Math.round(s.get() * 100.0) / 100.0);
    }

    private int drawNumber(DrawContext ctx, NumberSetting s, int x, int y, int w, boolean interactive) {
        text(ctx, s.name, x + 8, y + 3, 0xFFC8C8D0);
        textRight(ctx, fmt(s), x + w - 8, y + 3, ColorUtils.TEXT_DIM);
        int bx = x + 8, bw = w - 16, by = y + 16;
        float p = (float) s.getProgress();
        RenderUtils.roundedRect(ctx, bx, by, bw, 4, 2, a(ColorUtils.GRAY));
        int fillW = Math.max(4, Math.round(bw * p));
        RenderUtils.roundedRect(ctx, bx, by, fillW, 4, 2, a(ColorUtils.RED));
        RenderUtils.circle(ctx, bx + Math.round(bw * p), by + 2, 4, a(ColorUtils.WHITE));
        if (interactive) {
            addContentHit(bx - 3, y + 9, bw + 6, 15, null,
                    (mx, my) -> s.setProgress((mx - bx) / (double) bw));
            // drag.run bosilganda chaqirilishi uchun click ham kerak emas: mouseClicked drag'ni ishga tushiradi
        }
        return 26;
    }

    private int drawMode(DrawContext ctx, ModeSetting s, int x, int y, int w, boolean interactive) {
        text(ctx, s.name, x + 8, y + 5, 0xFFC8C8D0);
        textRight(ctx, "< " + s.get() + " >", x + w - 8, y + 5, ColorUtils.lerp(ColorUtils.RED, ColorUtils.WHITE, 0.35f));
        if (interactive) addContentHit(x, y, w, 18, (mx, my, b) -> {
            if (b == 0) s.next();
            else if (b == 1) s.previous();
        }, null);
        return 18;
    }

    private int drawMulti(DrawContext ctx, MultiBoolSetting s, int x, int y, int w, boolean interactive) {
        text(ctx, s.name, x + 8, y + 4, ColorUtils.TEXT_DIM);
        int yy = y + 16;
        for (String k : s.keys()) {
            final String key = k;
            boolean on = s.get(k);
            text(ctx, k, x + 14, yy + 4, 0xFFC8C8D0);
            RenderUtils.roundedRect(ctx, x + w - 8 - 10, yy + 3, 10, 10, 3, a(on ? ColorUtils.GREEN : ColorUtils.GRAY));
            if (on) ctx.fill(x + w - 8 - 7, yy + 7, x + w - 8 - 3, yy + 9, a(ColorUtils.WHITE));
            if (interactive) addContentHit(x, yy, w, 16, (mx, my, b) -> s.toggle(key), null);
            yy += 16;
        }
        return yy - y;
    }

    private int drawColor(DrawContext ctx, ColorSetting s, ModState st, int x, int y, int w, boolean interactive) {
        text(ctx, s.name, x + 8, y + 5, 0xFFC8C8D0);
        int swW = 24, swH = 10, sx = x + w - 8 - swW, sy = y + 4;
        RenderUtils.roundedRect(ctx, sx - 1, sy - 1, swW + 2, swH + 2, 3, a(0xFF3A3A44));
        RenderUtils.roundedRect(ctx, sx, sy, swW, swH, 3, a(ColorUtils.withAlpha(s.getColor(), 255)));
        if (interactive) addContentHit(x, y, w, 18, (mx, my, b) -> st.openColor = (st.openColor == s) ? null : s, null);
        int h = 18;
        if (st.openColor != s) return h;

        final int sbX = x + 8, sbW = w - 16;
        int ty = y + 20;
        int sbH = 40;
        float hue = s.getHue();
        for (int i = 0; i < sbW; i += 2) {
            float sat = i / (float) sbW;
            ctx.fillGradient(sbX + i, ty, Math.min(sbX + i + 2, sbX + sbW), ty + sbH,
                    a(ColorUtils.hsb(hue, sat, 1f, 1f)), a(0xFF000000));
        }
        RenderUtils.ring(ctx, sbX + Math.round(s.getSat() * sbW), ty + Math.round((1f - s.getBri()) * sbH), 3, 1, a(ColorUtils.WHITE));
        final int tyF = ty;
        if (interactive) addContentHit(sbX, ty, sbW, sbH, null, (mx, my) -> {
            s.setSat(clamp01((mx - sbX) / (double) sbW));
            s.setBri(clamp01(1.0 - (my - tyF) / (double) sbH));
        });

        int hy = ty + sbH + 4;
        RenderUtils.hStrips(ctx, sbX, hy, sbW, 8, 2, f -> a(ColorUtils.hsb(f, 1f, 1f, 1f)));
        ctx.fill(sbX + Math.round(hue * (sbW - 1)), hy - 1, sbX + Math.round(hue * (sbW - 1)) + 1, hy + 9, a(ColorUtils.WHITE));
        if (interactive) addContentHit(sbX, hy - 2, sbW, 12, null, (mx, my) -> s.setHue(clamp01((mx - sbX) / (double) sbW)));

        int ay = hy + 8 + 4;
        final int base = ColorUtils.hsb(hue, s.getSat(), s.getBri(), 1f);
        RenderUtils.hStrips(ctx, sbX, ay, sbW, 8, 2, f -> a(ColorUtils.lerp(0xFF202028, base, f)));
        ctx.fill(sbX + Math.round(s.getAlpha() * (sbW - 1)), ay - 1, sbX + Math.round(s.getAlpha() * (sbW - 1)) + 1, ay + 9, a(ColorUtils.WHITE));
        if (interactive) addContentHit(sbX, ay - 2, sbW, 12, null, (mx, my) -> s.setAlpha(clamp01((mx - sbX) / (double) sbW)));

        int ry = ay + 8 + 4;
        text(ctx, "Rainbow", x + 8, ry + 3, 0xFFC8C8D0);
        miniSwitch(ctx, x + w - 8 - 22, ry + 2, s.isRainbow());
        if (interactive) addContentHit(x, ry, w, 14, (mx, my, b) -> s.setRainbow(!s.isRainbow()), null);
        return h + PICKER_H;
    }

    // ================= Config sahifasi =================
    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int accent, Runnable action) {
        boolean hov = hover(x, y, w, h);
        RenderUtils.roundedRect(ctx, x, y, w, h, 5, a(hov ? 0xFF26262E : ColorUtils.BG_ROW));
        RenderUtils.roundedOutline(ctx, x, y, w, h, 5, 1, a(ColorUtils.withAlpha(accent, hov ? 255 : 150)));
        int tw = textRenderer.getWidth(label);
        text(ctx, label, x + (w - tw) / 2, y + (h - 8) / 2, ColorUtils.WHITE);
        addContentHit(x, y, w, h, (mx, my, b) -> { if (b == 0) action.run(); }, null);
    }

    private void saveNamedPreset() {
        String name = !cfgName.trim().isEmpty() ? cfgName.trim() : selectedPreset;
        if (name == null || name.isEmpty()) { setStatus("Enter a preset name first"); return; }
        if (BexaVisualsClient.INSTANCE.config.savePreset(name)) {
            selectedPreset = name;
            setStatus("Saved preset: " + name);
        } else {
            setStatus("Could not save preset");
        }
        refreshPresets();
    }

    private int drawConfigPage(DrawContext ctx, int cx, int cy, int cw) {
        int colW = (cw - 8) / 2;
        int y = cy - (int) scroll;
        BexaVisualsClient inst = BexaVisualsClient.INSTANCE;

        text(ctx, "Saved presets", cx + 2, y + 2, ColorUtils.WHITE);
        int ly = y + 16;
        if (presets.isEmpty()) {
            text(ctx, "No presets yet", cx + 2, ly + 4, ColorUtils.TEXT_DIM);
            ly += 22;
        }
        for (String p : presets) {
            final String name = p;
            boolean sel = p.equals(selectedPreset);
            boolean hov = hover(cx, ly, colW, 20);
            RenderUtils.roundedRect(ctx, cx, ly, colW, 20, 5, a(sel ? 0xFF2A1A1E : (hov ? 0xFF24242C : ColorUtils.BG_ROW)));
            if (sel) RenderUtils.roundedOutline(ctx, cx, ly, colW, 20, 5, 1, a(ColorUtils.RED));
            text(ctx, p, cx + 8, ly + 6, sel ? ColorUtils.WHITE : 0xFFC8C8D0);
            addContentHit(cx, ly, colW, 20, (mx, my, b) -> { selectedPreset = name; cfgName = name; }, null);
            ly += 24;
        }

        int rx = cx + colW + 8;
        text(ctx, "Preset name", rx + 2, y + 2, ColorUtils.WHITE);
        boolean f = focus == 2;
        RenderUtils.roundedRect(ctx, rx, y + 14, colW, 18, 5, a(ColorUtils.BG_ROW));
        RenderUtils.roundedOutline(ctx, rx, y + 14, colW, 18, 5, 1, a(f ? ColorUtils.RED : 0xFF2A2A32));
        String shown = cfgName;
        while (textRenderer.getWidth(shown) > colW - 16 && !shown.isEmpty()) shown = shown.substring(1);
        if (shown.isEmpty() && !f) text(ctx, "Type a name...", rx + 8, y + 19, ColorUtils.TEXT_DIM);
        else {
            text(ctx, shown, rx + 8, y + 19, ColorUtils.WHITE);
            if (f && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cxp = rx + 8 + textRenderer.getWidth(shown);
                ctx.fill(cxp, y + 18, cxp + 1, y + 28, a(ColorUtils.WHITE));
            }
        }
        addContentHit(rx, y + 14, colW, 18, (mx, my, b) -> focus = 2, null);

        button(ctx, rx, y + 40, colW, 20, "Save preset", ColorUtils.GREEN, this::saveNamedPreset);
        button(ctx, rx, y + 64, colW, 20, "Load selected", ColorUtils.GREEN, () -> {
            if (selectedPreset == null) { setStatus("Select a preset first"); return; }
            setStatus(inst.config.loadPreset(selectedPreset) ? "Loaded: " + selectedPreset : "Could not load preset");
        });
        button(ctx, rx, y + 88, colW, 20, "Delete selected", ColorUtils.RED, () -> {
            if (selectedPreset == null) { setStatus("Select a preset first"); return; }
            String d = selectedPreset;
            setStatus(inst.config.deletePreset(d) ? "Deleted: " + d : "Could not delete preset");
            refreshPresets();
        });
        button(ctx, rx, y + 116, colW, 20, "Save current config now", ColorUtils.RED, () -> {
            inst.config.save();
            setStatus("Config saved (bexavisuals.json)");
        });
        if (System.currentTimeMillis() < statusUntil) text(ctx, status, rx + 2, y + 144, ColorUtils.GREEN);
        text(ctx, "Auto-saves when you close the menu.", rx + 2, y + 158, ColorUtils.TEXT_DIM);

        return Math.max(ly, y + 174) - y;
    }

    // ================= kiritish =================
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        focus = 0;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mx, my)) continue;
            if (h.drag() != null && button == 0) {
                activeDrag = h.drag();
                h.drag().run(mx, my);
            }
            if (h.click() != null) h.click().run(mx, my, button);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingPanel) {
            offX += (float) dx;
            offY += (float) dy;
            float lim = Math.max(width, height);
            offX = Math.max(-lim, Math.min(lim, offX));
            offY = Math.max(-lim, Math.min(lim, offY));
            return true;
        }
        if (activeDrag != null) {
            activeDrag.run(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingPanel = false;
        activeDrag = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scrollTarget -= (float) (vAmount * 24.0);
        float maxScroll = Math.max(0f, contentH - viewH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // tugma biriktirish rejimi
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                binding.setKey(-1);
            } else {
                binding.setKey(keyCode);
            }
            binding = null;
            return true;
        }
        // matn kiritish
        if (focus != 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { focus = 0; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (focus == 1 && !search.isEmpty()) { search = search.substring(0, search.length() - 1); scrollTarget = 0; }
                else if (focus == 2 && !cfgName.isEmpty()) cfgName = cfgName.substring(0, cfgName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focus == 2) saveNamedPreset(); else focus = 0;
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && System.currentTimeMillis() - openedAt > 350)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focus == 0 || chr < 32 || chr == 127) return super.charTyped(chr, modifiers);
        if (focus == 1 && search.length() < 24) { search += chr; scrollTarget = 0; }
        else if (focus == 2 && cfgName.length() < 24) cfgName += chr;
        return true;
    }
}

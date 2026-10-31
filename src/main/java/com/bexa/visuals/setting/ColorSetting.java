package com.bexa.visuals.setting;

import com.bexa.visuals.util.ColorUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** To'liq HSB rang tanlagich + alfa + kamalak (rainbow) rejimi. */
public class ColorSetting extends Setting {
    private float hue, sat, bri, alpha;
    private boolean rainbow;

    public ColorSetting(String name, int argb) {
        super(name);
        setARGB(argb);
    }

    public float getHue() { return hue; }
    public float getSat() { return sat; }
    public float getBri() { return bri; }
    public float getAlpha() { return alpha; }
    public boolean isRainbow() { return rainbow; }

    public void setHue(float h) { hue = clamp01(h); }
    public void setSat(float s) { sat = clamp01(s); }
    public void setBri(float b) { bri = clamp01(b); }
    public void setAlpha(float a) { alpha = clamp01(a); }
    public void setRainbow(boolean r) { rainbow = r; }

    public void setARGB(int argb) {
        float[] hsb = ColorUtils.toHSB(argb);
        hue = hsb[0];
        sat = hsb[1];
        bri = hsb[2];
        alpha = ((argb >> 24) & 0xFF) / 255f;
    }

    /** Joriy rang (ARGB). Rainbow yoqilgan bo'lsa, vaqtga qarab o'zgaradi. */
    public int getColor() { return getColor(0); }

    /** Offset bilan: har xil nuqtalar uchun ranglar turlicha bo'ladi. */
    public int getColor(int offsetMs) {
        if (rainbow) return ColorUtils.rainbow(ColorUtils.rainbowSpeed, offsetMs, 1f, alpha);
        return ColorUtils.hsb(hue, sat, bri, alpha);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    public JsonElement serialize() {
        JsonObject o = new JsonObject();
        o.addProperty("h", hue);
        o.addProperty("s", sat);
        o.addProperty("b", bri);
        o.addProperty("a", alpha);
        o.addProperty("rainbow", rainbow);
        return o;
    }

    @Override
    public void deserialize(JsonElement e) {
        if (e == null || !e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        if (o.has("h")) setHue(o.get("h").getAsFloat());
        if (o.has("s")) setSat(o.get("s").getAsFloat());
        if (o.has("b")) setBri(o.get("b").getAsFloat());
        if (o.has("a")) setAlpha(o.get("a").getAsFloat());
        if (o.has("rainbow")) setRainbow(o.get("rainbow").getAsBoolean());
    }
}

package com.bexa.visuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting {
    public final double min, max, step;
    private double value;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = clampRound(def);
    }

    private double clampRound(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        v = Math.round(v * 10000.0) / 10000.0;
        return Math.max(min, Math.min(max, v));
    }

    public double get() { return value; }
    public float getF() { return (float) value; }
    public int getI() { return (int) Math.round(value); }

    public void set(double v) { this.value = clampRound(v); }

    /** 0..1 oralig'ida nisbat (slider uchun). */
    public double getProgress() { return max == min ? 0 : (value - min) / (max - min); }

    public void setProgress(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }

    @Override
    public JsonElement serialize() { return new JsonPrimitive(value); }

    @Override
    public void deserialize(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) set(e.getAsDouble());
    }
}

package com.bexa.visuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    private boolean value;

    public BoolSetting(String name, boolean def) {
        super(name);
        this.value = def;
    }

    public boolean get() { return value; }
    public void set(boolean v) { this.value = v; }
    public void toggle() { this.value = !this.value; }

    @Override
    public JsonElement serialize() { return new JsonPrimitive(value); }

    @Override
    public void deserialize(JsonElement e) {
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) value = e.getAsBoolean();
    }
}

package com.bexa.visuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting {
    public final String[] modes;
    private int index;

    public ModeSetting(String name, String def, String... modes) {
        super(name);
        this.modes = modes;
        this.index = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(def)) { index = i; break; }
        }
    }

    public String get() { return modes[index]; }
    public int getIndex() { return index; }
    public boolean is(String mode) { return modes[index].equalsIgnoreCase(mode); }

    public void setIndex(int i) {
        if (modes.length == 0) return;
        this.index = ((i % modes.length) + modes.length) % modes.length;
    }

    public void next() { setIndex(index + 1); }
    public void previous() { setIndex(index - 1); }

    @Override
    public JsonElement serialize() { return new JsonPrimitive(get()); }

    @Override
    public void deserialize(JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) return;
        String s = e.getAsString();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(s)) { index = i; return; }
        }
    }
}

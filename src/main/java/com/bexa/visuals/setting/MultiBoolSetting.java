package com.bexa.visuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bir nechta ON/OFF variantlar guruhi (masalan: Players, Mobs, Items). */
public class MultiBoolSetting extends Setting {
    private final LinkedHashMap<String, Boolean> options = new LinkedHashMap<>();

    public MultiBoolSetting(String name, Object... pairs) {
        super(name);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            options.put((String) pairs[i], (Boolean) pairs[i + 1]);
        }
    }

    public boolean get(String key) { return options.getOrDefault(key, false); }

    public void set(String key, boolean v) {
        if (options.containsKey(key)) options.put(key, v);
    }

    public void toggle(String key) { set(key, !get(key)); }

    public Iterable<String> keys() { return options.keySet(); }

    @Override
    public JsonElement serialize() {
        JsonObject o = new JsonObject();
        options.forEach(o::addProperty);
        return o;
    }

    @Override
    public void deserialize(JsonElement e) {
        if (e == null || !e.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
            if (options.containsKey(en.getKey()) && en.getValue().isJsonPrimitive()) {
                options.put(en.getKey(), en.getValue().getAsBoolean());
            }
        }
    }
}

package com.bexa.visuals.config;

import com.bexa.visuals.BexaVisualsClient;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.module.ModuleManager;
import com.bexa.visuals.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path mainFile;
    private final Path presetDir;
    private final ModuleManager modules;

    public ConfigManager(ModuleManager modules) {
        this.modules = modules;
        Path cfg = FabricLoader.getInstance().getConfigDir();
        this.mainFile = cfg.resolve("bexavisuals.json");
        this.presetDir = cfg.resolve("bexavisuals_presets");
    }

    public void save() { write(mainFile); }

    public void load() { read(mainFile); }

    public List<String> listPresets() {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(presetDir)) return out;
        try (Stream<Path> s = Files.list(presetDir)) {
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> {
                        String n = p.getFileName().toString();
                        out.add(n.substring(0, n.length() - 5));
                    });
        } catch (IOException e) {
            BexaVisualsClient.LOGGER.error("Preset list failed", e);
        }
        Collections.sort(out);
        return out;
    }

    public boolean savePreset(String name) {
        String safe = sanitize(name);
        if (safe.isEmpty()) return false;
        return write(presetDir.resolve(safe + ".json"));
    }

    public boolean loadPreset(String name) {
        String safe = sanitize(name);
        if (safe.isEmpty()) return false;
        return read(presetDir.resolve(safe + ".json"));
    }

    public boolean deletePreset(String name) {
        String safe = sanitize(name);
        if (safe.isEmpty()) return false;
        try {
            return Files.deleteIfExists(presetDir.resolve(safe + ".json"));
        } catch (IOException e) {
            BexaVisualsClient.LOGGER.error("Preset delete failed", e);
            return false;
        }
    }

    private static String sanitize(String name) {
        return name == null ? "" : name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "");
    }

    private boolean write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonObject mods = new JsonObject();
            for (Module m : modules.getAll()) {
                JsonObject mo = new JsonObject();
                mo.addProperty("enabled", m.isEnabled());
                mo.addProperty("key", m.getKey());
                JsonObject so = new JsonObject();
                for (Setting s : m.getSettings()) so.add(s.name, s.serialize());
                mo.add("settings", so);
                mods.add(m.name, mo);
            }
            root.add("modules", mods);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
            return true;
        } catch (Throwable t) {
            BexaVisualsClient.LOGGER.error("Config save failed: {}", file, t);
            return false;
        }
    }

    private boolean read(Path file) {
        if (!Files.isRegularFile(file)) return false;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (!root.has("modules")) return false;
            JsonObject mods = root.getAsJsonObject("modules");
            for (Module m : modules.getAll()) {
                if (!mods.has(m.name)) continue;
                JsonObject mo = mods.getAsJsonObject(m.name);
                if (mo.has("key")) m.setKey(mo.get("key").getAsInt());
                if (mo.has("settings")) {
                    JsonObject so = mo.getAsJsonObject("settings");
                    for (Setting s : m.getSettings()) {
                        if (so.has(s.name)) s.deserialize(so.get(s.name));
                    }
                }
                if (mo.has("enabled")) m.setEnabled(mo.get("enabled").getAsBoolean());
            }
            return true;
        } catch (Throwable t) {
            BexaVisualsClient.LOGGER.error("Config load failed: {}", file, t);
            return false;
        }
    }
}

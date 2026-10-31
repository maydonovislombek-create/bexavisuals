package com.bexa.visuals.module;

import com.bexa.visuals.BexaVisualsClient;
import com.bexa.visuals.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;

    private boolean enabled;
    /** GLFW tugma kodi, -1 = bog'lanmagan. */
    private int key = -1;
    private final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> getSettings() { return Collections.unmodifiableList(settings); }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        try {
            if (value) onEnable(); else onDisable();
        } catch (Throwable t) {
            BexaVisualsClient.LOGGER.error("Module '{}' toggle error", name, t);
        }
    }

    public void toggle() { setEnabled(!enabled); }

    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    /** Har client tick'da (faqat yoqilgan bo'lsa) chaqiriladi. */
    public void onTick() {}

    /** HUD ustiga chizish (faqat yoqilgan bo'lsa). */
    public void onHudRender(DrawContext ctx) {}

    /** 3D dunyo ustiga chizish (faqat yoqilgan bo'lsa). cam = kamera dunyo koordinatasi. */
    public void onWorldRender(MatrixStack stack, Vec3d cam) {}

    /** Siz biror jonzotga hujum qilganingizda (pos - urish nuqtasi). */
    public void onAttack(Entity entity, Vec3d pos) {}

    /** O'yin yopilayotganda (sozlamalarni tiklash uchun). */
    public void onShutdown() {}

    protected void onEnable() {}
    protected void onDisable() {}

    protected static boolean inWorld() {
        return mc.player != null && mc.world != null;
    }
}

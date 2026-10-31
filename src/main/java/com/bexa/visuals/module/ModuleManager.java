package com.bexa.visuals.module;

import com.bexa.visuals.BexaVisualsClient;
import com.bexa.visuals.module.esp.*;
import com.bexa.visuals.module.misc.*;
import com.bexa.visuals.module.players.*;
import com.bexa.visuals.module.visuals.*;
import com.bexa.visuals.util.Cps;
import com.bexa.visuals.util.EntitySmoother;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private int hudFrame;

    public ModuleManager() {
        // ---- Visuals ----
        register(new JumpCircle());
        register(new BlockOverlay());
        register(new CustomCrosshair());
        register(new FullBright());
        register(new HitColor());
        register(new AspectRatio());
        register(new HitBubble());
        register(new NoRender());
        register(new Zoom());
        register(new FovChanger());
        // ---- ESP ----
        register(new TargetESP());
        register(new EspModule());
        register(new Tracers());
        register(new Nametags());
        register(new GlowEsp());
        // ---- Players ----
        register(new HitMarker());
        register(new DamageIndicator());
        register(new KillEffect());
        register(new Trails());
        // ---- Misc ----
        register(new Watermark());
        register(new InfoHud());
        register(new ArmorHud());
        register(new PotionHud());
        register(new Keystrokes());
        register(new ThemeEditor());
    }

    public void register(Module m) { modules.add(m); }

    public List<Module> getAll() { return Collections.unmodifiableList(modules); }

    public List<Module> getByCategory(Category c) {
        return modules.stream().filter(m -> m.category == c).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> clazz) {
        for (Module m : modules) if (m.getClass() == clazz) return (T) m;
        return null;
    }

    public boolean isEnabled(Class<? extends Module> clazz) {
        for (Module m : modules) if (m.getClass() == clazz) return m.isEnabled();
        return false;
    }

    public Module getByName(String name) {
        for (Module m : modules) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public void tick() {
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick();
            } catch (Throwable t) {
                BexaVisualsClient.LOGGER.error("Module '{}' tick error", m.name, t);
            }
        }
    }

    public void renderHud(DrawContext ctx) {
        hudFrame++;
        Cps.update(hudFrame);
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onHudRender(ctx);
            } catch (Throwable t) {
                BexaVisualsClient.LOGGER.error("Module '{}' HUD error", m.name, t);
            }
        }
    }

    public void renderWorld(MatrixStack stack, Vec3d cam) {
        EntitySmoother.beginFrame();
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onWorldRender(stack, cam);
            } catch (Throwable t) {
                BexaVisualsClient.LOGGER.error("Module '{}' world render error", m.name, t);
            }
        }
    }

    public void attack(Entity entity, Vec3d pos) {
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onAttack(entity, pos);
            } catch (Throwable t) {
                BexaVisualsClient.LOGGER.error("Module '{}' attack hook error", m.name, t);
            }
        }
    }

    public void shutdown() {
        for (Module m : modules) {
            try {
                m.onShutdown();
            } catch (Throwable t) {
                BexaVisualsClient.LOGGER.error("Module '{}' shutdown error", m.name, t);
            }
        }
    }
}

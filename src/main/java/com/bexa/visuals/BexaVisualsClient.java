package com.bexa.visuals;

import com.bexa.visuals.config.ConfigManager;
import com.bexa.visuals.gui.ClickGuiScreen;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BexaVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "bexavisuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("BexaVisuals");

    public static BexaVisualsClient INSTANCE;
    /** Zoom modulining "ushlab turish" tugmasi (default: C). */
    public static KeyBinding zoomKey;

    public ModuleManager modules;
    public ConfigManager config;
    private KeyBinding openKey;

    /** Modul tugmalarining "bosilgan" holatini kuzatish uchun. */
    private final boolean[] keyDown = new boolean[512];

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        modules = new ModuleManager();
        config = new ConfigManager(modules);
        config.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bexavisuals.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.category.bexavisuals"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bexavisuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.category.bexavisuals"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> modules.renderHud(ctx));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (ctx.camera() == null) return;
            modules.renderWorld(ctx.matrixStack(), ctx.camera().getPos());
        });

        // Hujum hodisasi: barcha modullarga yetkaziladi
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (world.isClient() && player == mc.player && entity instanceof LivingEntity) {
                Vec3d pos;
                if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == entity) pos = ehr.getPos();
                else pos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.6, entity.getZ());
                modules.attack(entity, pos);
            }
            return ActionResult.PASS;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {
            modules.shutdown();
            config.save();
        });

        LOGGER.info("BexaVisuals loaded with {} modules", modules.getAll().size());
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        while (openKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
        }

        modules.tick();

        // Modullarga bog'langan tugmalar (faqat GUI yopiq bo'lganda)
        if (client.currentScreen != null) return;
        long handle = client.getWindow().getHandle();
        for (Module m : modules.getAll()) {
            int k = m.getKey();
            if (k < 0 || k >= keyDown.length) continue;
            boolean down = InputUtil.isKeyPressed(handle, k);
            if (down && !keyDown[k]) m.toggle();
            keyDown[k] = down;
        }
    }
}

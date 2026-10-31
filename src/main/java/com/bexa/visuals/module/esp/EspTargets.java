package com.bexa.visuals.module.esp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/** ESP modullari uchun umumiy "mos jonzotni topish" yordamchisi. */
public final class EspTargets {
    private EspTargets() {}

    public static List<LivingEntity> collect(boolean players, boolean mobs, boolean animals, double maxDist) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<LivingEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        double maxSq = maxDist * maxDist;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || e.isRemoved()) continue;
            boolean ok;
            if (e instanceof PlayerEntity) ok = players;
            else if (e instanceof Monster) ok = mobs;
            else ok = animals;
            if (!ok) continue;
            if (mc.player.squaredDistanceTo(e) > maxSq) continue;
            out.add(le);
        }
        return out;
    }
}

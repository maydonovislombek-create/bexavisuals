package com.bexa.visuals.module.players;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.BoolSetting;
import com.bexa.visuals.setting.ColorSetting;
import com.bexa.visuals.setting.NumberSetting;
import com.bexa.visuals.util.ColorUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** Urganda ekran markazida qisqa vaqt ko'rinadigan X belgisi. */
public class HitMarker extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", ColorUtils.WHITE));
    private final NumberSetting size = add(new NumberSetting("Size", 4, 2, 10, 1));
    private final NumberSetting duration = add(new NumberSetting("Duration", 0.15, 0.05, 0.5, 0.01));
    private final BoolSetting sound = add(new BoolSetting("Sound", false));

    private long lastHit = -1;

    public HitMarker() {
        super("Hit Marker", "Shows an on-screen marker when you hit an entity", Category.PLAYERS);
    }

    @Override
    public void onAttack(Entity entity, Vec3d pos) {
        lastHit = System.nanoTime();
        if (sound.get()) {
            try {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.6f));
            } catch (Throwable ignored) {
                // ba'zi versiyalarda sound event qiymati boshqacha bo'lishi mumkin; shovqinsiz o'tkazib yuboriladi
            }
        }
    }

    @Override
    public void onHudRender(DrawContext ctx) {
        if (lastHit < 0) return;
        float age = (System.nanoTime() - lastHit) / 1.0E9f;
        float dur = duration.getF();
        if (age >= dur) { lastHit = -1; return; }
        float a = 1f - age / dur;
        int c = ColorUtils.mulAlpha(color.getColor(), a);
        int cx = mc.getWindow().getScaledWidth() / 2, cy = mc.getWindow().getScaledHeight() / 2;
        int s = size.getI();
        ctx.fill(cx - s, cy - s, cx + s, cy - s + 1, c);
        ctx.fill(cx - s, cy - s, cx - s + 1, cy + s, c);
        ctx.fill(cx - s, cy + s - 1, cx + s, cy + s, c);
        ctx.fill(cx + s - 1, cy - s, cx + s, cy + s, c);
        for (int i = -s; i <= s; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c);
            ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, c);
        }
    }
}

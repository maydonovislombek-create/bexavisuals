package com.bexa.visuals.mixin;

import com.bexa.visuals.module.visuals.CustomCrosshair;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /** Maxsus nishon yoqilganda standart nishonni yashiradi. */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void bexa$hideCrosshair(CallbackInfo ci) {
        if (CustomCrosshair.hideVanilla) ci.cancel();
    }
}

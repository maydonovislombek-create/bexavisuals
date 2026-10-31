package com.bexa.visuals.mixin;

import com.bexa.visuals.module.visuals.NoRender;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    /** No Weather: yomg'ir/qor vizualini chizmaydi (ob-havoning o'ziga ta'sir qilmaydi). */
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void bexa$noWeather(CallbackInfo ci) {
        if (NoRender.weather) ci.cancel();
    }
}

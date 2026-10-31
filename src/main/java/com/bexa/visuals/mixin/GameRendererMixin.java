package com.bexa.visuals.mixin;

import com.bexa.visuals.module.visuals.AspectRatio;
import com.bexa.visuals.module.visuals.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /** Aspect Ratio: proyeksiya matritsasining X o'qini masshtablaydi. */
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void bexa$aspect(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        float target = AspectRatio.target;
        if (target <= 0f) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        int w = mc.getWindow().getFramebufferWidth(), h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;
        float cur = w / (float) h;
        Matrix4f m = new Matrix4f(cir.getReturnValue());
        m.m00(m.m00() * cur / target);
        cir.setReturnValue(m);
    }

    /** No Hurt Cam: zarba olganda kameraning silkinishini o'chiradi. */
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void bexa$noHurtCam(CallbackInfo ci) {
        if (NoRender.hurtCam) ci.cancel();
    }
}

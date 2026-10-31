package com.bexa.visuals.mixin;

import com.bexa.visuals.module.visuals.NoRender;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {

    /** No Fire Overlay: ekrandagi olov qatlamini chizmaydi. */
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void bexa$noFire(CallbackInfo ci) {
        if (NoRender.fire) ci.cancel();
    }
}

package com.bexa.visuals.mixin;

import com.bexa.visuals.module.visuals.FullBright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {

    /** Full Bright: gamma sozlamasi o'qilganda juda yuqori qiymat qaytaradi. */
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true, require = 0)
    private void bexa$gamma(CallbackInfoReturnable<T> cir) {
        if (!FullBright.gamma) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if ((Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) Double.valueOf(FullBright.GAMMA));
        }
    }
}

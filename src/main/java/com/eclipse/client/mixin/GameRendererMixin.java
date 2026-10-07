package com.eclipse.client.mixin;

import com.eclipse.client.EclipseClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void eclipse$zoom(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValueF() * EclipseClient.zoomFactor());
    }
}

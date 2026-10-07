package com.eclipse.client.mixin;

import com.eclipse.client.EclipseClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void eclipse$freelook(CallbackInfo ci) {
        if (EclipseClient.freelook) setRotation(EclipseClient.flYaw, EclipseClient.flPitch);
    }
}

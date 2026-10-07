package com.eclipse.client.mixin;

import com.eclipse.client.EclipseClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    // While freelook is held, mouse movement turns the camera only, not the player.
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void eclipse$freelookMouse(double dx, double dy, CallbackInfo ci) {
        if (EclipseClient.freelook && (Object) this == MinecraftClient.getInstance().player) {
            EclipseClient.flYaw += (float) dx * 0.15f;
            EclipseClient.flPitch = MathHelper.clamp(EclipseClient.flPitch + (float) dy * 0.15f, -90f, 90f);
            ci.cancel();
        }
    }
}

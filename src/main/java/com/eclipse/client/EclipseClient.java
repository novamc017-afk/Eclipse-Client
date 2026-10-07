package com.eclipse.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class EclipseClient implements ClientModInitializer {
    public static final String ID = "eclipse";

    // All three appear in Options > Controls > Key Binds under "Eclipse Client" (rebindable).
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(ID, "main"));
    public static KeyBinding MENU, ZOOM_KEY, FREELOOK_KEY;

    // zoom
    private static boolean zooming = false;
    private static float zoom = 1f;
    private static long lastNanos = System.nanoTime();

    // freelook (read by mixins)
    public static boolean freelook = false;
    public static float flYaw, flPitch;

    // stats (read by HUD)
    public static double speed = 0, reach = 0;
    public static int combo = 0;
    public static LivingEntity target;
    public static long targetTime;
    private static double lastX, lastZ;

    @Override
    public void onInitializeClient() {
        Modules.load();

        MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eclipse.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eclipse.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        FREELOOK_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.eclipse.freelook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(ID, "hud"), Hud::render);

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (ctx, tc) -> {
            if (Modules.CROSSHAIR.enabled) Hud.crosshair(ctx);
            else original.render(ctx, tc);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                combo++;
                if (entity instanceof LivingEntity le) { target = le; targetTime = System.currentTimeMillis(); }
                Vec3d eye = player.getEyePos();
                Vec3d end = eye.add(player.getRotationVec(1f).multiply(8));
                Optional<Vec3d> p = entity.getBoundingBox().raycast(eye, end);
                reach = p.map(v -> v.distanceTo(eye)).orElse((double) player.distanceTo(entity));
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        while (MENU.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new MenuScreen());
        }

        zooming = Modules.ZOOM.enabled && mc.currentScreen == null && ZOOM_KEY.isPressed();

        boolean wantFree = Modules.FREELOOK.enabled && mc.player != null
                && mc.currentScreen == null && FREELOOK_KEY.isPressed();
        if (wantFree && !freelook) { flYaw = mc.player.getYaw(); flPitch = mc.player.getPitch(); }
        freelook = wantFree;

        if (mc.player == null) return;

        speed = Math.hypot(mc.player.getX() - lastX, mc.player.getZ() - lastZ) * 20.0;
        lastX = mc.player.getX();
        lastZ = mc.player.getZ();

        if (mc.player.hurtTime > 0) combo = 0;

        if (Modules.AUTO_SPRINT.enabled && mc.currentScreen == null
                && mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
                && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }

        if (Modules.TOGGLE_SNEAK.enabled && !mc.options.getSneakToggled().getValue()) {
            mc.options.getSneakToggled().setValue(true);
        }

        if (Modules.CLEAR_WEATHER.enabled && mc.world != null) {
            mc.world.setRainGradient(0f);
            mc.world.setThunderGradient(0f);
        }
    }

    /** Multiplier applied to FOV by the GameRenderer mixin, eased for a smooth zoom. */
    public static float zoomFactor() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        zoom = Gfx.smooth(zoom, zooming ? 0.23f : 1f, dt, 12f);
        return zoom;
    }
}

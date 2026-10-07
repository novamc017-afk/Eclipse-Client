package com.eclipse.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class Hud {
    private Hud() {}

    /** True while the HUD editor is open: empty modules show placeholders so they can be dragged. */
    public static boolean editing = false;

    private static final Deque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    private static boolean lastL, lastR;
    private static final float[] KEY_ANIM = new float[6];
    private static long lastNanos = System.nanoTime();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final int PANEL = 0x0B0B10;
    private static final int ACCENT = 0x7C5CFF;

    private static int cps(Deque<Long> q) {
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static void pollClicks(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !lastL) LEFT.addLast(System.currentTimeMillis());
        if (r && !lastR) RIGHT.addLast(System.currentTimeMillis());
        lastL = l; lastR = r;
    }

    public static void crosshair(DrawContext ctx) {
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        int c = 0xFFFFFFFF, gap = 3, len = 5;
        ctx.fill(cx - gap - len, cy - 1, cx - gap, cy + 1, c);
        ctx.fill(cx + gap, cy - 1, cx + gap + len, cy + 1, c);
        ctx.fill(cx - 1, cy - gap - len, cx + 1, cy - gap, c);
        ctx.fill(cx - 1, cy + gap, cx + 1, cy + gap + len, c);
    }

    private static int count(MinecraftClient mc, Item... items) {
        int n = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            for (Item it : items) if (s.isOf(it)) n += s.getCount();
        }
        return n;
    }

    /** Text lines for text-type modules; null/empty = nothing to show. */
    private static List<String> lines(MinecraftClient mc, Modules.Module m) {
        var p = mc.player;
        switch (m.id) {
            case "fps": return List.of("FPS  " + mc.getCurrentFps());
            case "cps": return List.of("CPS  " + cps(LEFT) + " | " + cps(RIGHT));
            case "ping": {
                PlayerListEntry e = mc.getNetworkHandler() == null ? null
                        : mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                return List.of("Ping  " + (e == null ? 0 : e.getLatency()) + " ms");
            }
            case "coords": return List.of(String.format("XYZ  %.0f %.0f %.0f", p.getX(), p.getY(), p.getZ()));
            case "direction": {
                String d = p.getHorizontalFacing().asString();
                return List.of("Facing  " + Character.toUpperCase(d.charAt(0)) + d.substring(1));
            }
            case "rotation": return List.of(String.format("Yaw/Pitch  %.1f / %.1f", p.getYaw() % 360f, p.getPitch()));
            case "speed": return List.of(String.format("Speed  %.2f b/s", EclipseClient.speed));
            case "light": return mc.world == null ? null
                    : List.of("Light  " + mc.world.getLightLevel(p.getBlockPos()));
            case "day": return mc.world == null ? null
                    : List.of("Day  " + (mc.world.getTimeOfDay() / 24000L));
            case "saturation": return List.of(String.format("Food  %d  Sat  %.1f",
                    p.getHungerManager().getFoodLevel(), p.getHungerManager().getSaturationLevel()));
            case "health": return List.of(String.format("Health  %.1f", p.getHealth())
                    + (p.getAbsorptionAmount() > 0 ? String.format(" + %.1f", p.getAbsorptionAmount()) : ""));
            case "movement": return List.of(p.isSneaking() ? "Sneaking" : p.isSprinting() ? "Sprinting" : "Walking");
            case "clock": return List.of("Time  " + LocalTime.now().format(TIME));
            case "memory": {
                Runtime rt = Runtime.getRuntime();
                return List.of("Mem  " + ((rt.totalMemory() - rt.freeMemory()) >> 20) + " / " + (rt.maxMemory() >> 20) + " MB");
            }
            case "server": return List.of("Server  " + (mc.getCurrentServerEntry() == null
                    ? "Singleplayer" : mc.getCurrentServerEntry().address));
            case "combo": return List.of("Combo  " + EclipseClient.combo);
            case "reach": return List.of(String.format("Reach  %.2f", EclipseClient.reach));
            case "cooldown": return List.of(String.format("Cooldown  %d%%", Math.round(p.getAttackCooldownProgress(0f) * 100f)));
            case "target": {
                LivingEntity t = EclipseClient.target;
                if (t == null || !t.isAlive() || System.currentTimeMillis() - EclipseClient.targetTime > 8000) return null;
                return List.of(t.getName().getString(), String.format("%.1f / %.0f HP", t.getHealth(), t.getMaxHealth()));
            }
            case "items": {
                ItemStack held = p.getMainHandStack();
                if (held.isEmpty()) return null;
                return List.of(held.getName().getString() + "  x" + count(mc, held.getItem()));
            }
            case "pearls": return List.of("Pearls  " + count(mc, Items.ENDER_PEARL));
            case "gaps": return List.of("Apples  " + count(mc, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE));
            case "totems": return List.of("Totems  " + count(mc, Items.TOTEM_OF_UNDYING));
            case "pots": return List.of("Pots  " + count(mc, Items.SPLASH_POTION));
            case "arrows": return List.of("Arrows  " + count(mc, Items.ARROW));
            case "crystals": return List.of("Crystals  " + count(mc, Items.END_CRYSTAL));
            case "potions": {
                List<String> out = new ArrayList<>();
                for (StatusEffectInstance e : p.getStatusEffects()) {
                    String name = e.getEffectType().value().getName().getString();
                    int d = e.getDuration();
                    String time = e.isInfinite() ? "**" : String.format("%d:%02d", d / 1200, (d / 20) % 60);
                    out.add(name + " " + (e.getAmplifier() + 1) + "  " + time);
                }
                return out;
            }
            default: return null;
        }
    }

    private static void panel(DrawContext ctx, TextRenderer tr, List<String> lines, int x, int y, int w, int h) {
        Gfx.roundRect(ctx, x, y, w, h, 4, Gfx.argb(0.62f, PANEL));
        ctx.fill(x, y + 4, x + 2, y + h - 4, Gfx.argb(1f, ACCENT));
        int ty = y + 4;
        for (String s : lines) {
            ctx.drawTextWithShadow(tr, s, x + 7, ty, 0xFFEDEDF5);
            ty += 11;
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || (mc.options.hudHidden && !editing)) return;
        TextRenderer tr = mc.textRenderer;

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        pollClicks(mc);

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int flowY = 6;

        for (Modules.Module m : Modules.ALL) {
            if (!m.enabled || !m.isHud()) continue;

            int w, h;
            List<String> lines = null;
            if (m == Modules.KEYSTROKES) { w = 72; h = 70; }
            else if (m == Modules.ARMOR) { w = 24; h = 83; }
            else {
                lines = lines(mc, m);
                if (lines == null || lines.isEmpty()) {
                    if (!editing) continue;
                    lines = List.of(m.name);
                }
                int tw = 0;
                for (String s : lines) tw = Math.max(tw, tr.getWidth(s));
                w = tw + 12;
                h = lines.size() * 11 + 7;
            }

            int x, y;
            if (m.px >= 0) { x = Math.round(m.px * sw); y = Math.round(m.py * sh); }
            else if (m == Modules.POTIONS) { x = sw - 6 - w; y = 6; }
            else if (m == Modules.ARMOR) { x = sw - 30; y = sh / 2 - 42; }
            else { x = 6; y = flowY; flowY += h + 4; }
            x = clamp(x, 0, Math.max(0, sw - w));
            y = clamp(y, 0, Math.max(0, sh - h));
            m.bx = x; m.by = y; m.bw = w; m.bh = h;

            if (m == Modules.KEYSTROKES) {
                boolean[] down = {
                        mc.options.forwardKey.isPressed(), mc.options.leftKey.isPressed(),
                        mc.options.backKey.isPressed(), mc.options.rightKey.isPressed(), lastL, lastR};
                String[] names = {"W", "A", "S", "D", "LMB", "RMB"};
                int[][] pos = {{25, 0, 22}, {0, 24, 22}, {25, 24, 22}, {50, 24, 22}, {0, 48, 35}, {37, 48, 35}};
                for (int i = 0; i < 6; i++) {
                    KEY_ANIM[i] = Gfx.smooth(KEY_ANIM[i], down[i] ? 1f : 0f, dt, 22f);
                    int kx = x + pos[i][0], ky = y + pos[i][1], kw = pos[i][2];
                    int col = Gfx.lerpRgb(PANEL, ACCENT, KEY_ANIM[i]);
                    Gfx.roundRect(ctx, kx, ky, kw, 22, 4, Gfx.argb(0.62f + 0.3f * KEY_ANIM[i], col));
                    ctx.drawTextWithShadow(tr, names[i], kx + (kw - tr.getWidth(names[i])) / 2, ky + 7, 0xFFFFFFFF);
                }
            } else if (m == Modules.ARMOR) {
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                Gfx.roundRect(ctx, x, y, w, h, 4, Gfx.argb(0.62f, PANEL));
                for (int i = 0; i < 4; i++) {
                    ItemStack st = mc.player.getEquippedStack(slots[i]);
                    if (st.isEmpty()) continue;
                    ctx.drawItem(st, x + 4, y + 3 + i * 20);
                    ctx.drawStackOverlay(tr, st, x + 4, y + 3 + i * 20);
                }
            } else {
                panel(ctx, tr, lines, x, y, w, h);
            }
        }
    }
}

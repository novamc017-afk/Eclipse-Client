package com.eclipse.client;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Modules {
    public static final String[] CATEGORIES = {"HUD", "PvP", "Gameplay", "Visual"};

    public static final class Module {
        public final String id, name, desc, category;
        public boolean enabled;
        /** Custom HUD position as a fraction of the screen (-1 = default layout). */
        public float px = -1, py = -1;
        /** Last drawn bounds in scaled GUI pixels (used by the HUD editor). */
        public int bx, by, bw, bh;
        Module(String id, String name, String desc, String category, boolean enabled) {
            this.id = id; this.name = name; this.desc = desc; this.category = category; this.enabled = enabled;
        }
        public boolean isHud() { return category.equals("HUD") || category.equals("PvP"); }
    }

    public static final List<Module> ALL = new ArrayList<>();

    // ---- HUD ----
    public static final Module FPS        = add("fps", "FPS", "Shows your frame rate", "HUD", true);
    public static final Module PING       = add("ping", "Ping", "Server latency", "HUD", true);
    public static final Module COORDS     = add("coords", "Coordinates", "Your XYZ position", "HUD", true);
    public static final Module DIRECTION  = add("direction", "Direction", "Facing direction", "HUD", true);
    public static final Module ROTATION   = add("rotation", "Rotation", "Yaw and pitch", "HUD", false);
    public static final Module CLOCK      = add("clock", "Clock", "Real-world time", "HUD", false);
    public static final Module MEMORY     = add("memory", "Memory", "Java memory usage", "HUD", false);
    public static final Module SERVER     = add("server", "Server IP", "Current server address", "HUD", false);
    public static final Module DAY        = add("day", "Day Counter", "In-game day number", "HUD", false);
    public static final Module SPEED      = add("speed", "Speedometer", "Blocks per second", "HUD", false);
    public static final Module LIGHT      = add("light", "Light Level", "Light at your position", "HUD", false);
    public static final Module SATURATION = add("saturation", "Saturation", "Food and saturation", "HUD", false);
    public static final Module HEALTH     = add("health", "Health", "Health and absorption", "HUD", false);
    public static final Module MOVEMENT   = add("movement", "Movement Status", "Sprinting / sneaking state", "HUD", false);

    // ---- PvP ----
    public static final Module CPS        = add("cps", "CPS", "Clicks per second", "PvP", true);
    public static final Module COMBO      = add("combo", "Combo Counter", "Hits since last damage taken", "PvP", false);
    public static final Module REACH      = add("reach", "Reach Display", "Distance of your last hit", "PvP", false);
    public static final Module TARGET     = add("target", "Target Info", "Name and health of last hit", "PvP", false);
    public static final Module COOLDOWN   = add("cooldown", "Attack Cooldown", "Weapon charge percent", "PvP", false);
    public static final Module KEYSTROKES = add("keystrokes", "Keystrokes", "WASD and mouse overlay", "PvP", true);
    public static final Module ARMOR      = add("armor", "Armor Status", "Armor and durability", "PvP", true);
    public static final Module POTIONS    = add("potions", "Potion Status", "Active effects and timers", "PvP", true);
    public static final Module ITEMS      = add("items", "Item Counter", "Held item total in inventory", "PvP", false);
    public static final Module PEARLS     = add("pearls", "Pearl Counter", "Ender pearls in inventory", "PvP", false);
    public static final Module GAPS       = add("gaps", "Apple Counter", "Golden apples in inventory", "PvP", false);
    public static final Module TOTEMS     = add("totems", "Totem Counter", "Totems of undying", "PvP", false);
    public static final Module POTS       = add("pots", "Potion Counter", "Splash potions in inventory", "PvP", false);
    public static final Module ARROWS     = add("arrows", "Arrow Counter", "Arrows in inventory", "PvP", false);
    public static final Module CRYSTALS   = add("crystals", "Crystal Counter", "End crystals in inventory", "PvP", false);

    // ---- Gameplay ----
    public static final Module AUTO_SPRINT  = add("autosprint", "Auto Sprint", "Sprint while moving forward", "Gameplay", false);
    public static final Module TOGGLE_SNEAK = add("togglesneak", "Toggle Sneak", "Tap sneak to toggle it", "Gameplay", false);
    public static final Module ZOOM         = add("zoom", "Zoom", "Smooth zoom (hold zoom key)", "Gameplay", true);
    public static final Module FREELOOK     = add("freelook", "Freelook", "Look around without turning", "Gameplay", true);

    // ---- Visual ----
    public static final Module CROSSHAIR     = add("crosshair", "Custom Crosshair", "Clean gap crosshair", "Visual", false);
    public static final Module CLEAR_WEATHER = add("clearweather", "Clear Weather", "Hides rain and thunder", "Visual", false);

    private static Module add(String id, String name, String desc, String cat, boolean def) {
        Module m = new Module(id, name, desc, cat, def);
        ALL.add(m);
        return m;
    }

    public static List<Module> inCategory(String cat) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category.equals(cat)) out.add(m);
        return out;
    }

    public static void resetPositions() {
        for (Module m : ALL) { m.px = -1; m.py = -1; }
    }

    private static final class Cfg {
        Map<String, Boolean> enabled = new HashMap<>();
        Map<String, float[]> pos = new HashMap<>();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("eclipse.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Cfg c = new Gson().fromJson(Files.readString(f), Cfg.class);
            if (c == null) return;
            for (Module m : ALL) {
                if (c.enabled != null && c.enabled.containsKey(m.id)) m.enabled = c.enabled.get(m.id);
                if (c.pos != null && c.pos.containsKey(m.id)) {
                    float[] p = c.pos.get(m.id);
                    if (p != null && p.length == 2) { m.px = p[0]; m.py = p[1]; }
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Cfg c = new Cfg();
            for (Module m : ALL) {
                c.enabled.put(m.id, m.enabled);
                if (m.px >= 0) c.pos.put(m.id, new float[]{m.px, m.py});
            }
            Files.writeString(file(), new Gson().toJson(c));
        } catch (Exception ignored) {}
    }
}

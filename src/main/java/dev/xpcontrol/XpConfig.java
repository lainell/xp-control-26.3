package dev.xpcontrol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-mob XP rules, stored in config/xpcontrol.json as { "minecraft:zombie": "x2", "minecraft:cow": "=10", "*": "x1.5" }.
 *
 *  - "x2" or "2"  : multiply the vanilla XP by 2 (fractions allowed, e.g. "0.5")
 *  - "=10"        : always drop exactly 10 XP
 *  - key "*"      : default rule for every mob without its own rule
 */
public final class XpConfig {
    public static final String GLOBAL = "*";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> VALUES = new TreeMap<>();

    private XpConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("xpcontrol.json");
    }

    public static synchronized void load() {
        Path file = file();
        if (!Files.exists(file)) return;
        try {
            Map<String, String> read = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, String>>() {}.getType());
            VALUES.clear();
            if (read != null) VALUES.putAll(read);
        } catch (Exception e) {
            XpControl.LOGGER.error("Could not read xpcontrol.json", e);
        }
    }

    public static synchronized void save() {
        try {
            Files.writeString(file(), GSON.toJson(VALUES));
        } catch (IOException e) {
            XpControl.LOGGER.error("Could not write xpcontrol.json", e);
        }
    }

    public static synchronized String get(String id) {
        return VALUES.getOrDefault(id, "");
    }

    public static synchronized void set(String id, String value) {
        if (value == null || value.isBlank()) {
            VALUES.remove(id);
        } else {
            VALUES.put(id, value.trim());
        }
    }

    /** Returns the XP that should actually drop, given what vanilla would drop. */
    public static synchronized int apply(String id, int vanilla) {
        String rule = VALUES.get(id);
        if (rule == null || rule.isBlank()) rule = VALUES.get(GLOBAL);
        if (rule == null || rule.isBlank()) return vanilla;

        try {
            String r = rule.trim().replace(',', '.');
            if (r.startsWith("=")) {
                return Math.max(0, (int) Math.round(Double.parseDouble(r.substring(1).trim())));
            }
            if (r.startsWith("x") || r.startsWith("X") || r.startsWith("×")) {
                r = r.substring(1).trim();
            }
            double result = vanilla * Double.parseDouble(r);
            if (result <= 0) return 0;
            int whole = (int) Math.floor(result);
            double fraction = result - whole;
            // Fractional XP is rounded randomly so that 0.5x of 5 averages 2.5.
            return whole + (ThreadLocalRandom.current().nextDouble() < fraction ? 1 : 0);
        } catch (NumberFormatException e) {
            return vanilla; // bad input in the menu: leave vanilla behaviour
        }
    }
}

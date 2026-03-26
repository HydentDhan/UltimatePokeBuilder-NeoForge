package com.ultimatepokebuilder.util;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class CoinsEngineHook {

    // Magic reflection method to parse PAPI tags in a Forge Mod!
    public static String parsePAPI(UUID uuid, String text) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object offlinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class).invoke(null, uuid);
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papiClass.getMethod("setPlaceholders", Class.forName("org.bukkit.OfflinePlayer"), String.class).invoke(null, offlinePlayer, text);
        } catch (Exception e) {
            return text; // Returns raw string if PlaceholderAPI isn't installed
        }
    }

    // Now uses PAPI instead of SQLite! Unbreakable and never locks!
    public static int getBalance(ServerPlayer sp, String currency) {
        try {
            // Uses CoinsEngine's native raw balance placeholder
            String parsed = parsePAPI(sp.getUUID(), "%coinsengine_balance_raw_" + currency + "%");
            return (int) Double.parseDouble(parsed);
        } catch (Exception e) {
            return 0; // Failsafe if the player has no balance yet
        }
    }

    public static boolean takeBalance(ServerPlayer sp, String currency, int amount) {
        if (getBalance(sp, currency) < amount) return false;

        // Execute the Spigot command safely from the server console!
        String cmd = "coinsengine take " + sp.getGameProfile().getName() + " " + currency + " " + amount;
        sp.server.getCommands().performPrefixedCommand(sp.server.createCommandSourceStack(), cmd);
        return true;
    }
}
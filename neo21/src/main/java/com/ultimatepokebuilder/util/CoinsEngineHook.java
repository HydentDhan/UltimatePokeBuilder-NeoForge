package com.ultimatepokebuilder.util;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import com.ultimatepokebuilder.config.Config;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class CoinsEngineHook {

    public static String parsePAPI(UUID uuid, String text) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object player = bukkitClass.getMethod("getPlayer", UUID.class).invoke(null, uuid);
            if (player == null) {
                player = bukkitClass.getMethod("getOfflinePlayer", UUID.class).invoke(null, uuid);
            }

            Object pluginManager = bukkitClass.getMethod("getPluginManager").invoke(null);
            Object papiPlugin = pluginManager.getClass().getMethod("getPlugin", String.class).invoke(pluginManager, "PlaceholderAPI");

            if (papiPlugin != null) {
                ClassLoader pluginLoader = papiPlugin.getClass().getClassLoader();
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, pluginLoader);
                Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");

                return (String) papiClass.getMethod("setPlaceholders", offlinePlayerClass, String.class).invoke(null, player, text);
            }
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("Failed to cross ClassLoader boundary for PAPI", e);
        }
        return text;
    }

    public static int getBalance(ServerPlayer sp, String currency) {
        try {

            boolean isSpecial = currency.equals(Config.SERVER.currencySpecial.get());
            String papiTag = isSpecial ? Config.SERVER.ecoCheckPapiSpecial.get() : Config.SERVER.ecoCheckPapiStandard.get();

            String parsed = parsePAPI(sp.getUUID(), papiTag);

            if (parsed.contains("%")) {
                UltimatePokeBuilder.LOGGER.warn("PAPI string did not parse correctly: " + parsed);
                return 0;
            }

            parsed = parsed.replaceAll("[^\\d.]", "");
            return (int) Double.parseDouble(parsed);

        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("Failed to read PAPI balance for " + currency, e);
            return 0;
        }
    }

    public static boolean takeBalance(ServerPlayer sp, String currency, int amount) {
        if (getBalance(sp, currency) < amount) return false;


        boolean isSpecial = currency.equals(Config.SERVER.currencySpecial.get());
        String cmdTemplate = isSpecial ? Config.SERVER.ecoTakeCmdSpecial.get() : Config.SERVER.ecoTakeCmdStandard.get();

        String cmd = cmdTemplate
                .replace("%player%", sp.getGameProfile().getName())
                .replace("%amount%", String.valueOf(amount));

        sp.server.getCommands().performPrefixedCommand(sp.server.createCommandSourceStack(), cmd);
        return true;
    }
}
package com.ultimatepokebuilder.config;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class Config {
    private static Map<String, Object> data;
    public static final Server SERVER = new Server();

    public static void loadConfig(File configDir) {
        File configFile = new File(configDir, "UltimatePokeBuilder/config.yml");

        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        if (!configFile.exists()) {
            try (InputStream in = Config.class.getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    UltimatePokeBuilder.LOGGER.error("Default config.yml not found in resources folder!");
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                UltimatePokeBuilder.LOGGER.error("Failed to generate default config.yml", e);
            }
        }

        try (InputStream in = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            data = yaml.load(in);
            SERVER.reload();
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("Failed to load config.yml. Make sure formatting is correct!", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object get(String path) {
        if (data == null) return null;
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = data;

        for (int i = 0; i < keys.length - 1; i++) {
            Object obj = currentMap.get(keys[i]);
            if (obj instanceof Map) {
                currentMap = (Map<String, Object>) obj;
            } else {
                return null;
            }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    public static class ConfigValue<T> {
        private T value;
        private final String path;
        private final T def;

        public ConfigValue(String path, T def) {
            this.path = path;
            this.def = def;
            this.value = def;
        }

        public T get() { return value; }

        @SuppressWarnings("unchecked")
        public void update() {
            Object val = Config.get(path);
            if (val != null) {
                if (def instanceof Integer && val instanceof Number) {
                    this.value = (T) Integer.valueOf(((Number) val).intValue());
                } else if (def instanceof Double && val instanceof Number) {
                    this.value = (T) Double.valueOf(((Number) val).doubleValue());
                } else {
                    this.value = (T) val;
                }
            } else {
                this.value = def;
            }
        }
    }

    public static class Server {
        // --- Separated Economy Variables ---
        public final ConfigValue<String> currencyStandard = new ConfigValue<>("economy-integration.currencyStandard", "tokens");
        public final ConfigValue<String> currencySpecial = new ConfigValue<>("economy-integration.currencySpecial", "shards");
        public final ConfigValue<String> ecoTakeCmdStandard = new ConfigValue<>("economy-integration.ecoTakeCmdStandard", "tokens take %player% %amount%");
        public final ConfigValue<String> ecoTakeCmdSpecial = new ConfigValue<>("economy-integration.ecoTakeCmdSpecial", "shards take %player% %amount%");
        public final ConfigValue<String> ecoCheckPapiStandard = new ConfigValue<>("economy-integration.ecoCheckPapiStandard", "%coinsengine_balance_raw_tokens%");
        public final ConfigValue<String> ecoCheckPapiSpecial = new ConfigValue<>("economy-integration.ecoCheckPapiSpecial", "%coinsengine_balance_raw_shards%");

        public final ConfigValue<String> webhookTokens = new ConfigValue<>("audit-logging.webhookTokens", "");
        public final ConfigValue<String> webhookShards = new ConfigValue<>("audit-logging.webhookShards", "");

        public final ConfigValue<List<String>> blacklistSpecs = new ConfigValue<>("general-options.blacklistSpecs", List.of("ditto"));
        public final ConfigValue<List<String>> specialSpecs = new ConfigValue<>("general-options.specialSpecs", List.of("palette:special"));
        public final ConfigValue<Double> shinyMultiplier = new ConfigValue<>("general-options.shinyMultiplier", 2.0);

        public final ConfigValue<Integer> shinyCost = new ConfigValue<>("pricing-standard.shinyCost", 200);
        public final ConfigValue<Integer> abilityCost = new ConfigValue<>("pricing-standard.abilityCost", 100);
        public final ConfigValue<Integer> hiddenAbilityCost = new ConfigValue<>("pricing-standard.hiddenAbilityCost", 200);
        public final ConfigValue<Integer> costPerLevel = new ConfigValue<>("pricing-standard.costPerLevel", 10);
        public final ConfigValue<Integer> evCost = new ConfigValue<>("pricing-standard.evCost", 2);
        public final ConfigValue<Integer> ivCost = new ConfigValue<>("pricing-standard.ivCost", 10);
        public final ConfigValue<Integer> natureCost = new ConfigValue<>("pricing-standard.natureCost", 50);
        public final ConfigValue<Integer> genderCost = new ConfigValue<>("pricing-standard.genderCost", 50);
        public final ConfigValue<Integer> growthCost = new ConfigValue<>("pricing-standard.growthCost", 50);
        public final ConfigValue<Integer> ballCost = new ConfigValue<>("pricing-standard.ballCost", 50);
        public final ConfigValue<Integer> untradeableCost = new ConfigValue<>("pricing-standard.untradeableCost", 50);
        public final ConfigValue<Integer> unbreedableCost = new ConfigValue<>("pricing-standard.unbreedableCost", 50);

        public final ConfigValue<Integer> shardsShinyCost = new ConfigValue<>("pricing-special.shardsShinyCost", 10);
        public final ConfigValue<Integer> shardsAbilityCost = new ConfigValue<>("pricing-special.shardsAbilityCost", 5);
        public final ConfigValue<Integer> shardsHiddenAbilityCost = new ConfigValue<>("pricing-special.shardsHiddenAbilityCost", 8);
        public final ConfigValue<Integer> shardsCostPerLevel = new ConfigValue<>("pricing-special.shardsCostPerLevel", 2);
        public final ConfigValue<Integer> shardsEvCost = new ConfigValue<>("pricing-special.shardsEvCost", 1);
        public final ConfigValue<Integer> shardsIvCost = new ConfigValue<>("pricing-special.shardsIvCost", 2);
        public final ConfigValue<Integer> shardsNatureCost = new ConfigValue<>("pricing-special.shardsNatureCost", 5);
        public final ConfigValue<Integer> shardsGenderCost = new ConfigValue<>("pricing-special.shardsGenderCost", 5);
        public final ConfigValue<Integer> shardsGrowthCost = new ConfigValue<>("pricing-special.shardsGrowthCost", 5);
        public final ConfigValue<Integer> shardsBallCost = new ConfigValue<>("pricing-special.shardsBallCost", 5);

        public final ConfigValue<List<Integer>> partySlots = new ConfigValue<>("ui-layout.partySlots", List.of(10, 11, 12, 14, 15, 16));
        public final ConfigValue<Integer> infoSlot = new ConfigValue<>("ui-layout.infoSlot", 4);
        public final ConfigValue<String> infoMaterial = new ConfigValue<>("ui-layout.infoMaterial", "minecraft:book");
        public final ConfigValue<String> infoName = new ConfigValue<>("ui-layout.infoName", "§e§lInformation");
        public final ConfigValue<List<String>> infoLore = new ConfigValue<>("ui-layout.infoLore", List.of("§7Welcome to UltimatePokeBuilder!", "§7Select a Pokemon to edit its stats.", " ", "§7Tokens: §e%coinsengine_balance_tokens%", "§7Shards: §d%coinsengine_balance_shards%"));

        public final ConfigValue<Integer> slotPokemon = new ConfigValue<>("ui-layout.slotPokemon", 4);
        public final ConfigValue<Integer> slotShiny = new ConfigValue<>("ui-layout.slotShiny", 19);
        public final ConfigValue<Integer> slotLevel = new ConfigValue<>("ui-layout.slotLevel", 20);
        public final ConfigValue<Integer> slotAbility = new ConfigValue<>("ui-layout.slotAbility", 21);
        public final ConfigValue<Integer> slotNature = new ConfigValue<>("ui-layout.slotNature", 22);
        public final ConfigValue<Integer> slotGrowth = new ConfigValue<>("ui-layout.slotGrowth", 23);
        public final ConfigValue<Integer> slotGender = new ConfigValue<>("ui-layout.slotGender", 24);
        public final ConfigValue<Integer> slotBall = new ConfigValue<>("ui-layout.slotBall", 25);
        public final ConfigValue<Integer> slotEvs = new ConfigValue<>("ui-layout.slotEvs", 29);
        public final ConfigValue<Integer> slotIvs = new ConfigValue<>("ui-layout.slotIvs", 30);
        public final ConfigValue<Integer> slotUntradeable = new ConfigValue<>("ui-layout.slotUntradeable", 32);
        public final ConfigValue<Integer> slotUnbreedable = new ConfigValue<>("ui-layout.slotUnbreedable", 33);
        public final ConfigValue<Integer> slotBack = new ConfigValue<>("ui-layout.slotBack", 49);

        public void reload() {
            currencyStandard.update(); currencySpecial.update();
            ecoTakeCmdStandard.update(); ecoTakeCmdSpecial.update();
            ecoCheckPapiStandard.update(); ecoCheckPapiSpecial.update();

            webhookTokens.update(); webhookShards.update(); blacklistSpecs.update(); specialSpecs.update(); shinyMultiplier.update();

            shinyCost.update(); abilityCost.update(); hiddenAbilityCost.update(); costPerLevel.update();
            evCost.update(); ivCost.update(); natureCost.update(); genderCost.update(); growthCost.update();
            ballCost.update(); untradeableCost.update(); unbreedableCost.update();

            shardsShinyCost.update(); shardsAbilityCost.update(); shardsHiddenAbilityCost.update(); shardsCostPerLevel.update();
            shardsEvCost.update(); shardsIvCost.update(); shardsNatureCost.update(); shardsGenderCost.update(); shardsGrowthCost.update(); shardsBallCost.update();

            partySlots.update(); infoSlot.update(); infoMaterial.update(); infoName.update(); infoLore.update();
            slotPokemon.update(); slotShiny.update(); slotLevel.update(); slotAbility.update(); slotNature.update();
            slotGrowth.update(); slotGender.update(); slotBall.update(); slotEvs.update(); slotIvs.update();
            slotUntradeable.update(); slotUnbreedable.update(); slotBack.update();
        }
    }
}
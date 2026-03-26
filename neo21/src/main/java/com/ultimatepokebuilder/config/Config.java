package com.ultimatepokebuilder.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;

public class Config {
    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class Server {
        public final ModConfigSpec.BooleanValue useMySQL;
        public final ModConfigSpec.ConfigValue<String> dbHost;
        public final ModConfigSpec.IntValue dbPort;
        public final ModConfigSpec.ConfigValue<String> dbName;
        public final ModConfigSpec.ConfigValue<String> dbUser;
        public final ModConfigSpec.ConfigValue<String> dbPass;
        public final ModConfigSpec.ConfigValue<String> dbTable;

        public final ModConfigSpec.ConfigValue<String> currencyStandard;
        public final ModConfigSpec.ConfigValue<String> currencySpecial;

        public final ModConfigSpec.ConfigValue<String> webhookTokens;
        public final ModConfigSpec.ConfigValue<String> webhookShards;

        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistSpecs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> specialSpecs;
        public final ModConfigSpec.DoubleValue shinyMultiplier;


        public final ModConfigSpec.IntValue shinyCost;
        public final ModConfigSpec.IntValue abilityCost;
        public final ModConfigSpec.IntValue hiddenAbilityCost;
        public final ModConfigSpec.IntValue costPerLevel;
        public final ModConfigSpec.IntValue evCost;
        public final ModConfigSpec.IntValue ivCost;
        public final ModConfigSpec.IntValue natureCost;
        public final ModConfigSpec.IntValue genderCost;
        public final ModConfigSpec.IntValue growthCost;
        public final ModConfigSpec.IntValue ballCost;
        public final ModConfigSpec.IntValue untradeableCost;
        public final ModConfigSpec.IntValue unbreedableCost;

        public final ModConfigSpec.IntValue mythicShinyCost;
        public final ModConfigSpec.IntValue mythicAbilityCost;
        public final ModConfigSpec.IntValue mythicHiddenAbilityCost;
        public final ModConfigSpec.IntValue mythicCostPerLevel;
        public final ModConfigSpec.IntValue mythicEvCost;
        public final ModConfigSpec.IntValue mythicIvCost;
        public final ModConfigSpec.IntValue mythicNatureCost;
        public final ModConfigSpec.IntValue mythicGenderCost;
        public final ModConfigSpec.IntValue mythicGrowthCost;
        public final ModConfigSpec.IntValue mythicBallCost;

        public final ModConfigSpec.IntValue shardsShinyCost;
        public final ModConfigSpec.IntValue shardsAbilityCost;
        public final ModConfigSpec.IntValue shardsHiddenAbilityCost;
        public final ModConfigSpec.IntValue shardsCostPerLevel;
        public final ModConfigSpec.IntValue shardsEvCost;
        public final ModConfigSpec.IntValue shardsIvCost;
        public final ModConfigSpec.IntValue shardsNatureCost;
        public final ModConfigSpec.IntValue shardsGenderCost;
        public final ModConfigSpec.IntValue shardsGrowthCost;
        public final ModConfigSpec.IntValue shardsBallCost;


        public final ModConfigSpec.ConfigValue<List<? extends Integer>> partySlots;
        public final ModConfigSpec.IntValue infoSlot;
        public final ModConfigSpec.ConfigValue<String> infoMaterial;
        public final ModConfigSpec.ConfigValue<String> infoName;
        public final ModConfigSpec.ConfigValue<List<? extends String>> infoLore;

        public final ModConfigSpec.IntValue slotPokemon;
        public final ModConfigSpec.IntValue slotShiny;
        public final ModConfigSpec.IntValue slotLevel;
        public final ModConfigSpec.IntValue slotAbility;
        public final ModConfigSpec.IntValue slotNature;
        public final ModConfigSpec.IntValue slotGrowth;
        public final ModConfigSpec.IntValue slotGender;
        public final ModConfigSpec.IntValue slotBall;
        public final ModConfigSpec.IntValue slotEvs;
        public final ModConfigSpec.IntValue slotIvs;
        public final ModConfigSpec.IntValue slotUntradeable;
        public final ModConfigSpec.IntValue slotUnbreedable;
        public final ModConfigSpec.IntValue slotBack;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("Database Setup (CoinsEngine)");
            useMySQL = builder.define("useMySQL", false);
            dbHost = builder.define("dbHost", "localhost");
            dbPort = builder.defineInRange("dbPort", 3306, 1, 65535);
            dbName = builder.define("dbName", "minecraft");
            dbUser = builder.define("dbUser", "root");
            dbPass = builder.define("dbPass", "");
            dbTable = builder.define("dbTable", "coinsengine_users");
            currencyStandard = builder.define("currencyStandard", "tokens");
            currencySpecial = builder.define("currencySpecial", "shards");
            builder.pop();

            builder.push("Audit Logging (Webhooks)");
            webhookTokens = builder.comment("URL for Token purchases").define("webhookTokens", "");
            webhookShards = builder.comment("URL for Shard purchases").define("webhookShards", "");
            builder.pop();

            builder.push("General Options");
            blacklistSpecs = builder.defineListAllowEmpty(List.of("blacklistSpecs"), () -> List.of("ditto"), o -> o instanceof String);
            specialSpecs = builder.defineListAllowEmpty(List.of("specialSpecs"), () -> List.of("palette:special"), o -> o instanceof String);
            shinyMultiplier = builder.defineInRange("shinyMultiplier", 2.0, 1.0, 100.0);
            builder.pop();

            builder.push("Standard Pricing (Tokens)");
            shinyCost = builder.defineInRange("shinyCost", 200, 0, Integer.MAX_VALUE);
            abilityCost = builder.defineInRange("abilityCost", 100, 0, Integer.MAX_VALUE);
            hiddenAbilityCost = builder.defineInRange("hiddenAbilityCost", 200, 0, Integer.MAX_VALUE);
            costPerLevel = builder.defineInRange("costPerLevel", 10, 0, Integer.MAX_VALUE);
            evCost = builder.defineInRange("evCost", 2, 0, Integer.MAX_VALUE);
            ivCost = builder.defineInRange("ivCost", 10, 0, Integer.MAX_VALUE);
            natureCost = builder.defineInRange("natureCost", 50, 0, Integer.MAX_VALUE);
            genderCost = builder.defineInRange("genderCost", 50, 0, Integer.MAX_VALUE);
            growthCost = builder.defineInRange("growthCost", 50, 0, Integer.MAX_VALUE);
            ballCost = builder.defineInRange("ballCost", 50, 0, Integer.MAX_VALUE);
            untradeableCost = builder.defineInRange("untradeableCost", 50, 0, Integer.MAX_VALUE);
            unbreedableCost = builder.defineInRange("unbreedableCost", 50, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.push("Mythic Pricing (Mythic Tokens)");
            mythicShinyCost = builder.defineInRange("mythicShinyCost", 5, 0, Integer.MAX_VALUE);
            mythicAbilityCost = builder.defineInRange("mythicAbilityCost", 2, 0, Integer.MAX_VALUE);
            mythicHiddenAbilityCost = builder.defineInRange("mythicHiddenAbilityCost", 4, 0, Integer.MAX_VALUE);
            mythicCostPerLevel = builder.defineInRange("mythicCostPerLevel", 1, 0, Integer.MAX_VALUE);
            mythicEvCost = builder.defineInRange("mythicEvCost", 1, 0, Integer.MAX_VALUE);
            mythicIvCost = builder.defineInRange("mythicIvCost", 1, 0, Integer.MAX_VALUE);
            mythicNatureCost = builder.defineInRange("mythicNatureCost", 1, 0, Integer.MAX_VALUE);
            mythicGenderCost = builder.defineInRange("mythicGenderCost", 1, 0, Integer.MAX_VALUE);
            mythicGrowthCost = builder.defineInRange("mythicGrowthCost", 1, 0, Integer.MAX_VALUE);
            mythicBallCost = builder.defineInRange("mythicBallCost", 1, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.push("Special Pricing (Shards)");
            shardsShinyCost = builder.defineInRange("shardsShinyCost", 10, 0, Integer.MAX_VALUE);
            shardsAbilityCost = builder.defineInRange("shardsAbilityCost", 5, 0, Integer.MAX_VALUE);
            shardsHiddenAbilityCost = builder.defineInRange("shardsHiddenAbilityCost", 8, 0, Integer.MAX_VALUE);
            shardsCostPerLevel = builder.defineInRange("shardsCostPerLevel", 2, 0, Integer.MAX_VALUE);
            shardsEvCost = builder.defineInRange("shardsEvCost", 1, 0, Integer.MAX_VALUE);
            shardsIvCost = builder.defineInRange("shardsIvCost", 2, 0, Integer.MAX_VALUE);
            shardsNatureCost = builder.defineInRange("shardsNatureCost", 5, 0, Integer.MAX_VALUE);
            shardsGenderCost = builder.defineInRange("shardsGenderCost", 5, 0, Integer.MAX_VALUE);
            shardsGrowthCost = builder.defineInRange("shardsGrowthCost", 5, 0, Integer.MAX_VALUE);
            shardsBallCost = builder.defineInRange("shardsBallCost", 5, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.push("UI Layout");
            partySlots = builder.comment("Slots for Party Pokemon 1-6 (0-26)").defineListAllowEmpty(List.of("partySlots"), () -> List.of(10, 11, 12, 14, 15, 16), o -> o instanceof Integer);
            infoSlot = builder.defineInRange("infoSlot", 4, 0, 53);
            infoMaterial = builder.define("infoMaterial", "minecraft:book");
            infoName = builder.define("infoName", "§e§lInformation");
            infoLore = builder.defineListAllowEmpty(List.of("infoLore"), () -> List.of("§7Welcome to UltimatePokeBuilder!", "§7Select a Pokemon to edit its stats."), o -> o instanceof String);

            slotPokemon = builder.comment("Slot for the Pokemon display icon").defineInRange("slotPokemon", 4, 0, 53);
            slotShiny = builder.defineInRange("slotShiny", 19, 0, 53);
            slotLevel = builder.defineInRange("slotLevel", 20, 0, 53);
            slotAbility = builder.defineInRange("slotAbility", 21, 0, 53);
            slotNature = builder.defineInRange("slotNature", 22, 0, 53);
            slotGrowth = builder.defineInRange("slotGrowth", 23, 0, 53);
            slotGender = builder.defineInRange("slotGender", 24, 0, 53);
            slotBall = builder.defineInRange("slotBall", 25, 0, 53);
            slotEvs = builder.defineInRange("slotEvs", 29, 0, 53);
            slotIvs = builder.defineInRange("slotIvs", 30, 0, 53);
            slotUntradeable = builder.defineInRange("slotUntradeable", 32, 0, 53);
            slotUnbreedable = builder.defineInRange("slotUnbreedable", 33, 0, 53);
            slotBack = builder.defineInRange("slotBack", 49, 0, 53);
            builder.pop();
        }
    }
}
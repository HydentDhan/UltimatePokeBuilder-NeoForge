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

        // NEW SECTION: Declare the webhooks here!
        public final ModConfigSpec.ConfigValue<String> webhookTokens;
        public final ModConfigSpec.ConfigValue<String> webhookShards;

        public final ModConfigSpec.ConfigValue<String> currencyStandard;
        public final ModConfigSpec.ConfigValue<String> currencySpecial;

        public final ModConfigSpec.ConfigValue<List<? extends String>> blacklistSpecs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> specialSpecs;
        public final ModConfigSpec.DoubleValue shinyMultiplier;

        // Standard Pricing (Tokens)
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

        // Mythic Pricing (Mythic Tokens)
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

        // Special Pricing (Shards)
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

        public Server(ModConfigSpec.Builder builder) {
            builder.push("Database Setup (CoinsEngine)");
            useMySQL = builder.define("useMySQL", false);
            dbHost = builder.define("dbHost", "localhost");
            dbPort = builder.defineInRange("dbPort", 3306, 1, 65535);
            dbName = builder.define("dbName", "minecraft");
            dbUser = builder.define("dbUser", "root");
            dbPass = builder.define("dbPass", "password");
            dbTable = builder.define("dbTable", "coinsengine_balances");

            currencyStandard = builder.define("currencyStandard", "tokens");
            currencySpecial = builder.define("currencySpecial", "shards");
            builder.pop();

            // NEW SECTION: Discord Webhooks
            builder.push("Audit Logging (Webhooks)");
            webhookTokens = builder.comment("Discord Webhook URL for Standard Token purchases").define("webhookTokens", "");
            webhookShards = builder.comment("Discord Webhook URL for Mythic/Special Shard purchases").define("webhookShards", "");
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
            evCost = builder.defineInRange("evCost", 100, 0, Integer.MAX_VALUE);
            ivCost = builder.defineInRange("ivCost", 100, 0, Integer.MAX_VALUE);
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
            shardsEvCost = builder.defineInRange("shardsEvCost", 2, 0, Integer.MAX_VALUE);
            shardsIvCost = builder.defineInRange("shardsIvCost", 2, 0, Integer.MAX_VALUE);
            shardsNatureCost = builder.defineInRange("shardsNatureCost", 5, 0, Integer.MAX_VALUE);
            shardsGenderCost = builder.defineInRange("shardsGenderCost", 5, 0, Integer.MAX_VALUE);
            shardsGrowthCost = builder.defineInRange("shardsGrowthCost", 5, 0, Integer.MAX_VALUE);
            shardsBallCost = builder.defineInRange("shardsBallCost", 5, 0, Integer.MAX_VALUE);
            builder.pop();
        }
    }
}
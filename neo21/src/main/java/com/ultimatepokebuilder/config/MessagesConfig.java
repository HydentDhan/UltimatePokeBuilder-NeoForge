package com.ultimatepokebuilder.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class MessagesConfig {

    public static final ModConfigSpec SPEC;
    public static final Messages MESSAGES;

    static {
        final Pair<Messages, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Messages::new);
        SPEC = specPair.getRight();
        MESSAGES = specPair.getLeft();
    }

    public static class Messages {
        public final ModConfigSpec.ConfigValue<String> prefix;
        public final ModConfigSpec.ConfigValue<String> successUpdate;
        public final ModConfigSpec.ConfigValue<String> insufficientTokens;
        public final ModConfigSpec.ConfigValue<String> blacklisted;
        public final ModConfigSpec.ConfigValue<String> alreadyHasTrait;
        public final ModConfigSpec.ConfigValue<String> tokensBalance;
        public final ModConfigSpec.ConfigValue<String> tokensPaid;
        public final ModConfigSpec.ConfigValue<String> tokensReceived;

        public Messages(ModConfigSpec.Builder builder) {
            builder.push("Chat Messages");

            prefix = builder.comment("Prefix before all UPB messages").define("prefix", "&8[&bUPB&8] ");

            successUpdate = builder.comment("Variables: %pokemon%, %cost%")
                    .define("successUpdate", "&aSuccessfully updated %pokemon% for &e%cost% Tokens&a!");

            insufficientTokens = builder.comment("When a player can't afford an upgrade")
                    .define("insufficientTokens", "&cUpdate failed. You need more tokens.");

            blacklisted = builder.comment("When they select a Ditto/Legendary")
                    .define("blacklisted", "&c&l(!) &cThat Pokémon is blacklisted and cannot be edited.");

            alreadyHasTrait = builder.comment("When they try to make a Shiny pokemon Shiny again")
                    .define("alreadyHasTrait", "&cThat Pokémon already has that trait.");

            tokensBalance = builder.comment("Variables: %balance%")
                    .define("tokensBalance", "&eYou have &6%balance% &etokens.");

            tokensPaid = builder.comment("Variables: %amount%, %target%")
                    .define("tokensPaid", "&aPaid &e%amount% &atokens to &e%target%&a.");

            tokensReceived = builder.comment("Variables: %amount%, %sender%")
                    .define("tokensReceived", "&aReceived &e%amount% &atokens from &e%sender%&a.");

            builder.pop();
        }
    }
}
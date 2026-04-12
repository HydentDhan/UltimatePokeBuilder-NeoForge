package com.ultimatepokebuilder.config;

import com.ultimatepokebuilder.UltimatePokeBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;

public class Language {
    private static Map<String, Object> data;
    public static final Strings STRINGS = new Strings();
    private static final int CURRENT_VERSION = 1;

    public static void loadLocale(File configDir) {
        File localeFile = new File(configDir, "UltimatePokeBuilder/locale.yml");

        if (!localeFile.getParentFile().exists()) {
            localeFile.getParentFile().mkdirs();
        }

        if (!localeFile.exists()) {
            extractDefaultLocale(localeFile);
        }

        loadYamlToMemory(localeFile);

        boolean requiresUpdate = false;
        if (data != null) {
            if (!data.containsKey("locale-version")) {
                requiresUpdate = true;
            } else {
                Object verObj = data.get("locale-version");
                if (verObj instanceof Number && ((Number) verObj).intValue() < CURRENT_VERSION) {
                    requiresUpdate = true;
                }
            }
        }

        if (requiresUpdate) {
            UltimatePokeBuilder.LOGGER.warn("Outdated locale.yml detected! Creating backup and generating fresh language file...");
            File backupFile = new File(configDir, "UltimatePokeBuilder/locale-backup-v" + (CURRENT_VERSION - 1) + "-" + System.currentTimeMillis() + ".yml");
            localeFile.renameTo(backupFile);
            extractDefaultLocale(localeFile);
            loadYamlToMemory(localeFile);
        }

        STRINGS.reload();
    }

    private static void extractDefaultLocale(File localeFile) {
        try (InputStream in = Language.class.getResourceAsStream("/upb-default-locale.yml")) {
            if (in != null) {
                Files.copy(in, localeFile.toPath());
            } else {
                UltimatePokeBuilder.LOGGER.error("Default locale not found in resources folder!");
                localeFile.createNewFile();
            }
        } catch (IOException e) {
            UltimatePokeBuilder.LOGGER.error("Failed to generate default locale.yml", e);
        }
    }

    private static void loadYamlToMemory(File localeFile) {
        try (InputStream in = new FileInputStream(localeFile)) {
            Yaml yaml = new Yaml();
            data = yaml.load(in);
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("Failed to load locale.yml.", e);
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
            } else { return null; }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    public static class LangValue<T> {
        private T value;
        private final String path;
        private final T def;

        public LangValue(String path, T def) {
            this.path = path;
            this.def = def;
            this.value = def;
        }

        public T get() { return value; }

        @SuppressWarnings("unchecked")
        public void update() {
            Object val = Language.get(path);
            if (val != null) {
                this.value = (T) val;
            } else {
                this.value = def;
            }
        }
    }

    public static class Strings {
        public final LangValue<String> msgUpgradeSuccess = new LangValue<>("messages.upgrade-success", "&#55FF55Upgrade successful!");
        public final LangValue<String> msgInsufficient = new LangValue<>("messages.insufficient-funds", "&#FF5555Insufficient %currency%!");
        public final LangValue<String> msgAlreadyHasTrait = new LangValue<>("messages.trait-exists", "&#FF5555Pokémon already has that trait!");
        public final LangValue<String> msgAlreadyHasNature = new LangValue<>("messages.nature-exists", "&#FF5555Pokémon already has that Nature!");
        public final LangValue<String> msgAlreadyHasGender = new LangValue<>("messages.gender-exists", "&#FF5555Pokémon already has that Gender!");
        public final LangValue<String> msgAlreadyHasBall = new LangValue<>("messages.ball-exists", "&#FF5555Pokémon is already in that Pokéball!");
        public final LangValue<String> msgAlreadyHasGrowth = new LangValue<>("messages.growth-exists", "&#FF5555Pokémon already has that Growth!");
        public final LangValue<String> msgError = new LangValue<>("messages.error-apply", "&#FF5555Error applying trait.");
        public final LangValue<String> msgRenameBlank = new LangValue<>("messages.rename-blank", "&#FF5555Name cannot be blank!");
        public final LangValue<String> msgRenameCancel = new LangValue<>("messages.rename-cancel", "&#FF5555Rename cancelled.");
        public final LangValue<String> msgRenamePrompt1 = new LangValue<>("messages.rename-prompt-1", "&#bPlease type the new name for your Pokémon in chat!");
        public final LangValue<String> msgRenamePrompt2 = new LangValue<>("messages.rename-prompt-2", "&#7(Supports standard &a and hex &#FF0000)");
        public final LangValue<String> msgRenamePrompt3 = new LangValue<>("messages.rename-prompt-3", "&#cType 'cancel' to abort.");
        public final LangValue<String> msgRenameDisabled = new LangValue<>("messages.rename-disabled", "&#FF5555This feature is currently in development and coming soon!");

        public final LangValue<String> titleParty = new LangValue<>("titles.party", "Select a Pokemon");
        public final LangValue<String> titleBuilder = new LangValue<>("titles.builder", "Builder");
        public final LangValue<String> titleConfirm = new LangValue<>("titles.confirm", "Confirm Upgrade");
        public final LangValue<String> titleLevel = new LangValue<>("titles.level", "Adjust Level");
        public final LangValue<String> titleAbility = new LangValue<>("titles.ability", "Select Ability");
        public final LangValue<String> titleNature = new LangValue<>("titles.nature", "Select Nature");
        public final LangValue<String> titleGrowth = new LangValue<>("titles.growth", "Select Growth");
        public final LangValue<String> titleGender = new LangValue<>("titles.gender", "Select Gender");
        public final LangValue<String> titleBall = new LangValue<>("titles.ball", "Select Ball");
        public final LangValue<String> titleEvs = new LangValue<>("titles.evs", "Select EV Stat");
        public final LangValue<String> titleIvs = new LangValue<>("titles.ivs", "Select IV Stat");

        public final LangValue<String> btnConfirm = new LangValue<>("buttons.confirm", "&#55FF55&lCONFIRM");
        public final LangValue<String> btnCancel = new LangValue<>("buttons.cancel", "&#FF5555&lCANCEL");
        public final LangValue<String> btnBack = new LangValue<>("buttons.back", "&#FF5555Back");
        public final LangValue<String> btnBackParty = new LangValue<>("buttons.back-party", "&#FF5555Back to Party");
        public final LangValue<String> btnBackBuilder = new LangValue<>("buttons.back-builder", "&#FF5555Back to Builder");

        public final LangValue<String> btnMakeShiny = new LangValue<>("buttons.make-shiny", "&#FFAA00Make Shiny");
        public final LangValue<String> btnRevertShiny = new LangValue<>("buttons.revert-shiny", "&#FFFF55Revert Shiny");
        public final LangValue<String> btnMakeTradeable = new LangValue<>("buttons.make-tradeable", "&#FFFF55Make Tradeable");
        public final LangValue<String> btnMakeUntradeable = new LangValue<>("buttons.make-untradeable", "&#FF5555Make Untradeable");
        public final LangValue<String> btnMakeBreedable = new LangValue<>("buttons.make-breedable", "&#FFFF55Make Breedable");
        public final LangValue<String> btnMakeUnbreedable = new LangValue<>("buttons.make-unbreedable", "&#FF5555Make Unbreedable");
        public final LangValue<String> btnLevel = new LangValue<>("buttons.level", "&#55FF55Adjust Level");
        public final LangValue<String> btnAbility = new LangValue<>("buttons.ability", "&#FFFF55Change Ability");
        public final LangValue<String> btnNature = new LangValue<>("buttons.nature", "&#55FFFFSet Nature");
        public final LangValue<String> btnGrowth = new LangValue<>("buttons.growth", "&#55AA00Set Growth");
        public final LangValue<String> btnGender = new LangValue<>("buttons.gender", "&#FF55FFSet Gender");
        public final LangValue<String> btnGenderless = new LangValue<>("buttons.genderless", "&#FF5555Genderless");
        public final LangValue<String> btnBall = new LangValue<>("buttons.ball", "&#FFFFFFSwap Pokeball");
        public final LangValue<String> btnEvs = new LangValue<>("buttons.evs", "&#FF5555Adjust EVs");
        public final LangValue<String> btnIvs = new LangValue<>("buttons.ivs", "&#55FF55Adjust IVs");
        public final LangValue<String> btnRename = new LangValue<>("buttons.rename", "&#FF5555&lRename Pokémon");

        public final LangValue<String> loreCost = new LangValue<>("lore.cost", "&#AAAAAACost: &#FFFF55%cost% %currency%");
        public final LangValue<String> loreCostLvl = new LangValue<>("lore.cost-lvl", "&#AAAAAACost per lvl: &#FFFF55%cost% %currency%");
        public final LangValue<String> loreCostEv = new LangValue<>("lore.cost-ev", "&#AAAAAACost per EV: &#FFFF55%cost% %currency%");
        public final LangValue<String> loreCostIv = new LangValue<>("lore.cost-iv", "&#AAAAAACost per IV: &#FFFF55%cost% %currency%");
        public final LangValue<String> lorePay = new LangValue<>("lore.pay", "&#AAAAAAPay %cost% %currency%");
        public final LangValue<String> loreGenderless = new LangValue<>("lore.genderless", "&#AAAAAAThis Pokémon has no gender.");
        public final LangValue<String> loreClickAbility = new LangValue<>("lore.click-ability", "&#AAAAAAClick to open Ability Menu");
        public final LangValue<String> loreClickNature = new LangValue<>("lore.click-nature", "&#AAAAAAClick to set Nature");
        public final LangValue<String> loreClickGrowth = new LangValue<>("lore.click-growth", "&#AAAAAAClick to set Growth");
        public final LangValue<String> loreClickBall = new LangValue<>("lore.click-ball", "&#AAAAAAClick to swap ball");
        public final LangValue<String> loreClickStat = new LangValue<>("lore.click-stat", "&#AAAAAASelect a stat to modify");
        public final LangValue<String> loreRenameSoon = new LangValue<>("lore.rename-soon", "Coming soon!");

        public void reload() {
            msgUpgradeSuccess.update(); msgInsufficient.update(); msgAlreadyHasTrait.update(); msgAlreadyHasNature.update(); msgAlreadyHasGender.update(); msgAlreadyHasBall.update(); msgAlreadyHasGrowth.update(); msgError.update(); msgRenameBlank.update(); msgRenameCancel.update(); msgRenamePrompt1.update(); msgRenamePrompt2.update(); msgRenamePrompt3.update(); msgRenameDisabled.update();
            titleParty.update(); titleBuilder.update(); titleConfirm.update(); titleLevel.update(); titleAbility.update(); titleNature.update(); titleGrowth.update(); titleGender.update(); titleBall.update(); titleEvs.update(); titleIvs.update();
            btnConfirm.update(); btnCancel.update(); btnBack.update(); btnBackParty.update(); btnBackBuilder.update(); btnMakeShiny.update(); btnRevertShiny.update(); btnMakeTradeable.update(); btnMakeUntradeable.update(); btnMakeBreedable.update(); btnMakeUnbreedable.update(); btnLevel.update(); btnAbility.update(); btnNature.update(); btnGrowth.update(); btnGender.update(); btnGenderless.update(); btnBall.update(); btnEvs.update(); btnIvs.update(); btnRename.update();
            loreCost.update(); loreCostLvl.update(); loreCostEv.update(); loreCostIv.update(); lorePay.update(); loreGenderless.update(); loreClickAbility.update(); loreClickNature.update(); loreClickGrowth.update(); loreClickBall.update(); loreClickStat.update(); loreRenameSoon.update();
        }
    }
}
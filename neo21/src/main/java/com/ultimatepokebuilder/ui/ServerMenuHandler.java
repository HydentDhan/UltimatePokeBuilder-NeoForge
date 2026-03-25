package com.ultimatepokebuilder.ui;

import com.pixelmonmod.api.Flags;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.ultimatepokebuilder.UltimatePokeBuilder;
import com.ultimatepokebuilder.config.Config;
import com.ultimatepokebuilder.util.CoinsEngineHook;
import com.ultimatepokebuilder.util.WebhookUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class ServerMenuHandler {

    private static ItemStack getFiller() {
        ItemStack item = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return item;
    }

    private static ItemStack createBtn(String id, String name, String lore) {
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        if (item.isEmpty()) item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null) item.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        return item;
    }

    public static String getCurrency(Pokemon p) {
        if (p.isLegendary() || p.isUltraBeast()) return Config.SERVER.currencySpecial.get();
        for (String spec : Config.SERVER.specialSpecs.get()) {
            try { if (PokemonSpecificationProxy.create(spec).get().matches(p)) return Config.SERVER.currencySpecial.get(); } catch (Exception ignored) {}
        }
        return Config.SERVER.currencyStandard.get();
    }

    public static int getCost(String type, Pokemon pkmn, String cur) {
        int base = 0;
        boolean isShards = cur.equals(Config.SERVER.currencySpecial.get());
        boolean isMythic = pkmn.isLegendary() || pkmn.isUltraBeast();

        switch(type) {
            case "shiny": base = isShards ? Config.SERVER.shardsShinyCost.get() : (isMythic ? Config.SERVER.mythicShinyCost.get() : Config.SERVER.shinyCost.get()); break;
            case "level": base = isShards ? Config.SERVER.shardsCostPerLevel.get() : (isMythic ? Config.SERVER.mythicCostPerLevel.get() : Config.SERVER.costPerLevel.get()); break;
            case "ev": base = isShards ? Config.SERVER.shardsEvCost.get() : (isMythic ? Config.SERVER.mythicEvCost.get() : Config.SERVER.evCost.get()); break;
            case "iv": base = isShards ? Config.SERVER.shardsIvCost.get() : (isMythic ? Config.SERVER.mythicIvCost.get() : Config.SERVER.ivCost.get()); break;
            case "ability": base = isShards ? Config.SERVER.shardsAbilityCost.get() : (isMythic ? Config.SERVER.mythicAbilityCost.get() : Config.SERVER.abilityCost.get()); break;
            case "hidden_ability": base = isShards ? Config.SERVER.shardsHiddenAbilityCost.get() : (isMythic ? Config.SERVER.mythicHiddenAbilityCost.get() : Config.SERVER.hiddenAbilityCost.get()); break;
            case "nature": base = isShards ? Config.SERVER.shardsNatureCost.get() : (isMythic ? Config.SERVER.mythicNatureCost.get() : Config.SERVER.natureCost.get()); break;
            case "gender": base = isShards ? Config.SERVER.shardsGenderCost.get() : (isMythic ? Config.SERVER.mythicGenderCost.get() : Config.SERVER.genderCost.get()); break;
            case "growth": base = isShards ? Config.SERVER.shardsGrowthCost.get() : (isMythic ? Config.SERVER.mythicGrowthCost.get() : Config.SERVER.growthCost.get()); break;
            case "ball": base = isShards ? Config.SERVER.shardsBallCost.get() : (isMythic ? Config.SERVER.mythicBallCost.get() : Config.SERVER.ballCost.get()); break;
            case "untradeable": base = Config.SERVER.untradeableCost.get(); break;
            case "unbreedable": base = Config.SERVER.unbreedableCost.get(); break;
        }
        return pkmn.isShiny() && !type.equals("shiny") ? (int)(base * Config.SERVER.shinyMultiplier.get()) : base;
    }

    public static void openPartyMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new PartyMenu(id, inv), Component.literal("Select a Pokemon")));
    }

    public static void openConfirm(ServerPlayer sp, int pSlot, String action, int cost, String cur) {
        sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ConfirmMenu(id, inv, pSlot, action, cost, cur), Component.literal("Confirm Upgrade"))));
    }

    public static void triggerSuccess(ServerPlayer sp, Pokemon pkmn, String action, int cost, String cur) {
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        sp.sendSystemMessage(Component.literal("§aUpgrade successful!"));

        String cleanAction = action.toUpperCase();
        String auditMsg = "[UPB Audit] " + sp.getName().getString() + " spent " + cost + " " + cur + " to apply [" + cleanAction + "] to " + pkmn.getLocalizedName();
        UltimatePokeBuilder.LOGGER.info(auditMsg);

        String url = cur.equals(Config.SERVER.currencySpecial.get()) ? Config.SERVER.webhookShards.get() : Config.SERVER.webhookTokens.get();
        WebhookUtil.sendAudit(url, "💸 **UPB Purchase:** `" + sp.getName().getString() + "` spent **" + cost + " " + cur.toUpperCase() + "** on `" + pkmn.getLocalizedName() + "` (Action: **" + cleanAction + "**)");
    }

    public static void triggerFail(ServerPlayer sp, String cur) {
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        sp.sendSystemMessage(Component.literal("§cInsufficient " + cur.toUpperCase() + "!"));
    }

    // --- 1. PARTY MENU ---
    public static class PartyMenu extends ChestMenu {
        public PartyMenu(int id, Inventory inv) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());
            for (int i = 0; i < 6; i++) {
                Pokemon pkmn = StorageProxy.getPartyNow((ServerPlayer) inv.player).get(i);
                if (pkmn != null) {
                    ItemStack sprite = SpriteItemHelper.getPhoto(pkmn);
                    sprite.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lSelect " + pkmn.getLocalizedName()));
                    getContainer().setItem(10 + i, sprite);
                }
            }
        }
        @Override
        public void clicked(int slotId, int b, ClickType c, Player p) {
            if (slotId >= 10 && slotId <= 15) {
                ((ServerPlayer) p).server.execute(() -> p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, slotId - 10), Component.literal("Builder"))));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 2. MAIN BUILDER MENU ---
    public static class BuilderMenu extends ChestMenu {
        private final int pSlot;
        public BuilderMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x6, id, inv, new SimpleContainer(54), 6);
            this.pSlot = pSlot;
            for (int i = 0; i < 54; i++) getContainer().setItem(i, getFiller());

            Pokemon pkmn = StorageProxy.getPartyNow((ServerPlayer) inv.player).get(pSlot);
            if (pkmn == null) return;
            String cur = getCurrency(pkmn);

            String growthName = pkmn.getGrowth().unwrapKey().map(k -> k.location().getPath()).orElse("ordinary");
            growthName = growthName.substring(0, 1).toUpperCase() + growthName.substring(1);
            String ballName = pkmn.getBall().toString().replace("pixelmon:", "").replace("_", " ");
            ballName = ballName.substring(0, 1).toUpperCase() + ballName.substring(1);

            // The Fancy Color-Coded Hover Lore
            ItemStack sprite = SpriteItemHelper.getPhoto(pkmn);
            sprite.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l" + pkmn.getLocalizedName()));

            String ivs = "§a" + pkmn.getIVs().getStat(BattleStatsType.HP) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getIVs().getTotal() + "/186)";
            String evs = "§e" + pkmn.getEVs().getStat(BattleStatsType.HP) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getEVs().getTotal() + "/510)";

            sprite.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§8§m----------------------------------"),
                    Component.literal("§7Level: §b" + pkmn.getPokemonLevel() + "   §7Growth: §2" + growthName),
                    Component.literal("§7Nature: §a" + pkmn.getNature().getLocalizedName() + "   §7Ball: §f" + ballName),
                    Component.literal("§7Ability: §e" + pkmn.getAbility().getLocalizedName() + (pkmn.getAbilitySlot() == 2 ? " §d(HA)" : " §7(Normal)")),
                    Component.literal("§8§m----------------------------------"),
                    Component.literal("§7IVs (HP/Atk/Def/SpA/SpD/Spe):"),
                    Component.literal(ivs),
                    Component.literal("§7EVs (HP/Atk/Def/SpA/SpD/Spe):"),
                    Component.literal(evs),
                    Component.literal("§8§m----------------------------------"),
                    Component.literal("§7Your Balance: §e" + CoinsEngineHook.getBalance(inv.player.getUUID(), cur) + " " + cur.toUpperCase())
            )));
            getContainer().setItem(4, sprite);

            // Re-Arranged UI Layout for Better Look
            getContainer().setItem(19, createBtn("pixelmon:shiny_stone", "§6Make Shiny", "§7Cost: " + getCost("shiny", pkmn, cur)));
            getContainer().setItem(20, createBtn("pixelmon:rare_candy", "§aAdjust Level", "§7Cost per lvl: " + getCost("level", pkmn, cur)));
            getContainer().setItem(21, createBtn("pixelmon:ability_capsule", "§eChange Ability", "§7Click to open Ability Menu"));
            getContainer().setItem(22, createBtn("pixelmon:adamant_mint", "§bSet Nature", "§7Cost: " + getCost("nature", pkmn, cur)));
            getContainer().setItem(23, createBtn("minecraft:slime_block", "§2Set Growth", "§7Cost: " + getCost("growth", pkmn, cur)));
            getContainer().setItem(24, createBtn("minecraft:pink_dye", "§dSet Gender", "§7Cost: " + getCost("gender", pkmn, cur)));
            getContainer().setItem(25, createBtn("pixelmon:poke_ball", "§fSwap Pokeball", "§7Cost: " + getCost("ball", pkmn, cur)));

            getContainer().setItem(29, createBtn("pixelmon:hp_up", "§cAdjust EVs", "§7Cost per EV: " + getCost("ev", pkmn, cur)));
            getContainer().setItem(30, createBtn("pixelmon:calcium", "§aAdjust IVs", "§7Cost per IV: " + getCost("iv", pkmn, cur)));
            getContainer().setItem(32, createBtn("minecraft:barrier", "§cUntradeable", "§7Cost: " + getCost("untradeable", pkmn, cur)));
            getContainer().setItem(33, createBtn("minecraft:iron_bars", "§cUnbreedable", "§7Cost: " + getCost("unbreedable", pkmn, cur)));

            getContainer().setItem(49, createBtn("minecraft:arrow", "§cBack to Party", null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);

            if (slot == 49) sp.server.execute(() -> openPartyMenu(sp));
            if (slot == 20) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AdjustMenu(id, inv, pSlot, "level", null), Component.literal("Adjust Level"))));
            if (slot == 21) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AbilityMenu(id, inv, pSlot), Component.literal("Select Ability"))));
            if (slot == 22) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new NatureMenu(id, inv, pSlot), Component.literal("Select Nature"))));
            if (slot == 23) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GrowthMenu(id, inv, pSlot), Component.literal("Select Growth"))));
            if (slot == 24) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GenderMenu(id, inv, pSlot), Component.literal("Select Gender"))));
            if (slot == 25) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BallMenu(id, inv, pSlot), Component.literal("Select Ball"))));

            if (slot == 29) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new StatSelectMenu(id, inv, pSlot, "ev"), Component.literal("Select EV Stat"))));
            if (slot == 30) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new StatSelectMenu(id, inv, pSlot, "iv"), Component.literal("Select IV Stat"))));

            if (slot == 19) openConfirm(sp, pSlot, "shiny", getCost("shiny", pkmn, cur), cur);
            if (slot == 32) openConfirm(sp, pSlot, "untradeable", getCost("untradeable", pkmn, cur), cur);
            if (slot == 33) openConfirm(sp, pSlot, "unbreedable", getCost("unbreedable", pkmn, cur), cur);
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 3. ABILITY MENU (Safely Handles Pokemon with 1 Ability) ---
    public static class AbilityMenu extends ChestMenu {
        private final int pSlot;

        public AbilityMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            Pokemon pkmn = StorageProxy.getPartyNow((ServerPlayer) inv.player).get(pSlot);
            String cur = getCurrency(pkmn);

            String ab1 = "None", ab2 = "None", ha = "None";
            try {
                var normalAbs = pkmn.getForm().getAbilities().getAbilities();
                if (normalAbs.length > 0 && normalAbs[0] != null) ab1 = normalAbs[0].getName();
                if (normalAbs.length > 1 && normalAbs[1] != null) ab2 = normalAbs[1].getName();
                var hiddenAbs = pkmn.getForm().getAbilities().getHiddenAbilities();
                if (hiddenAbs.length > 0 && hiddenAbs[0] != null) ha = hiddenAbs[0].getName();
            } catch (Exception e) {}

            if (!ab1.equals("None")) getContainer().setItem(11, createBtn("pixelmon:ability_capsule", "§eAbility 1: §f" + ab1, "§7Cost: " + getCost("ability", pkmn, cur)));
            if (!ab2.equals("None")) getContainer().setItem(13, createBtn("pixelmon:ability_capsule", "§eAbility 2: §f" + ab2, "§7Cost: " + getCost("ability", pkmn, cur)));
            if (!ha.equals("None"))  getContainer().setItem(15, createBtn("pixelmon:ability_patch", "§dHidden Ability: §f" + ha, "§7Cost: " + getCost("hidden_ability", pkmn, cur)));

            getContainer().setItem(26, createBtn("minecraft:barrier", "§cBack to Builder", null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);

            try {
                if (slot == 11 && pkmn.getForm().getAbilities().getAbilities().length > 0) openConfirm(sp, pSlot, "ability:0", getCost("ability", pkmn, cur), cur);
                if (slot == 13 && pkmn.getForm().getAbilities().getAbilities().length > 1) openConfirm(sp, pSlot, "ability:1", getCost("ability", pkmn, cur), cur);
                if (slot == 15 && pkmn.getForm().getAbilities().getHiddenAbilities().length > 0) openConfirm(sp, pSlot, "ability:2", getCost("hidden_ability", pkmn, cur), cur);
            } catch (Exception ignored) {}
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 4. STAT SELECT MENU (For EVs and IVs) ---
    public static class StatSelectMenu extends ChestMenu {
        private final int pSlot;
        private final String mode;

        public StatSelectMenu(int id, Inventory inv, int pSlot, String mode) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            this.mode = mode;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            String title = mode.equals("ev") ? "§eAdjust EVs" : "§aAdjust IVs";
            getContainer().setItem(4, createBtn("minecraft:book", title, "§7Select a stat to modify"));

            getContainer().setItem(10, createBtn("minecraft:golden_apple", "§cHP", null));
            getContainer().setItem(11, createBtn("minecraft:iron_sword", "§cAttack", null));
            getContainer().setItem(12, createBtn("minecraft:iron_chestplate", "§eDefense", null));
            getContainer().setItem(14, createBtn("minecraft:blaze_powder", "§9Sp. Atk", null));
            getContainer().setItem(15, createBtn("minecraft:shield", "§aSp. Def", null));
            getContainer().setItem(16, createBtn("minecraft:feather", "§bSpeed", null));

            getContainer().setItem(26, createBtn("minecraft:barrier", "§cBack to Builder", null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }

            BattleStatsType stat = null;
            if (slot == 10) stat = BattleStatsType.HP;
            if (slot == 11) stat = BattleStatsType.ATTACK;
            if (slot == 12) stat = BattleStatsType.DEFENSE;
            if (slot == 14) stat = BattleStatsType.SPECIAL_ATTACK;
            if (slot == 15) stat = BattleStatsType.SPECIAL_DEFENSE;
            if (slot == 16) stat = BattleStatsType.SPEED;

            if (stat != null) {
                final BattleStatsType finalStat = stat;
                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AdjustMenu(id, inv, pSlot, mode, finalStat), Component.literal("Adjust Stat"))));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 5. UNIVERSAL ADJUST MENU (Handles Levels, EVs, and IVs) ---
    public static class AdjustMenu extends ChestMenu {
        private final int pSlot;
        private final String mode;
        private final BattleStatsType stat;
        private int addedValues = 0;

        public AdjustMenu(int id, Inventory inv, int pSlot, String mode, BattleStatsType stat) {
            super(MenuType.GENERIC_9x4, id, inv, new SimpleContainer(36), 4);
            this.pSlot = pSlot;
            this.mode = mode;
            this.stat = stat;
            refresh(inv.player);
        }

        private void refresh(Player p) {
            for (int i = 0; i < 36; i++) getContainer().setItem(i, getFiller());
            Pokemon pkmn = StorageProxy.getPartyNow((ServerPlayer) p).get(pSlot);
            String cur = getCurrency(pkmn);

            int totalCost = addedValues * getCost(mode, pkmn, cur);
            int currentVal = mode.equals("level") ? pkmn.getPokemonLevel() : (mode.equals("ev") ? pkmn.getEVs().getStat(stat) : pkmn.getIVs().getStat(stat));
            String statName = mode.equals("level") ? "Level" : stat.getLocalizedName();

            getContainer().setItem(10, createBtn("minecraft:red_wool", "§c-50", null));
            getContainer().setItem(11, createBtn("minecraft:red_terracotta", "§c-10", null));
            getContainer().setItem(12, createBtn("minecraft:red_stained_glass", "§c-1", null));

            getContainer().setItem(13, createBtn("pixelmon:rare_candy", "§bTarget " + statName + ": §f" + (currentVal + addedValues), "§7Total Cost: §e" + totalCost + " " + cur));

            getContainer().setItem(14, createBtn("minecraft:green_stained_glass", "§a+1", null));
            getContainer().setItem(15, createBtn("minecraft:green_terracotta", "§a+10", null));
            getContainer().setItem(16, createBtn("minecraft:green_wool", "§a+50", null));

            getContainer().setItem(30, createBtn("minecraft:emerald_block", "§a§lCONFIRM", "§7Pay " + totalCost));
            getContainer().setItem(32, createBtn("minecraft:barrier", "§c§lCANCEL", "§7Back to Builder"));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);

            int currentVal = mode.equals("level") ? pkmn.getPokemonLevel() : (mode.equals("ev") ? pkmn.getEVs().getStat(stat) : pkmn.getIVs().getStat(stat));
            int maxPossible = mode.equals("level") ? 100 : (mode.equals("ev") ? 252 : 31);
            int maxAllowed = maxPossible - currentVal;

            if (mode.equals("ev")) {
                int remainingTotalEVs = 510 - pkmn.getEVs().getTotal();
                maxAllowed = Math.min(maxAllowed, remainingTotalEVs);
            }

            if (slot == 10) addedValues = Math.max(0, addedValues - 50);
            if (slot == 11) addedValues = Math.max(0, addedValues - 10);
            if (slot == 12) addedValues = Math.max(0, addedValues - 1);
            if (slot == 14) addedValues = Math.min(maxAllowed, addedValues + 1);
            if (slot == 15) addedValues = Math.min(maxAllowed, addedValues + 10);
            if (slot == 16) addedValues = Math.min(maxAllowed, addedValues + 50);

            if (slot == 32) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder"))));

            if (slot == 30 && addedValues > 0) {
                String cur = getCurrency(pkmn);
                int totalCost = addedValues * getCost(mode, pkmn, cur);
                if (CoinsEngineHook.takeBalance(sp.getUUID(), cur, totalCost)) {
                    if (mode.equals("level")) pkmn.setLevel(currentVal + addedValues);
                    else if (mode.equals("ev")) pkmn.getEVs().setStat(stat, currentVal + addedValues);
                    else if (mode.equals("iv")) pkmn.getIVs().setStat(stat, currentVal + addedValues);

                    triggerSuccess(sp, pkmn, "+" + addedValues + " " + mode.toUpperCase(), totalCost, cur);
                    sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder"))));
                } else {
                    triggerFail(sp, cur);
                }
            }
            refresh(p);
            this.broadcastChanges();
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 6. SELECTION MENUS (Nature, Gender, Growth, Ball) ---
    public static class NatureMenu extends ChestMenu {
        private final int pSlot;
        public NatureMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x4, id, inv, new SimpleContainer(36), 4);
            this.pSlot = pSlot;
            for (int i = 0; i < 36; i++) getContainer().setItem(i, getFiller());
            String[] natures = {"Hardy","Lonely","Brave","Adamant","Naughty","Bold","Docile","Relaxed","Impish","Lax","Timid","Hasty","Serious","Jolly","Naive","Modest","Mild","Quiet","Bashful","Rash","Calm","Gentle","Sassy","Careful","Quirky"};
            for (int i = 0; i < natures.length; i++) getContainer().setItem(i, createBtn("pixelmon:" + natures[i].toLowerCase() + "_mint", "§b" + natures[i], null));
            getContainer().setItem(35, createBtn("minecraft:barrier", "§cBack", null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 35) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            String[] natures = {"Hardy","Lonely","Brave","Adamant","Naughty","Bold","Docile","Relaxed","Impish","Lax","Timid","Hasty","Serious","Jolly","Naive","Modest","Mild","Quiet","Bashful","Rash","Calm","Gentle","Sassy","Careful","Quirky"};
            if (slot >= 0 && slot < natures.length) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "nature:" + natures[slot], getCost("nature", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    public static class GenderMenu extends ChestMenu {
        private final int pSlot;
        public GenderMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());
            getContainer().setItem(11, createBtn("minecraft:light_blue_wool", "§bMale", null));
            getContainer().setItem(15, createBtn("minecraft:pink_wool", "§dFemale", null));
            getContainer().setItem(26, createBtn("minecraft:barrier", "§cBack", null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);
            if (slot == 11) openConfirm(sp, pSlot, "gender:Male", getCost("gender", pkmn, cur), cur);
            if (slot == 15) openConfirm(sp, pSlot, "gender:Female", getCost("gender", pkmn, cur), cur);
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    public static class GrowthMenu extends ChestMenu {
        private final int pSlot;
        public GrowthMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());
            String[] growths = {"Microscopic", "Pygmy", "Runt", "Small", "Ordinary", "Huge", "Giant", "Enormous", "Ginormous"};
            for (int i = 0; i < growths.length; i++) getContainer().setItem(9 + i, createBtn("minecraft:slime_block", "§2" + growths[i], null));
            getContainer().setItem(26, createBtn("minecraft:barrier", "§cBack", null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            String[] growths = {"Microscopic", "Pygmy", "Runt", "Small", "Ordinary", "Huge", "Giant", "Enormous", "Ginormous"};
            if (slot >= 9 && slot < 9 + growths.length) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "growth:" + growths[slot - 9], getCost("growth", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    public static class BallMenu extends ChestMenu {
        private final int pSlot;
        public BallMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x4, id, inv, new SimpleContainer(36), 4);
            this.pSlot = pSlot;
            for (int i = 0; i < 36; i++) getContainer().setItem(i, getFiller());
            String[] balls = {"poke_ball", "great_ball", "ultra_ball", "master_ball", "premier_ball", "heal_ball", "net_ball", "nest_ball", "dive_ball", "dusk_ball", "timer_ball", "quick_ball", "repeat_ball", "safari_ball", "fast_ball", "level_ball", "lure_ball", "heavy_ball", "love_ball", "friend_ball", "moon_ball", "sport_ball", "park_ball", "dream_ball", "beast_ball", "cherish_ball"};
            for (int i = 0; i < Math.min(balls.length, 35); i++) getContainer().setItem(i, createBtn("pixelmon:" + balls[i], "§f" + balls[i].replace("_", " ").toUpperCase(), null));
            getContainer().setItem(35, createBtn("minecraft:barrier", "§cBack", null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 35) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            String[] balls = {"poke_ball", "great_ball", "ultra_ball", "master_ball", "premier_ball", "heal_ball", "net_ball", "nest_ball", "dive_ball", "dusk_ball", "timer_ball", "quick_ball", "repeat_ball", "safari_ball", "fast_ball", "level_ball", "lure_ball", "heavy_ball", "love_ball", "friend_ball", "moon_ball", "sport_ball", "park_ball", "dream_ball", "beast_ball", "cherish_ball"};
            if (slot >= 0 && slot < Math.min(balls.length, 35)) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "ball:" + balls[slot], getCost("ball", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }

    // --- 7. THE UNIVERSAL CONFIRM MENU ---
    public static class ConfirmMenu extends ChestMenu {
        private final int pSlot;
        private final String action;
        private final int finalCost;
        private final String cur;

        public ConfirmMenu(int id, Inventory inv, int pSlot, String action, int finalCost, String cur) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            this.action = action;
            this.finalCost = finalCost;
            this.cur = cur;

            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());
            getContainer().setItem(11, createBtn("minecraft:emerald_block", "§a§lCONFIRM", "§7Cost: §e" + finalCost + " " + cur));
            getContainer().setItem(15, createBtn("minecraft:barrier", "§c§lCANCEL", "§7Back to Builder"));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 15) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder")))); return; }
            if (slot == 11) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);

                if (action.equals("shiny") && pkmn.isShiny() ||
                        action.equals("untradeable") && pkmn.hasFlag(Flags.UNTRADEABLE) ||
                        action.equals("unbreedable") && pkmn.hasFlag(Flags.UNBREEDABLE) ||
                        (action.startsWith("ability:") && pkmn.getAbilitySlot() == Integer.parseInt(action.split(":")[1]))) {
                    sp.sendSystemMessage(Component.literal("§cPokémon already has that trait!"));
                    return;
                }

                if (CoinsEngineHook.takeBalance(sp.getUUID(), cur, finalCost)) {
                    try {
                        if (action.equals("shiny")) pkmn.setShiny(true);
                        else if (action.startsWith("ability:")) pkmn.setAbilitySlot(Integer.parseInt(action.split(":")[1]));
                        else if (action.equals("untradeable")) pkmn.addFlag(Flags.UNTRADEABLE);
                        else if (action.equals("unbreedable")) pkmn.addFlag(Flags.UNBREEDABLE);
                        else if (action.startsWith("nature:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);
                        else if (action.startsWith("gender:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);
                        else if (action.startsWith("growth:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);
                        else if (action.startsWith("ball:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);

                        triggerSuccess(sp, pkmn, action, finalCost, cur);
                    } catch (Exception e) {
                        sp.sendSystemMessage(Component.literal("§cError applying trait."));
                        UltimatePokeBuilder.LOGGER.error("Failed to apply UPB trait", e);
                    }
                } else {
                    triggerFail(sp, cur);
                }

                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), Component.literal("Builder"))));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
    }
}
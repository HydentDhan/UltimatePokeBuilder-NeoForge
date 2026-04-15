package com.ultimatepokebuilder.ui;

import com.pixelmonmod.api.Flags;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.ultimatepokebuilder.UltimatePokeBuilder;
import com.ultimatepokebuilder.config.Config;
import com.ultimatepokebuilder.config.Language;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class ServerMenuHandler {

    public static final Map<UUID, Integer> activeRenames = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        activeRenames.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity pixelmon) {
            if (pixelmon.getPokemon() != null && pixelmon.getPokemon().hasNickname()) {
                pixelmon.setCustomNameVisible(true);
            }
        }
    }

    public static Component parseHexName(String input) {
        if (!input.contains("&#")) return Component.literal(input.replace("&", "§"));

        net.minecraft.network.chat.MutableComponent result = Component.empty();
        String[] parts = input.split("(?=&#)");

        for (String part : parts) {
            if (part.startsWith("&#") && part.length() >= 8) {
                String hex = "#" + part.substring(2, 8);
                String text = part.substring(8).replace("&", "§");
                try {
                    net.minecraft.network.chat.TextColor color = net.minecraft.network.chat.TextColor.parseColor(hex).getOrThrow();
                    result.append(Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color)));
                } catch (Exception e) {
                    result.append(Component.literal(part.replace("&", "§")));
                }
            } else {
                result.append(Component.literal(part.replace("&", "§")));
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        if (activeRenames.containsKey(sp.getUUID())) {
            event.setCanceled(true);
            int pSlot = activeRenames.remove(sp.getUUID());
            String input = event.getMessage().getString();

            if (input.trim().isEmpty()) {
                sp.sendSystemMessage(parseHexName(Language.STRINGS.msgRenameBlank.get()));
                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));
                return;
            }

            if (input.equalsIgnoreCase("cancel")) {
                sp.sendSystemMessage(parseHexName(Language.STRINGS.msgRenameCancel.get()));
                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));
                return;
            }

            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            if (pkmn == null) return;

            String cur = getCurrency(pkmn);
            int cost = getCost("rename", pkmn, cur);

            if (CoinsEngineHook.takeBalance(sp, cur, cost)) {
                Component formattedName = parseHexName(input);
                pkmn.setNickname(formattedName);

                pkmn.getPixelmonEntity().ifPresent(entity -> entity.setCustomNameVisible(true));

                triggerSuccess(sp, pkmn, "rename", cost, cur);
            } else {
                triggerFail(sp, cur);
            }

            sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));
        }
    }

    private static ItemStack getFiller() {
        ItemStack item = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        item.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return item;
    }

    private static ItemStack createBtn(String id, String name, String lore) {
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
        if (item.isEmpty()) item = new ItemStack(Items.PAPER);
        item.set(DataComponents.CUSTOM_NAME, parseHexName(name));
        if (lore != null) {
            List<Component> loreList = new ArrayList<>();
            for (String line : lore.split("\n")) {
                loreList.add(parseHexName(line));
            }
            item.set(DataComponents.LORE, new ItemLore(loreList));
        }
        return item;
    }

    private static ItemStack createBallBtn(String ballId, String name, String lore) {
        ItemStack item = null;
        try {
            Class<?> registryClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry");
            Object registryValue = registryClass.getMethod("getPokeBall", String.class).invoke(null, ballId);

            if (registryValue != null) {
                Object pokeBall = registryValue.getClass().getMethod("get").invoke(registryValue);
                if (pokeBall != null) {
                    try {
                        item = (ItemStack) pokeBall.getClass().getMethod("getItemStack").invoke(pokeBall);
                    } catch (Exception e1) {
                        try {
                            Object rawItem = pokeBall.getClass().getMethod("getItem").invoke(pokeBall);
                            item = new ItemStack((net.minecraft.world.item.Item) rawItem);
                        } catch (Exception e2) {
                            for (java.lang.reflect.Method m : pokeBall.getClass().getMethods()) {
                                if (m.getReturnType() == ItemStack.class && m.getParameterCount() == 0) {
                                    String mName = m.getName().toLowerCase();
                                    if (!mName.contains("base") && !mName.contains("lid") && !mName.contains("craft")) {
                                        item = (ItemStack) m.invoke(pokeBall);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            UltimatePokeBuilder.LOGGER.error("Failed to generate Pokeball from Registry: " + ballId, e);
        }

        if (item == null || item.isEmpty()) {
            item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("pixelmon:poke_ball")));
            if (item.isEmpty()) item = new ItemStack(Items.PAPER);
        }

        item.set(DataComponents.CUSTOM_NAME, parseHexName(name));
        if (lore != null) {
            List<Component> loreList = new ArrayList<>();
            for (String line : lore.split("\n")) {
                loreList.add(parseHexName(line));
            }
            item.set(DataComponents.LORE, new ItemLore(loreList));
        }
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

        switch(type) {
            case "shiny": base = isShards ? Config.SERVER.shardsShinyCost.get() : Config.SERVER.shinyCost.get(); break;
            case "level": base = isShards ? Config.SERVER.shardsCostPerLevel.get() : Config.SERVER.costPerLevel.get(); break;
            case "ev": base = isShards ? Config.SERVER.shardsEvCost.get() : Config.SERVER.evCost.get(); break;
            case "iv": base = isShards ? Config.SERVER.shardsIvCost.get() : Config.SERVER.ivCost.get(); break;
            case "ability": base = isShards ? Config.SERVER.shardsAbilityCost.get() : Config.SERVER.abilityCost.get(); break;
            case "hidden_ability": base = isShards ? Config.SERVER.shardsHiddenAbilityCost.get() : Config.SERVER.hiddenAbilityCost.get(); break;
            case "nature": base = isShards ? Config.SERVER.shardsNatureCost.get() : Config.SERVER.natureCost.get(); break;
            case "gender": base = isShards ? Config.SERVER.shardsGenderCost.get() : Config.SERVER.genderCost.get(); break;
            case "growth": base = isShards ? Config.SERVER.shardsGrowthCost.get() : Config.SERVER.growthCost.get(); break;
            case "ball": base = isShards ? Config.SERVER.shardsBallCost.get() : Config.SERVER.ballCost.get(); break;
            case "untradeable": base = isShards ? Config.SERVER.shardsUntradeableCost.get() : Config.SERVER.untradeableCost.get(); break;
            case "unbreedable": base = isShards ? Config.SERVER.shardsUnbreedableCost.get() : Config.SERVER.unbreedableCost.get(); break;
            case "rename": base = isShards ? Config.SERVER.shardsRenameCost.get() : Config.SERVER.renameCost.get(); break;
        }
        return pkmn.isShiny() && !type.equals("shiny") ? (int)(base * Config.SERVER.shinyMultiplier.get()) : base;
    }

    public static void openPartyMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new PartyMenu(id, inv), parseHexName(Language.STRINGS.titleParty.get())));
    }

    public static void openConfirm(ServerPlayer sp, int pSlot, String action, int cost, String cur) {
        sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ConfirmMenu(id, inv, pSlot, action, cost, cur), parseHexName(Language.STRINGS.titleConfirm.get()))));
    }

    public static void triggerSuccess(ServerPlayer sp, Pokemon pkmn, String action, int cost, String cur) {
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgUpgradeSuccess.get()));

        String cleanAction = action.toUpperCase();
        String auditMsg = "[UPB Audit] " + sp.getName().getString() + " spent " + cost + " " + cur + " to apply [" + cleanAction + "] to " + pkmn.getLocalizedName();
        UltimatePokeBuilder.LOGGER.info(auditMsg);

        String url = cur.equals(Config.SERVER.currencySpecial.get()) ? Config.SERVER.webhookShards.get() : Config.SERVER.webhookTokens.get();
        WebhookUtil.sendAudit(url, "💸 **UPB Purchase:** `" + sp.getName().getString() + "` spent **" + cost + " " + cur.toUpperCase() + "** on `" + pkmn.getLocalizedName() + "` (Action: **" + cleanAction + "**)");
    }

    public static void triggerFail(ServerPlayer sp, String cur) {
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgInsufficient.get().replace("%currency%", cur.toUpperCase())));
    }

    public static class PartyMenu extends ChestMenu {
        public PartyMenu(int id, Inventory inv) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            ServerPlayer sp = (ServerPlayer) inv.player;

            ItemStack infoBtn = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(Config.SERVER.infoMaterial.get())));
            if (infoBtn.isEmpty()) infoBtn = new ItemStack(Items.BOOK);
            infoBtn.set(DataComponents.CUSTOM_NAME, parseHexName(Config.SERVER.infoName.get()));

            List<Component> loreLines = new ArrayList<>();
            for (String line : Config.SERVER.infoLore.get()) {
                loreLines.add(parseHexName(CoinsEngineHook.parsePAPI(sp.getUUID(), line)));
            }

            infoBtn.set(DataComponents.LORE, new ItemLore(loreLines));
            getContainer().setItem(Config.SERVER.infoSlot.get(), infoBtn);

            List<Integer> partySlots = Config.SERVER.partySlots.get();
            for (int i = 0; i < 6 && i < partySlots.size(); i++) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(i);
                if (pkmn != null) {

                    com.pixelmonmod.pixelmon.api.pokemon.growth.GrowthData gData = pkmn.getForm().getGrowthData();
                    double zScore = (pkmn.getSize() - gData.mean()) / gData.standardDeviation();

                    String growthName = "Ordinary";
                    if (zScore <= -6.0) growthName = "Microscopic";
                    else if (zScore <= -4.0) growthName = "Pygmy";
                    else if (zScore <= -2.0) growthName = "Runt";
                    else if (zScore <= -0.5) growthName = "Small";
                    else if (zScore <= 0.5) growthName = "Ordinary";
                    else if (zScore <= 2.0) growthName = "Huge";
                    else if (zScore <= 4.0) growthName = "Giant";
                    else if (zScore <= 6.0) growthName = "Enormous";
                    else growthName = "Ginormous";

                    String ballName = pkmn.getBall().toString().replace("pixelmon:", "").replace("_", " ");
                    ballName = ballName.substring(0, 1).toUpperCase() + ballName.substring(1);
                    String gender = pkmn.getGender().name();
                    gender = gender.substring(0, 1).toUpperCase() + gender.substring(1).toLowerCase();
                    String shiny = pkmn.isShiny() ? "§eYes" : "§7No";
                    String heldItem = pkmn.getHeldItem().isEmpty() ? "None" : pkmn.getHeldItem().getHoverName().getString();
                    String form = pkmn.getForm().getName();
                    if (form == null || form.isEmpty() || form.equalsIgnoreCase("base")) form = "Normal";

                    ItemStack sprite = SpriteItemHelper.getPhoto(pkmn);
                    sprite.set(DataComponents.CUSTOM_NAME, parseHexName("&#FFAA00&l" + pkmn.getLocalizedName()));

                    String ivs = "§a" + pkmn.getIVs().getStat(BattleStatsType.HP) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getIVs().getTotal() + "/186)";
                    String evs = "§e" + pkmn.getEVs().getStat(BattleStatsType.HP) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getEVs().getTotal() + "/510)";

                    sprite.set(DataComponents.LORE, new ItemLore(List.of(
                            parseHexName("&#555555&m----------------------------------"),
                            parseHexName("&#AAAAAALevel: &#55FFFF" + pkmn.getPokemonLevel() + "   &#AAAAAAGrowth: &#55FF55" + growthName),
                            parseHexName("&#AAAAAANature: &#55FF55" + pkmn.getNature().getLocalizedName() + "   &#AAAAAAGender: &#FF55FF" + gender),
                            parseHexName("&#AAAAAAAbility: &#FFFF55" + pkmn.getAbility().getLocalizedName() + (pkmn.getAbilitySlot() == 2 ? " &#FF55FF(HA)" : " &#AAAAAA(Normal)")),
                            parseHexName("&#AAAAAABall: &#FFFFFF" + ballName + "   &#AAAAAAShiny: " + shiny),
                            parseHexName("&#AAAAAAForm: &#FFAA00" + form + "   &#AAAAAAFriend: &#FF5555" + pkmn.getFriendship() + "/255"),
                            parseHexName("&#AAAAAAHeld Item: &#55FFFF" + heldItem),
                            parseHexName("&#555555&m----------------------------------"),
                            parseHexName("&#AAAAAAIVs (HP/Atk/Def/SpA/SpD/Spe):"),
                            parseHexName(ivs),
                            parseHexName("&#AAAAAAEVs (HP/Atk/Def/SpA/SpD/Spe):"),
                            parseHexName(evs),
                            parseHexName("&#555555&m----------------------------------"),
                            parseHexName("&#FFFF55Click to Open Builder!")
                    )));
                    getContainer().setItem(partySlots.get(i), sprite);
                }
            }
        }
        @Override
        public void clicked(int slotId, int b, ClickType c, Player p) {
            List<Integer> partySlots = Config.SERVER.partySlots.get();
            if (partySlots.contains(slotId)) {
                int pIndex = partySlots.indexOf(slotId);
                ((ServerPlayer) p).server.execute(() -> p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pIndex), parseHexName(Language.STRINGS.titleBuilder.get()))));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class BuilderMenu extends ChestMenu {
        private final int pSlot;
        public BuilderMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x6, id, inv, new SimpleContainer(54), 6);
            this.pSlot = pSlot;
            for (int i = 0; i < 54; i++) getContainer().setItem(i, getFiller());

            ServerPlayer sp = (ServerPlayer) inv.player;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            if (pkmn == null) return;
            String cur = getCurrency(pkmn);
            boolean isLegend = pkmn.isLegendary() || pkmn.isUltraBeast();

            com.pixelmonmod.pixelmon.api.pokemon.growth.GrowthData gData = pkmn.getForm().getGrowthData();
            double zScore = (pkmn.getSize() - gData.mean()) / gData.standardDeviation();

            String growthName = "Ordinary";
            if (zScore <= -6.0) growthName = "Microscopic";
            else if (zScore <= -4.0) growthName = "Pygmy";
            else if (zScore <= -2.0) growthName = "Runt";
            else if (zScore <= -0.5) growthName = "Small";
            else if (zScore <= 0.5) growthName = "Ordinary";
            else if (zScore <= 2.0) growthName = "Huge";
            else if (zScore <= 4.0) growthName = "Giant";
            else if (zScore <= 6.0) growthName = "Enormous";
            else growthName = "Ginormous";

            String ballName = pkmn.getBall().toString().replace("pixelmon:", "").replace("_", " ");
            ballName = ballName.substring(0, 1).toUpperCase() + ballName.substring(1);
            String gender = pkmn.getGender().name();
            gender = gender.substring(0, 1).toUpperCase() + gender.substring(1).toLowerCase();
            String shiny = pkmn.isShiny() ? "§eYes" : "§7No";
            String heldItem = pkmn.getHeldItem().isEmpty() ? "None" : pkmn.getHeldItem().getHoverName().getString();
            String form = pkmn.getForm().getName();
            if (form == null || form.isEmpty() || form.equalsIgnoreCase("base")) form = "Normal";

            ItemStack sprite = SpriteItemHelper.getPhoto(pkmn);
            sprite.set(DataComponents.CUSTOM_NAME, parseHexName("&#FFAA00&l" + pkmn.getLocalizedName()));

            String ivs = "§a" + pkmn.getIVs().getStat(BattleStatsType.HP) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§a" + pkmn.getIVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getIVs().getTotal() + "/186)";
            String evs = "§e" + pkmn.getEVs().getStat(BattleStatsType.HP) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_ATTACK) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPECIAL_DEFENSE) + "§8/§e" + pkmn.getEVs().getStat(BattleStatsType.SPEED) + " §7(" + pkmn.getEVs().getTotal() + "/510)";

            sprite.set(DataComponents.LORE, new ItemLore(List.of(
                    parseHexName("&#555555&m----------------------------------"),
                    parseHexName("&#AAAAAALevel: &#55FFFF" + pkmn.getPokemonLevel() + "   &#AAAAAAGrowth: &#55FF55" + growthName),
                    parseHexName("&#AAAAAANature: &#55FF55" + pkmn.getNature().getLocalizedName() + "   &#AAAAAAGender: &#FF55FF" + gender),
                    parseHexName("&#AAAAAAAbility: &#FFFF55" + pkmn.getAbility().getLocalizedName() + (pkmn.getAbilitySlot() == 2 ? " &#FF55FF(HA)" : " &#AAAAAA(Normal)")),
                    parseHexName("&#AAAAAABall: &#FFFFFF" + ballName + "   &#AAAAAAShiny: " + shiny),
                    parseHexName("&#AAAAAAForm: &#FFAA00" + form + "   &#AAAAAAFriend: &#FF5555" + pkmn.getFriendship() + "/255"),
                    parseHexName("&#AAAAAAHeld Item: &#55FFFF" + heldItem),
                    parseHexName("&#555555&m----------------------------------"),
                    parseHexName("&#AAAAAAIVs (HP/Atk/Def/SpA/SpD/Spe):"),
                    parseHexName(ivs),
                    parseHexName("&#AAAAAAEVs (HP/Atk/Def/SpA/SpD/Spe):"),
                    parseHexName(evs),
                    parseHexName("&#555555&m----------------------------------"),
                    parseHexName("&#AAAAAAYour Balance: &#FFFF55" + CoinsEngineHook.getBalance(sp, cur) + " " + cur.toUpperCase())
            )));
            getContainer().setItem(Config.SERVER.slotPokemon.get(), sprite);

            if (pkmn.isShiny()) {
                getContainer().setItem(Config.SERVER.slotShiny.get(), createBtn("minecraft:sponge", Language.STRINGS.btnRevertShiny.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("shiny", pkmn, cur))).replace("%currency%", cur)));
            } else {
                getContainer().setItem(Config.SERVER.slotShiny.get(), createBtn("pixelmon:shiny_stone", Language.STRINGS.btnMakeShiny.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("shiny", pkmn, cur))).replace("%currency%", cur)));
            }

            if (pkmn.hasFlag(Flags.UNTRADEABLE)) {
                getContainer().setItem(Config.SERVER.slotUntradeable.get(), createBtn("minecraft:gold_nugget", Language.STRINGS.btnMakeTradeable.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("untradeable", pkmn, cur))).replace("%currency%", cur)));
            } else {
                getContainer().setItem(Config.SERVER.slotUntradeable.get(), createBtn("minecraft:iron_bars", Language.STRINGS.btnMakeUntradeable.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("untradeable", pkmn, cur))).replace("%currency%", cur)));
            }

            if (isLegend) {
                getContainer().setItem(Config.SERVER.slotUnbreedable.get(), createBtn("minecraft:barrier", "&#FF5555Legendary", "&#AAAAAALegendary Pokémon cannot breed."));
            } else if (pkmn.hasFlag(Flags.UNBREEDABLE)) {
                getContainer().setItem(Config.SERVER.slotUnbreedable.get(), createBtn("minecraft:egg", Language.STRINGS.btnMakeBreedable.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("unbreedable", pkmn, cur))).replace("%currency%", cur)));
            } else {
                getContainer().setItem(Config.SERVER.slotUnbreedable.get(), createBtn("minecraft:iron_bars", Language.STRINGS.btnMakeUnbreedable.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("unbreedable", pkmn, cur))).replace("%currency%", cur)));
            }

            getContainer().setItem(Config.SERVER.slotLevel.get(), createBtn("pixelmon:rare_candy", Language.STRINGS.btnLevel.get(), Language.STRINGS.loreCostLvl.get().replace("%cost%", String.valueOf(getCost("level", pkmn, cur))).replace("%currency%", cur)));
            getContainer().setItem(Config.SERVER.slotAbility.get(), createBtn("pixelmon:ability_capsule", Language.STRINGS.btnAbility.get(), Language.STRINGS.loreClickAbility.get()));
            getContainer().setItem(Config.SERVER.slotNature.get(), createBtn("pixelmon:mint_adamant", Language.STRINGS.btnNature.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("nature", pkmn, cur))).replace("%currency%", cur)));
            getContainer().setItem(Config.SERVER.slotGrowth.get(), createBtn("minecraft:slime_block", Language.STRINGS.btnGrowth.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("growth", pkmn, cur))).replace("%currency%", cur)));

            if (isLegend || pkmn.getGender() == Gender.NONE) {
                getContainer().setItem(Config.SERVER.slotGender.get(), createBtn("minecraft:barrier", Language.STRINGS.btnGenderless.get(), Language.STRINGS.loreGenderless.get()));
            } else {
                getContainer().setItem(Config.SERVER.slotGender.get(), createBtn("minecraft:pink_dye", Language.STRINGS.btnGender.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("gender", pkmn, cur))).replace("%currency%", cur)));
            }

            getContainer().setItem(Config.SERVER.slotBall.get(), createBallBtn("poke_ball", Language.STRINGS.btnBall.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("ball", pkmn, cur))).replace("%currency%", cur)));
            getContainer().setItem(Config.SERVER.slotEvs.get(), createBtn("pixelmon:hp_up", Language.STRINGS.btnEvs.get(), Language.STRINGS.loreCostEv.get().replace("%cost%", String.valueOf(getCost("ev", pkmn, cur))).replace("%currency%", cur)));
            getContainer().setItem(Config.SERVER.slotIvs.get(), createBtn("pixelmon:calcium", Language.STRINGS.btnIvs.get(), Language.STRINGS.loreCostIv.get().replace("%cost%", String.valueOf(getCost("iv", pkmn, cur))).replace("%currency%", cur)));
            getContainer().setItem(Config.SERVER.slotRename.get(), createBtn("minecraft:barrier", Language.STRINGS.btnRename.get(), Language.STRINGS.loreRenameSoon.get()));
            getContainer().setItem(Config.SERVER.slotBack.get(), createBtn("minecraft:arrow", Language.STRINGS.btnBackParty.get(), null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);
            boolean isLegend = pkmn.isLegendary() || pkmn.isUltraBeast();

            if (slot == Config.SERVER.slotBack.get()) sp.server.execute(() -> openPartyMenu(sp));
            if (slot == Config.SERVER.slotLevel.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AdjustMenu(id, inv, pSlot, "level", null), parseHexName(Language.STRINGS.titleLevel.get()))));
            if (slot == Config.SERVER.slotAbility.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AbilityMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleAbility.get()))));
            if (slot == Config.SERVER.slotNature.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new NatureMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleNature.get()))));
            if (slot == Config.SERVER.slotGrowth.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GrowthMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleGrowth.get()))));
            if (slot == Config.SERVER.slotBall.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BallMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBall.get()))));

            if (slot == Config.SERVER.slotGender.get() && !isLegend && pkmn.getGender() != Gender.NONE) {
                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new GenderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleGender.get()))));
            }

            if (slot == Config.SERVER.slotEvs.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new StatSelectMenu(id, inv, pSlot, "ev"), parseHexName(Language.STRINGS.titleEvs.get()))));
            if (slot == Config.SERVER.slotIvs.get()) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new StatSelectMenu(id, inv, pSlot, "iv"), parseHexName(Language.STRINGS.titleIvs.get()))));

            if (slot == Config.SERVER.slotShiny.get()) openConfirm(sp, pSlot, pkmn.isShiny() ? "unshiny" : "shiny", getCost("shiny", pkmn, cur), cur);
            if (slot == Config.SERVER.slotUntradeable.get()) openConfirm(sp, pSlot, pkmn.hasFlag(Flags.UNTRADEABLE) ? "tradeable" : "untradeable", getCost("untradeable", pkmn, cur), cur);

            if (slot == Config.SERVER.slotUnbreedable.get() && !isLegend) {
                openConfirm(sp, pSlot, pkmn.hasFlag(Flags.UNBREEDABLE) ? "breedable" : "unbreedable", getCost("unbreedable", pkmn, cur), cur);
            }

            if (slot == Config.SERVER.slotRename.get()) {
                sp.sendSystemMessage(parseHexName(Language.STRINGS.msgRenameDisabled.get()));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class AbilityMenu extends ChestMenu {
        private final int pSlot;

        public AbilityMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            ServerPlayer sp = (ServerPlayer) inv.player;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);

            String ab1 = "None", ab2 = "None", ha = "None";
            try {
                var normalAbs = pkmn.getForm().getAbilities().getAbilities();
                if (normalAbs.length > 0 && normalAbs[0] != null) ab1 = normalAbs[0].getName();
                if (normalAbs.length > 1 && normalAbs[1] != null) ab2 = normalAbs[1].getName();
                var hiddenAbs = pkmn.getForm().getAbilities().getHiddenAbilities();
                if (hiddenAbs.length > 0 && hiddenAbs[0] != null) ha = hiddenAbs[0].getName();
            } catch (Exception e) {}

            if (!ab1.equals("None")) getContainer().setItem(11, createBtn("pixelmon:ability_capsule", "&#FFFF55Ability 1: &#FFFFFF" + ab1, Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("ability", pkmn, cur))).replace("%currency%", cur)));
            if (!ab2.equals("None")) getContainer().setItem(13, createBtn("pixelmon:ability_capsule", "&#FFFF55Ability 2: &#FFFFFF" + ab2, Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("ability", pkmn, cur))).replace("%currency%", cur)));
            if (!ha.equals("None"))  getContainer().setItem(15, createBtn("pixelmon:ability_patch", "&#FF55FFHidden Ability: &#FFFFFF" + ha, Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(getCost("hidden_ability", pkmn, cur))).replace("%currency%", cur)));

            getContainer().setItem(26, createBtn("minecraft:barrier", Language.STRINGS.btnBackBuilder.get(), null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);

            try {
                var normalAbs = pkmn.getForm().getAbilities().getAbilities();
                var hiddenAbs = pkmn.getForm().getAbilities().getHiddenAbilities();

                if (slot == 11 && normalAbs.length > 0) openConfirm(sp, pSlot, "ability:" + normalAbs[0].getName(), getCost("ability", pkmn, cur), cur);
                if (slot == 13 && normalAbs.length > 1) openConfirm(sp, pSlot, "ability:" + normalAbs[1].getName(), getCost("ability", pkmn, cur), cur);
                if (slot == 15 && hiddenAbs.length > 0) openConfirm(sp, pSlot, "ability:" + hiddenAbs[0].getName(), getCost("hidden_ability", pkmn, cur), cur);
            } catch (Exception ignored) {}
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class StatSelectMenu extends ChestMenu {
        private final int pSlot;
        private final String mode;

        public StatSelectMenu(int id, Inventory inv, int pSlot, String mode) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            this.mode = mode;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            String title = mode.equals("ev") ? "&#FFFF55Adjust EVs" : "&#55FF55Adjust IVs";
            getContainer().setItem(4, createBtn("minecraft:book", title, Language.STRINGS.loreClickStat.get()));

            getContainer().setItem(10, createBtn("minecraft:golden_apple", "&#FF5555HP", null));
            getContainer().setItem(11, createBtn("minecraft:iron_sword", "&#FF5555Attack", null));
            getContainer().setItem(12, createBtn("minecraft:iron_chestplate", "&#FFFF55Defense", null));
            getContainer().setItem(14, createBtn("minecraft:blaze_powder", "&#5555FFSp. Atk", null));
            getContainer().setItem(15, createBtn("minecraft:shield", "&#55FF55Sp. Def", null));
            getContainer().setItem(16, createBtn("minecraft:feather", "&#55FFFFSpeed", null));

            getContainer().setItem(26, createBtn("minecraft:barrier", Language.STRINGS.btnBackBuilder.get(), null));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }

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
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

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
            ServerPlayer sp = (ServerPlayer) p;
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);

            int totalCost = addedValues * getCost(mode, pkmn, cur);
            int currentVal = mode.equals("level") ? pkmn.getPokemonLevel() : (mode.equals("ev") ? pkmn.getEVs().getStat(stat) : pkmn.getIVs().getStat(stat));
            String statName = mode.equals("level") ? "Level" : stat.getLocalizedName();

            getContainer().setItem(10, createBtn("minecraft:red_wool", "&#FF5555-50", null));
            getContainer().setItem(11, createBtn("minecraft:red_terracotta", "&#FF5555-10", null));
            getContainer().setItem(12, createBtn("minecraft:red_stained_glass", "&#FF5555-1", null));

            getContainer().setItem(13, createBtn("pixelmon:rare_candy", "&#55FFFFTarget " + statName + ": &#FFFFFF" + (currentVal + addedValues), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(totalCost)).replace("%currency%", cur)));

            getContainer().setItem(14, createBtn("minecraft:green_stained_glass", "&#55FF55+1", null));
            getContainer().setItem(15, createBtn("minecraft:green_terracotta", "&#55FF55+10", null));
            getContainer().setItem(16, createBtn("minecraft:green_wool", "&#55FF55+50", null));

            getContainer().setItem(30, createBtn("minecraft:emerald_block", Language.STRINGS.btnConfirm.get(), Language.STRINGS.lorePay.get().replace("%cost%", String.valueOf(totalCost)).replace("%currency%", cur)));
            getContainer().setItem(32, createBtn("minecraft:barrier", Language.STRINGS.btnCancel.get(), Language.STRINGS.btnBackBuilder.get()));
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

            if (slot == 32) sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));

            if (slot == 30 && addedValues > 0) {
                String cur = getCurrency(pkmn);
                int totalCost = addedValues * getCost(mode, pkmn, cur);
                if (CoinsEngineHook.takeBalance(sp, cur, totalCost)) {
                    if (mode.equals("level")) pkmn.setLevel(currentVal + addedValues);
                    else if (mode.equals("ev")) pkmn.getEVs().setStat(stat, currentVal + addedValues);
                    else if (mode.equals("iv")) pkmn.getIVs().setStat(stat, currentVal + addedValues);

                    triggerSuccess(sp, pkmn, "+" + addedValues + " " + mode.toUpperCase(), totalCost, cur);
                    sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));
                } else {
                    triggerFail(sp, cur);
                }
            }
            refresh(p);
            this.broadcastChanges();
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class NatureMenu extends ChestMenu {
        private final int pSlot;
        public NatureMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x4, id, inv, new SimpleContainer(36), 4);
            this.pSlot = pSlot;
            for (int i = 0; i < 36; i++) getContainer().setItem(i, getFiller());

            String[] natures = {"Adamant", "Bashful", "Bold", "Brave", "Calm", "Careful", "Docile", "Gentle", "Hardy", "Hasty", "Impish", "Jolly", "Lax", "Lonely", "Mild", "Modest", "Naive", "Naughty", "Quiet", "Quirky", "Rash", "Relaxed", "Sassy", "Serious", "Timid"};

            for (int i = 0; i < natures.length; i++) {
                getContainer().setItem(i, createBtn("pixelmon:mint_" + natures[i].toLowerCase(), "&#55FFFF" + natures[i], Language.STRINGS.loreClickNature.get()));
            }
            getContainer().setItem(35, createBtn("minecraft:barrier", Language.STRINGS.btnBack.get(), null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 35) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            String[] natures = {"Adamant", "Bashful", "Bold", "Brave", "Calm", "Careful", "Docile", "Gentle", "Hardy", "Hasty", "Impish", "Jolly", "Lax", "Lonely", "Mild", "Modest", "Naive", "Naughty", "Quiet", "Quirky", "Rash", "Relaxed", "Sassy", "Serious", "Timid"};
            if (slot >= 0 && slot < natures.length) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "nature:" + natures[slot], getCost("nature", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class GenderMenu extends ChestMenu {
        private final int pSlot;
        public GenderMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());
            getContainer().setItem(11, createBtn("minecraft:light_blue_wool", "&#55FFFFMale", null));
            getContainer().setItem(15, createBtn("minecraft:pink_wool", "&#FF55FFFemale", null));
            getContainer().setItem(26, createBtn("minecraft:barrier", Language.STRINGS.btnBack.get(), null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
            String cur = getCurrency(pkmn);
            if (slot == 11) openConfirm(sp, pSlot, "gender:Male", getCost("gender", pkmn, cur), cur);
            if (slot == 15) openConfirm(sp, pSlot, "gender:Female", getCost("gender", pkmn, cur), cur);
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class GrowthMenu extends ChestMenu {
        private final int pSlot;
        public GrowthMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x3, id, inv, new SimpleContainer(27), 3);
            this.pSlot = pSlot;
            for (int i = 0; i < 27; i++) getContainer().setItem(i, getFiller());

            String[] growths = {"Microscopic", "Pygmy", "Runt", "Small", "Ordinary", "Huge", "Giant", "Enormous", "Ginormous"};
            String[] icons = {
                    "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:crimson_fungus",
                    "minecraft:warped_fungus", "minecraft:oak_sapling", "minecraft:slime_block",
                    "minecraft:mushroom_stem", "minecraft:brown_mushroom_block", "minecraft:red_mushroom_block"
            };

            for (int i = 0; i < growths.length; i++) {
                getContainer().setItem(9 + i, createBtn(icons[i], "&#55AA00" + growths[i], Language.STRINGS.loreClickGrowth.get()));
            }
            getContainer().setItem(26, createBtn("minecraft:barrier", Language.STRINGS.btnBack.get(), null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 26) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            String[] growths = {"Microscopic", "Pygmy", "Runt", "Small", "Ordinary", "Huge", "Giant", "Enormous", "Ginormous"};
            if (slot >= 9 && slot < 9 + growths.length) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "growth:" + growths[slot - 9].toLowerCase(), getCost("growth", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

    public static class BallMenu extends ChestMenu {
        private final int pSlot;
        public BallMenu(int id, Inventory inv, int pSlot) {
            super(MenuType.GENERIC_9x4, id, inv, new SimpleContainer(36), 4);
            this.pSlot = pSlot;
            for (int i = 0; i < 36; i++) getContainer().setItem(i, getFiller());

            String[] balls = {"poke_ball", "great_ball", "ultra_ball", "master_ball", "premier_ball", "heal_ball", "net_ball", "nest_ball", "dive_ball", "dusk_ball", "timer_ball", "quick_ball", "repeat_ball", "safari_ball", "fast_ball", "level_ball", "lure_ball", "heavy_ball", "love_ball", "friend_ball", "moon_ball", "sport_ball", "park_ball", "dream_ball", "beast_ball", "cherish_ball"};
            String[] hexColors = {"&#FF5555", "&#5555FF", "&#FFFF55", "&#AA00AA", "&#FFFFFF", "&#FF55FF", "&#55FFFF", "&#55FF55", "&#5555FF", "&#555555", "&#FFAA00", "&#55FFFF", "&#FF5555", "&#55AA00", "&#FFAA00", "&#FFAA00", "&#5555FF", "&#AAAAAA", "&#FF55FF", "&#55FF55", "&#55FFFF", "&#FFAA00", "&#FFFF55", "&#FF55FF", "&#5555FF", "&#FF5555"};

            for (int i = 0; i < Math.min(balls.length, 35); i++) {
                String cleanName = balls[i].replace("_", " ").toUpperCase();
                String color = (i < hexColors.length) ? hexColors[i] : "&#FFFFFF";
                getContainer().setItem(i, createBallBtn(balls[i], color + "&l" + cleanName, Language.STRINGS.loreClickBall.get()));
            }
            getContainer().setItem(35, createBtn("minecraft:barrier", Language.STRINGS.btnBack.get(), null));
        }
        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 35) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            String[] balls = {"poke_ball", "great_ball", "ultra_ball", "master_ball", "premier_ball", "heal_ball", "net_ball", "nest_ball", "dive_ball", "dusk_ball", "timer_ball", "quick_ball", "repeat_ball", "safari_ball", "fast_ball", "level_ball", "lure_ball", "heavy_ball", "love_ball", "friend_ball", "moon_ball", "sport_ball", "park_ball", "dream_ball", "beast_ball", "cherish_ball"};
            if (slot >= 0 && slot < Math.min(balls.length, 35)) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);
                String cur = getCurrency(pkmn);
                openConfirm(sp, pSlot, "ball:" + balls[slot], getCost("ball", pkmn, cur), cur);
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }

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
            getContainer().setItem(11, createBtn("minecraft:emerald_block", Language.STRINGS.btnConfirm.get(), Language.STRINGS.loreCost.get().replace("%cost%", String.valueOf(finalCost)).replace("%currency%", cur)));
            getContainer().setItem(15, createBtn("minecraft:barrier", Language.STRINGS.btnCancel.get(), Language.STRINGS.btnBackBuilder.get()));
        }

        @Override
        public void clicked(int slot, int b, ClickType c, Player p) {
            ServerPlayer sp = (ServerPlayer) p;
            if (slot == 15) { sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get())))); return; }
            if (slot == 11) {
                Pokemon pkmn = StorageProxy.getPartyNow(sp).get(pSlot);

                if (action.equals("shiny") && pkmn.isShiny() ||
                        action.equals("unshiny") && !pkmn.isShiny() ||
                        action.equals("untradeable") && pkmn.hasFlag(Flags.UNTRADEABLE) ||
                        action.equals("tradeable") && !pkmn.hasFlag(Flags.UNTRADEABLE) ||
                        action.equals("unbreedable") && pkmn.hasFlag(Flags.UNBREEDABLE) ||
                        action.equals("breedable") && !pkmn.hasFlag(Flags.UNBREEDABLE)) {
                    sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasTrait.get()));
                    return;
                }

                if (action.startsWith("ability:")) {
                    String targetAb = action.split(":")[1];
                    if (pkmn.getAbility().getName().equalsIgnoreCase(targetAb)) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasTrait.get()));
                        return;
                    }
                }

                if (action.startsWith("nature:")) {
                    if (pkmn.getNature().name().equalsIgnoreCase(action.split(":")[1])) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasNature.get()));
                        return;
                    }
                }

                if (action.startsWith("gender:")) {
                    if (pkmn.getGender().name().equalsIgnoreCase(action.split(":")[1])) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasGender.get()));
                        return;
                    }
                }

                if (action.startsWith("ball:")) {
                    String bId = action.split(":")[1].toLowerCase();
                    if (pkmn.getBall().toString().toLowerCase().contains(bId)) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasBall.get()));
                        return;
                    }
                }

                if (action.startsWith("growth:")) {
                    String targetGrowth = action.split(":")[1].toLowerCase();
                    com.pixelmonmod.pixelmon.api.pokemon.growth.GrowthData gData = pkmn.getForm().getGrowthData();
                    double zScore = 0.0;
                    switch (targetGrowth) {
                        case "microscopic": zScore = -7.5; break;
                        case "pygmy":       zScore = -5.25; break;
                        case "runt":        zScore = -3.0; break;
                        case "small":       zScore = -1.5; break;
                        case "ordinary":    zScore = 0.0; break;
                        case "huge":        zScore = 1.5; break;
                        case "giant":       zScore = 3.0; break;
                        case "enormous":    zScore = 5.25; break;
                        case "ginormous":   zScore = 7.5; break;
                    }
                    double targetSize = gData.mean() + (zScore * gData.standardDeviation());
                    if (Math.abs(pkmn.getSize() - targetSize) < 0.01) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgAlreadyHasGrowth.get()));
                        return;
                    }
                }

                if (CoinsEngineHook.takeBalance(sp, cur, finalCost)) {
                    try {
                        if (action.equals("shiny")) pkmn.setShiny(true);
                        else if (action.equals("unshiny")) pkmn.setShiny(false);
                        else if (action.equals("untradeable")) pkmn.addFlag(Flags.UNTRADEABLE);
                        else if (action.equals("tradeable")) pkmn.removeFlag(Flags.UNTRADEABLE);
                        else if (action.equals("unbreedable")) pkmn.addFlag(Flags.UNBREEDABLE);
                        else if (action.equals("breedable")) pkmn.removeFlag(Flags.UNBREEDABLE);
                        else if (action.startsWith("ability:")) {
                            String specName = action.split(":")[1].replace(" ", "").toLowerCase();
                            PokemonSpecificationProxy.create("ability:" + specName).get().apply(pkmn);
                        }
                        else if (action.startsWith("growth:")) {
                            com.pixelmonmod.pixelmon.api.pokemon.growth.GrowthData gData = pkmn.getForm().getGrowthData();
                            double mean = gData.mean();
                            double stdDev = gData.standardDeviation();
                            double zScore = 0.0;

                            String targetGrowth = action.split(":")[1].toLowerCase();
                            switch (targetGrowth) {
                                case "microscopic": zScore = -7.5; break;
                                case "pygmy":       zScore = -5.25; break;
                                case "runt":        zScore = -3.0; break;
                                case "small":       zScore = -1.5; break;
                                case "ordinary":    zScore = 0.0; break;
                                case "huge":        zScore = 1.5; break;
                                case "giant":       zScore = 3.0; break;
                                case "enormous":    zScore = 5.25; break;
                                case "ginormous":   zScore = 7.5; break;
                            }

                            pkmn.setSize(mean + (zScore * stdDev));
                        }
                        else if (action.startsWith("nature:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);
                        else if (action.startsWith("gender:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);
                        else if (action.startsWith("ball:")) PokemonSpecificationProxy.create(action).get().apply(pkmn);

                        triggerSuccess(sp, pkmn, action, finalCost, cur);
                    } catch (Exception e) {
                        sp.sendSystemMessage(parseHexName(Language.STRINGS.msgError.get()));
                        UltimatePokeBuilder.LOGGER.error("Failed to apply UPB trait", e);
                    }
                } else {
                    triggerFail(sp, cur);
                }

                sp.server.execute(() -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new BuilderMenu(id, inv, pSlot), parseHexName(Language.STRINGS.titleBuilder.get()))));
            }
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
    }
}
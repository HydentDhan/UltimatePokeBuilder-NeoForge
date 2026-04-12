package com.ultimatepokebuilder;

import com.mojang.logging.LogUtils;
import com.ultimatepokebuilder.command.PokeBuilderCommand;
import com.ultimatepokebuilder.config.Config;
import com.ultimatepokebuilder.config.Language;
import com.ultimatepokebuilder.config.MessagesConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import java.io.File;

@Mod(UltimatePokeBuilder.MOD_ID)
public class UltimatePokeBuilder {

    public static final String MOD_ID = "ultimatepokebuilder";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static UltimatePokeBuilder instance;

    public UltimatePokeBuilder(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modContainer.registerConfig(ModConfig.Type.SERVER, MessagesConfig.SPEC, "UltimatePokeBuilder/messages.toml");

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::onCommandsRegister);
        NeoForge.EVENT_BUS.register(UPBPermissions.class);
        NeoForge.EVENT_BUS.register(com.ultimatepokebuilder.ui.ServerMenuHandler.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        File configFolder = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().toFile();
        Config.loadConfig(configFolder);
        Language.loadLocale(configFolder);
        LOGGER.info("UltimatePokeBuilder Core Initialized.");
    }

    public void onCommandsRegister(RegisterCommandsEvent event) {
        PokeBuilderCommand.register(event.getDispatcher());
    }
}
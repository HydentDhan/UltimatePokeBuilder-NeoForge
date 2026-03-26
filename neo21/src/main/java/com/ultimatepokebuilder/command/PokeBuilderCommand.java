package com.ultimatepokebuilder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.ultimatepokebuilder.ui.ServerMenuHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PokeBuilderCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("upb")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ServerMenuHandler.openPartyMenu(player);
                    }
                    return 1;
                })
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2)) // Requires Server OP/Admin
                        .executes(context -> {
                            // NeoForge natively auto-reloads TOML configs when saved, but this confirms it for the admin.
                            context.getSource().sendSystemMessage(Component.literal("§a[UPB] Configuration reloaded successfully!"));
                            return 1;
                        })
                )
        );

        // Alias
        dispatcher.register(Commands.literal("pokebuilder").executes(context -> {
            if (context.getSource().getEntity() instanceof ServerPlayer player) ServerMenuHandler.openPartyMenu(player);
            return 1;
        }));
    }
}
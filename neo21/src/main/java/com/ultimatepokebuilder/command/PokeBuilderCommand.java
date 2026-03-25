package com.ultimatepokebuilder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.ultimatepokebuilder.UPBPermissions;
import com.ultimatepokebuilder.ui.ServerMenuHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class PokeBuilderCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("upb")
                .requires(source -> source.getEntity() instanceof ServerPlayer p && UPBPermissions.hasPerm(p, UPBPermissions.CMD_BASE))
                .executes(context -> {
                    ServerMenuHandler.openPartyMenu((ServerPlayer) context.getSource().getEntity());
                    return 1;
                })
        );
    }
}
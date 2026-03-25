package com.ultimatepokebuilder;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

public class UPBPermissions {

    // Command Nodes
    public static final PermissionNode<Boolean> CMD_BASE = new PermissionNode<>(UltimatePokeBuilder.MOD_ID, "command.base", PermissionTypes.BOOLEAN, (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> CMD_RELOAD = new PermissionNode<>(UltimatePokeBuilder.MOD_ID, "command.reload", PermissionTypes.BOOLEAN, (player, uuid, context) -> false); // Default OP/Admin

    // Tokens Nodes
    public static final PermissionNode<Boolean> TOKENS_BASE = new PermissionNode<>(UltimatePokeBuilder.MOD_ID, "command.tokens.base", PermissionTypes.BOOLEAN, (player, uuid, context) -> true);
    public static final PermissionNode<Boolean> TOKENS_GIVE = new PermissionNode<>(UltimatePokeBuilder.MOD_ID, "command.tokens.give", PermissionTypes.BOOLEAN, (player, uuid, context) -> false);
    public static final PermissionNode<Boolean> TOKENS_TAKE = new PermissionNode<>(UltimatePokeBuilder.MOD_ID, "command.tokens.take", PermissionTypes.BOOLEAN, (player, uuid, context) -> false);

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(CMD_BASE, CMD_RELOAD, TOKENS_BASE, TOKENS_GIVE, TOKENS_TAKE);
        UltimatePokeBuilder.LOGGER.info("Registered LuckPerms Permission Nodes!");
    }

    // Helper method to easily check permissions
    public static boolean hasPerm(ServerPlayer player, PermissionNode<Boolean> node) {
        return PermissionAPI.getPermission(player, node);
    }
}
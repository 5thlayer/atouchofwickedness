// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /wickedness}, for operators: a pack author's or a tester's view of what a player never sees.
 *
 * <ul>
 *   <li>{@code /wickedness get <player>} reports the player's Wickedness.</li>
 *   <li>{@code /wickedness set <player> <value>} sets it, and {@code add} adds to it, a negative value
 *       taking away; either one reaching the Threshold sends the Bailiff.</li>
 *   <li>{@code /wickedness threshold <player>} reports the player's Threshold.</li>
 * </ul>
 */
public final class WickednessCommand {

    private WickednessCommand() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(Commands.literal("wickedness")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                        .executes(WickednessCommand::get)))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(WickednessCommand::set))))
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", IntegerArgumentType.integer()).executes(WickednessCommand::add))))
                .then(Commands.literal("threshold").then(Commands.argument("player", EntityArgument.player())
                        .executes(WickednessCommand::threshold)))));
    }

    private static int get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        return report(context, player, Wickedness.of(player));
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        Wickedness.set(player, IntegerArgumentType.getInteger(context, "value"));
        return report(context, player, Wickedness.of(player));
    }

    private static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        Wickedness.add(player, IntegerArgumentType.getInteger(context, "value"));
        return report(context, player, Wickedness.of(player));
    }

    private static int threshold(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(context, "player");
        int threshold = Wickedness.threshold(player);
        context.getSource().sendSuccess(() -> Component.translatable("commands.atouchofwickedness.threshold", player.getDisplayName(), threshold), false);
        return threshold;
    }

    private static int report(CommandContext<CommandSourceStack> context, ServerPlayer player, int wickedness) {
        context.getSource().sendSuccess(() -> Component.translatable("commands.atouchofwickedness.wickedness", player.getDisplayName(), wickedness), false);
        return wickedness;
    }
}

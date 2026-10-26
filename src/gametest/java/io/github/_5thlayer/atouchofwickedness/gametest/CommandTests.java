// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;

/**
 * {@code /wickedness get|set|add <player> [value]} and {@code /wickedness threshold <player>} work
 * for a player with permission level 2, and not for one without.
 */
final class CommandTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);

    private CommandTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("command_get_set_add_and_threshold", 1, CommandTests::works);
        tests.test("command_needs_permission_level_2", 1, CommandTests::needsLevel2);
    }

    private static void works(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 1_000);
        var target = player.getScoreboardName();
        var set = run(player, LevelBasedPermissionSet.GAMEMASTER, "wickedness set " + target + " 17");
        if (Wickedness.of(player) != 17) {
            helper.fail("set left Wickedness at " + Wickedness.of(player) + ", not 17: " + set.stream().map(Component::getString).toList());
            return;
        }
        run(player, LevelBasedPermissionSet.GAMEMASTER, "wickedness add " + target + " 5");
        run(player, LevelBasedPermissionSet.GAMEMASTER, "wickedness add " + target + " -2");
        var got = run(player, LevelBasedPermissionSet.GAMEMASTER, "wickedness get " + target);
        if (Wickedness.of(player) != 20 || got.size() != 1 || !got.getFirst().getString().contains("20")) {
            helper.fail("17 add 5 add -2 reads " + got.stream().map(Component::getString).toList());
            return;
        }
        var threshold = run(player, LevelBasedPermissionSet.GAMEMASTER, "wickedness threshold " + target);
        if (threshold.size() != 1 || !threshold.getFirst().getString().contains("1000")) {
            helper.fail("threshold reads " + threshold.stream().map(Component::getString).toList());
            return;
        }
        helper.succeed();
    }

    private static void needsLevel2(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 1_000);
        run(player, LevelBasedPermissionSet.MODERATOR, "wickedness set " + player.getScoreboardName() + " 9");
        if (Wickedness.of(player) != 0) {
            helper.fail("a player of permission level 1 set their Wickedness to " + Wickedness.of(player));
            return;
        }
        helper.succeed();
    }

    /** Runs {@code command} as {@code player} with {@code permissions}, and returns what it said back. */
    private static List<Component> run(ServerPlayer player, PermissionSet permissions, String command) {
        var messages = new ArrayList<Component>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                messages.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        var source = player.createCommandSourceStack().withSource(sink).withPermission(permissions);
        player.level().getServer().getCommands().performPrefixedCommand(source, command);
        return messages;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * A new player's Threshold is drawn from 50 to 100 as they join, and their Wickedness and Threshold
 * are saved with them, so they survive a restart: the player saved as the server saves them on
 * stopping, and loaded again into a new player as it loads them on starting.
 */
final class ThresholdTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);

    private ThresholdTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("threshold_a_new_player_draws_from_50_to_100", 1, ThresholdTests::drawn);
        tests.test("threshold_survives_a_restart_with_wickedness", 1, ThresholdTests::survivesARestart);
    }

    private static void drawn(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        int threshold = Wickedness.threshold(player);
        if (threshold < 50 || threshold > 100) {
            helper.fail("a new player drew a Threshold of " + threshold);
            return;
        }
        if (Wickedness.of(player) != 0) {
            helper.fail("a new player starts with " + Wickedness.of(player) + " Wickedness");
            return;
        }
        helper.succeed();
    }

    private static void survivesARestart(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 87);
        Wickedness.set(player, 23);
        var registries = helper.getLevel().registryAccess();
        var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        player.saveWithoutId(saved);
        var restarted = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), player.getGameProfile(), ClientInformation.createDefault());
        restarted.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved.buildResult()));
        if (Wickedness.of(restarted) != 23 || Wickedness.threshold(restarted) != 87) {
            helper.fail("after a restart the player has " + Wickedness.of(restarted) + " Wickedness against a Threshold of "
                    + Wickedness.threshold(restarted) + ", not 23 against 87");
            return;
        }
        helper.succeed();
    }
}

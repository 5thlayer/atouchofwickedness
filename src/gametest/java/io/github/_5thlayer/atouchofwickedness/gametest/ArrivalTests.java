// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;

/**
 * Wickedness reaching the Threshold sends one Bailiff, 12 to 24 blocks from its player across the
 * ground, and as it arrives Wickedness drops to 0 and a new Threshold is drawn from 50 to 100. The
 * test sets a Threshold of 3, outside that range, so the redraw shows.
 */
final class ArrivalTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);

    private ArrivalTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("arrival_crossing_the_threshold_sends_one_bailiff", 20, ArrivalTests::crossing);
        tests.test("arrival_below_the_threshold_sends_none", 20, ArrivalTests::below);
    }

    private static void crossing(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 3);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        var bailiffs = Bailiffs.of(player);
        if (bailiffs.size() != 1) {
            helper.fail("crossing the Threshold sent " + bailiffs.size() + " Bailiffs");
            return;
        }
        var bailiff = bailiffs.getFirst();
        double across = Math.hypot(bailiff.getX() - player.getX(), bailiff.getZ() - player.getZ());
        if (across < 12 || across > 24) {
            helper.fail("the Bailiff arrived " + across + " blocks from its player");
            return;
        }
        if (Wickedness.of(player) != 0) {
            helper.fail("Wickedness is " + Wickedness.of(player) + " after the Bailiff arrived, not 0");
            return;
        }
        int threshold = Wickedness.threshold(player);
        if (threshold < 50 || threshold > 100) {
            helper.fail("the Threshold drawn after the visit is " + threshold);
            return;
        }
        // A second crossing while it is still here sends no other.
        Wickedness.set(player, threshold);
        if (Bailiffs.of(player).size() != 1) {
            helper.fail("crossing again while a Bailiff was here sent another");
            return;
        }
        helper.succeed();
    }

    private static void below(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 3);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        if (!Bailiffs.of(player).isEmpty() || Wickedness.of(player) != 2) {
            helper.fail("2 Wickedness against a Threshold of 3 sent a Bailiff or was reset");
            return;
        }
        helper.succeed();
    }
}

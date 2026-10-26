// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a Bailiff shows a viewer such as Jade: the player it came for, and the stacks it has taken
 * from them.
 */
final class ReportTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(5, 1, 5);

    private ReportTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("report_names_its_player_and_what_it_carries", 20, ReportTests::namesItsPlayer);
    }

    private static void namesItsPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        var report = bailiff.report(helper.getLevel());
        if (report == null) {
            helper.fail("the Bailiff has no report");
            return;
        }
        if (!report.player().equals(player.getDisplayName())) {
            helper.fail("the report names " + report.player().getString() + ", not " + player.getDisplayName().getString());
            return;
        }
        if (report.carried().size() != 1 || !report.carried().getFirst().is(Items.EMERALD) || report.carried().getFirst().getCount() != 7) {
            helper.fail("the report shows the Bailiff carrying " + report.carried() + ", not the 7 emeralds it took");
            return;
        }
        helper.succeed();
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.wickedness.Warnings;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

/**
 * A player entering Marked, Overdue or Forfeit hears that band's Warning, sent to them alone, so a
 * player beside them hears none. While a player is Forfeit, a cow near them flees from them.
 */
final class WarningTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BYSTANDER = new BlockPos(4, 1, 2);
    private static final BlockPos CORNER = new BlockPos(0, 1, 0);
    private static final BlockPos COW = new BlockPos(2, 1, 2);
    private static final List<Holder<SoundEvent>> WARNINGS = List.of(Warnings.MARKED, Warnings.OVERDUE, Warnings.FORFEIT);

    private WarningTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        // Within each band, clear of where it starts.
        tests.test("warning_marked_rings_a_far_bell", 20, helper -> hears(helper, 60, Warnings.MARKED));
        tests.test("warning_overdue_rattles_chains", 20, helper -> hears(helper, 80, Warnings.OVERDUE));
        tests.test("warning_forfeit_sounds_footsteps", 20, helper -> hears(helper, 95, Warnings.FORFEIT));
        tests.test("warning_forfeit_sends_a_cow_fleeing", 300, WarningTests::cowFlees);
    }

    private static void hears(GameTestHelper helper, int wickedness, Holder<SoundEvent> warning) {
        var player = Players.join(helper, PLAYER);
        var bystander = Players.join(helper, BYSTANDER);
        Wickedness.setThreshold(player, 100);
        Wickedness.set(player, wickedness);
        helper.succeedWhen(() -> {
            if (!player.heard.contains(warning)) {
                throw helper.assertionException(wickedness + " Wickedness of 100 sounded no " + warning.getRegisteredName());
            }
            var others = player.heard.stream().filter(sound -> WARNINGS.contains(sound) && !sound.equals(warning)).toList();
            if (!others.isEmpty()) {
                throw helper.assertionException(wickedness + " Wickedness of 100 sounded another band's Warning too");
            }
            if (bystander.heard.stream().anyMatch(WARNINGS::contains)) {
                throw helper.assertionException("a player beside them heard their Warning");
            }
        });
    }

    private static void cowFlees(GameTestHelper helper) {
        // In a corner of the platform, so the cow has its length to flee along.
        var player = Players.join(helper, CORNER);
        var cow = helper.spawn(EntityType.COW, COW);
        // Wandering, it might stray off by chance: only fleeing moves it now.
        cow.goalSelector.removeAllGoals(goal -> goal instanceof RandomStrollGoal);
        double before = cow.distanceTo(player);
        Wickedness.setThreshold(player, 100);
        Wickedness.set(player, 95);
        helper.succeedWhen(() -> {
            double now = cow.distanceTo(player);
            if (now < before + 4) {
                throw helper.assertionException("the cow is " + now + " blocks from a Forfeit player, from " + before);
            }
        });
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.wickedness.WarningSounds;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

/**
 * A player entering Marked, Overdue or Forfeit hears that band's Warning, sent to them alone, so a
 * player beside them hears none. While a player is Forfeit, a cow near them flees from them, on
 * Peaceful too.
 */
final class WarningTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BYSTANDER = new BlockPos(4, 1, 2);
    private static final BlockPos CORNER = new BlockPos(0, 1, 0);
    private static final BlockPos COW = new BlockPos(2, 1, 2);
    private static final List<Holder<SoundEvent>> WARNINGS = List.of(WarningSounds.MARKED, WarningSounds.OVERDUE, WarningSounds.FORFEIT);

    private WarningTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("warning_each_band_sounds_its_own_to_the_player_alone", 60, WarningTests::eachBand);
        // On Peaceful, where vanilla's own avoiding never starts, since no animal there may attack a player.
        tests.testOnPeaceful("warning_forfeit_sends_a_cow_fleeing", 300, WarningTests::cowFlees);
    }

    private static void eachBand(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bystander = Players.join(helper, BYSTANDER);
        Wickedness.setThreshold(player, 100);
        // Within each band, clear of where it starts.
        helper.startSequence()
                .thenExecute(() -> Wickedness.set(player, 60))
                .thenWaitUntil(() -> assertHeard(helper, player, WARNINGS.subList(0, 1)))
                .thenExecute(() -> Wickedness.set(player, 80))
                .thenWaitUntil(() -> assertHeard(helper, player, WARNINGS.subList(0, 2)))
                .thenExecute(() -> Wickedness.set(player, 95))
                .thenWaitUntil(() -> assertHeard(helper, player, WARNINGS))
                .thenExecute(() -> {
                    if (bystander.sounds().stream().anyMatch(WARNINGS::contains)) {
                        throw helper.assertionException("a player beside them heard their Warning");
                    }
                })
                .thenSucceed();
    }

    /** Asserts that the player has heard {@code warnings}, in order, and no other Warning. */
    private static void assertHeard(GameTestHelper helper, Players.Joined player,
            List<Holder<SoundEvent>> warnings) {
        var heard = player.sounds().stream()
                .filter(WARNINGS::contains)
                .map(Holder::getRegisteredName)
                .toList();
        var expected = warnings.stream().map(Holder::getRegisteredName).toList();
        if (!heard.equals(expected)) {
            throw helper.assertionException("the player heard " + heard + ", not " + expected);
        }
    }

    private static void cowFlees(GameTestHelper helper) {
        if (helper.getLevel().getDifficulty() != Difficulty.PEACEFUL) {
            helper.fail("the batch is on " + helper.getLevel().getDifficulty() + ", not Peaceful");
            return;
        }
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

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.Set;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Wickedness reaching the Threshold sends one Bailiff, 12 to 24 blocks from its player across the
 * ground, and as it arrives Wickedness drops to 0 and a new Threshold is drawn from 50 to 100. The
 * tests set a Threshold of 3, outside that range, so the redraw shows. No other Bailiff comes for
 * the player until the cooldown after that arrival is over. A Bailiff that finds no spot, or waits
 * out the cooldown, tries again every second, so the tests wait for it a few tries' worth. Its
 * arrival sounds at its player, wherever it arrived.
 */
final class ArrivalTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);
    private static final int COOLDOWN_SECONDS = 4;

    private ArrivalTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("arrival_crossing_the_threshold_sends_one_bailiff", 100, ArrivalTests::crossing);
        tests.test("arrival_below_the_threshold_sends_none", 20, ArrivalTests::below);
        tests.test("arrival_with_no_spot_tries_again", 100, ArrivalTests::triesAgain);
        tests.test("arrival_sounds_at_its_player", 100, ArrivalTests::soundsAtThePlayer);
        // In the batch after the others, since it shortens the cooldown every player's visits wait out.
        tests.testAlone("arrival_cooldown_holds_the_next_until_it_ends", 200, ArrivalTests::cooldown);
    }

    private static void crossing(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 3);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        helper.startSequence()
                .thenWaitUntil(() -> assertArrived(helper, player))
                .thenExecute(() -> {
                    // Gone at once, a second crossing still sends no other while the cooldown runs,
                    // nor do the tries every second after it.
                    Bailiffs.of(player).forEach(Entity::discard);
                    Wickedness.set(player, Wickedness.threshold(player));
                })
                .thenExecuteFor(40, () -> {
                    if (!Bailiffs.of(player).isEmpty()) {
                        throw helper.assertionException("crossing again within the cooldown sent another Bailiff");
                    }
                })
                .thenSucceed();
    }

    private static void cooldown(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        forceCooldown(helper, COOLDOWN_SECONDS);
        // Far enough over each Threshold that the day this batch's Decay test skips leaves it over.
        Wickedness.setThreshold(player, 3);
        Wickedness.set(player, 3 + 50);
        helper.startSequence()
                .thenWaitUntil(() -> assertArrived(helper, player))
                .thenExecute(() -> {
                    Bailiffs.of(player).forEach(Entity::discard);
                    Wickedness.set(player, Wickedness.threshold(player) + 50);
                })
                // Up to the cooldown's last ticks, counted from the arrival this step follows.
                .thenExecuteFor(COOLDOWN_SECONDS * 20 - 2, () -> {
                    if (!Bailiffs.of(player).isEmpty()) {
                        throw helper.assertionException("a Bailiff came within the cooldown after the last");
                    }
                })
                .thenWaitUntil(() -> assertArrived(helper, player))
                .thenSucceed();
    }

    /** Shortens the cooldown between visits to {@code seconds} until the test ends. */
    private static void forceCooldown(GameTestHelper helper, int seconds) {
        var spec = (ModConfigSpec) ModConfigs.getModConfigs(ATouchOfWickedness.MOD_ID).stream()
                .filter(config -> config.getType() == ModConfig.Type.COMMON).findFirst().orElseThrow().getSpec();
        ModConfigSpec.IntValue value = spec.getValues().get("bailiff.secondsBetweenVisits");
        int before = value.getAsInt();
        value.set(seconds);
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
                value.set(before);
            }

            @Override
            public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
                value.set(before);
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }
        });
    }

    private static void soundsAtThePlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 3);
        Wickedness.set(player, 3);
        helper.succeedWhen(() -> {
            var arrivals = player.heard.stream().filter(sound -> sound.getSound().value() == BailiffContent.ARRIVES.get()).toList();
            if (arrivals.size() != 1) {
                throw helper.assertionException("the player heard " + arrivals.size() + " arrivals, not 1");
            }
            var sound = arrivals.getFirst();
            double off = Math.sqrt(player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()));
            if (off > 1) {
                throw helper.assertionException("the arrival sounded " + off + " blocks from its player");
            }
        });
    }

    private static void triesAgain(GameTestHelper helper) {
        // High above the ground, where no Bailiff finds a spot within its reach of the player.
        var player = Players.join(helper, PLAYER.above(30));
        Wickedness.setThreshold(player, 3);
        Wickedness.set(player, 3);
        if (!Bailiffs.of(player).isEmpty() || Wickedness.of(player) != 3) {
            helper.fail("a Bailiff arrived with no ground within reach of its player");
            return;
        }
        var feet = helper.absoluteVec(PLAYER.getBottomCenter());
        player.teleportTo(helper.getLevel(), feet.x, feet.y, feet.z, Set.of(), 0, 0, true);
        helper.succeedWhen(() -> assertArrived(helper, player));
    }

    /**
     * Asserts that one Bailiff arrived for the player, the ground's distance away, and that their
     * Wickedness and Threshold were reset.
     */
    private static void assertArrived(GameTestHelper helper, ServerPlayer player) {
        var bailiffs = Bailiffs.of(player);
        if (bailiffs.size() != 1) {
            throw helper.assertionException(bailiffs.size() + " Bailiffs came for a player at their Threshold, not 1");
        }
        var bailiff = bailiffs.getFirst();
        double across = Math.hypot(bailiff.getX() - player.getX(), bailiff.getZ() - player.getZ());
        if (across < 12 || across > 24) {
            throw helper.assertionException("the Bailiff arrived " + across + " blocks from its player");
        }
        if (Wickedness.of(player) != 0) {
            throw helper.assertionException("Wickedness is " + Wickedness.of(player) + " after the Bailiff arrived, not 0");
        }
        int threshold = Wickedness.threshold(player);
        if (threshold < 50 || threshold > 100) {
            throw helper.assertionException("the Threshold drawn after the visit is " + threshold);
        }
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

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;
import java.util.Set;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The Bailiff in a harder world: it picks up items lying near it, its dead player's drops included,
 * toward its haul, and never comes for a dead player; on Peaceful it deals no damage and still
 * steals; each wicked player gets a Bailiff of their own, which anyone may kill; and it is gone once
 * its player logs out or changes dimension, and is never saved, so none is left after a restart.
 */
final class HarderWorldTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos OTHER_PLAYER = new BlockPos(6, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(5, 1, 5);
    /** The platform's middle, so a dead player's drops, thrown every way, stay on it. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private HarderWorldTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("harder_world_it_picks_up_an_item_on_the_ground", 100, HarderWorldTests::picksUp);
        tests.test("harder_world_it_picks_up_its_dead_players_drops", 200, HarderWorldTests::deadPlayersDrops);
        tests.test("harder_world_a_dead_player_is_not_visited", 100, HarderWorldTests::deadPlayer);
        tests.testOnPeaceful("harder_world_on_peaceful_it_deals_no_damage_and_still_steals", 200, HarderWorldTests::peaceful);
        tests.test("harder_world_two_wicked_players_each_get_their_own_and_another_player_kills_one", 200, HarderWorldTests::twoPlayers);
        tests.test("harder_world_it_is_gone_once_its_player_logs_out", 20, HarderWorldTests::logsOut);
        tests.test("harder_world_it_is_gone_once_its_player_changes_dimension", 20, HarderWorldTests::changesDimension);
        tests.test("harder_world_it_is_never_saved_so_none_is_left_after_a_restart", 1, HarderWorldTests::restart);
    }

    private static void picksUp(GameTestHelper helper) {
        // With nothing to take from its player, an item on the ground is its haul.
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var item = helper.spawnItem(Items.DIAMOND, BAILIFF.east().getBottomCenter());
        helper.succeedWhen(() -> {
            if (!item.isRemoved()) {
                throw helper.assertionException("the diamond still lies on the ground");
            }
            assertCarries(helper, bailiff, new ItemStack(Items.DIAMOND));
        });
    }

    private static void deadPlayersDrops(GameTestHelper helper) {
        var player = Players.join(helper, MIDDLE);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        player.kill(helper.getLevel());
        helper.succeedWhen(() -> {
            if (bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff left as its player died");
            }
            assertCarries(helper, bailiff, new ItemStack(Items.EMERALD, 7));
        });
    }

    private static void deadPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 3);
        player.kill(helper.getLevel());
        Wickedness.set(player, 3);
        // Past a retry, so the tries since the death are refused too.
        helper.startSequence()
                .thenExecuteFor(70, () -> {
                    if (!Bailiffs.of(player).isEmpty() || Wickedness.of(player) != 3) {
                        throw helper.assertionException("a Bailiff came for a dead player");
                    }
                })
                .thenSucceed();
    }

    private static void peaceful(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff was despawned on Peaceful");
            }
            if (player.getHealth() < player.getMaxHealth()) {
                throw helper.assertionException("the Bailiff hurt its player on Peaceful");
            }
            if (Bailiffs.stacksIn(player.getInventory()) != 0) {
                throw helper.assertionException("the Bailiff has not taken the player's emeralds");
            }
        });
    }

    private static void twoPlayers(GameTestHelper helper) {
        var first = Players.join(helper, PLAYER);
        var second = Players.join(helper, OTHER_PLAYER);
        Players.stock(first, new ItemStack(Items.EMERALD, 7));
        Players.stock(second, new ItemStack(Items.GOLD_INGOT, 9));
        for (var player : List.of(first, second)) {
            Wickedness.setThreshold(player, 3);
            Wickedness.set(player, 3);
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (var player : List.of(first, second)) {
                        if (Bailiffs.of(player).size() != 1) {
                            throw helper.assertionException("a wicked player with another's Bailiff about has "
                                    + Bailiffs.of(player).size() + " of their own, not 1");
                        }
                    }
                })
                .thenExecute(() -> {
                    var firsts = Bailiffs.of(first).getFirst();
                    if (firsts.cameFor(second)) {
                        throw helper.assertionException("the first player's Bailiff came for the second player too");
                    }
                    // Onto this test's platform, out of the other tests' structures, which clear their entities.
                    var onPlatform = helper.absoluteVec(BAILIFF.getBottomCenter());
                    firsts.teleportTo(onPlatform.x, onPlatform.y, onPlatform.z);
                    // Struck by another player, it still goes for its own, and not the one who struck it.
                    second.attack(firsts);
                })
                .thenExecuteFor(20, () -> {
                    var target = Bailiffs.of(first).getFirst().getTarget();
                    if (target != null && target != first) {
                        throw helper.assertionException("the first player's Bailiff went for another player");
                    }
                })
                .thenExecute(() -> WickedKillTests.killInMelee(helper, second, Bailiffs.of(first).getFirst()))
                .thenSucceed();
    }

    private static void logsOut(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.getLevel().getServer().getPlayerList().remove(player);
        helper.succeedWhen(() -> {
            if (!bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff stayed after its player logged out");
            }
        });
    }

    private static void changesDimension(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        player.teleportTo(nether, 0.5, 64, 0.5, Set.of(), 0, 0, true);
        helper.succeedWhen(() -> {
            if (!bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff stayed after its player changed dimension");
            }
        });
    }

    private static void restart(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        // What its chunk writes as the server stops: nothing, so none comes back as it starts.
        if (bailiff.save(saved)) {
            helper.fail("the Bailiff was saved with its chunk");
            return;
        }
        helper.succeed();
    }

    /** Asserts that {@code bailiff} carries {@code expected}, all it has taken. */
    private static void assertCarries(GameTestHelper helper, Bailiff bailiff, ItemStack expected) {
        var report = bailiff.report(helper.getLevel());
        var carried = report == null ? List.<ItemStack>of() : report.carried();
        if (carried.size() != 1 || !ItemStack.matches(carried.getFirst(), expected)) {
            throw helper.assertionException("the Bailiff carries " + carried + ", not " + expected);
        }
    }
}

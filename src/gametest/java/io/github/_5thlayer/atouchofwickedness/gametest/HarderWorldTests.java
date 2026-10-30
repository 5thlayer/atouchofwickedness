// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;
import java.util.Set;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.discovery.Discoveries;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The Bailiff in a harder world: it picks up items lying near it, its dead player's drops included,
 * toward its haul, and never comes for a dead player; on Peaceful it deals no damage and still
 * steals, though not through a raised shield; each wicked player gets a Bailiff of their own,
 * which anyone may kill; and it is gone once its player logs out or changes dimension, respawning
 * included, leaving them owing, and is never saved, so none is left after a restart.
 */
final class HarderWorldTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos OTHER_PLAYER = new BlockPos(6, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(5, 1, 5);
    /** Beside {@link #PLAYER}, within a hit's reach. */
    private static final BlockPos SHIELD_SIDE = new BlockPos(3, 1, 2);
    /** The platform's middle, so a dead player's drops, thrown every way, stay on it. */
    private static final BlockPos MIDDLE = new BlockPos(4, 1, 4);

    private HarderWorldTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("harder_world_it_picks_up_an_item_on_the_ground_a_seizure", 20, HarderWorldTests::picksUp);
        tests.test("harder_world_it_picks_up_its_dead_players_drops", 200, HarderWorldTests::deadPlayersDrops);
        tests.test("harder_world_a_dead_player_is_not_visited", 100, HarderWorldTests::deadPlayer);
        tests.testOnPeaceful("harder_world_on_peaceful_it_deals_no_damage_and_still_steals", 200, HarderWorldTests::peaceful);
        tests.testOnPeaceful("harder_world_on_peaceful_a_raised_shield_still_stops_its_theft", 20, HarderWorldTests::shielded);
        tests.test("harder_world_two_wicked_players_each_get_their_own_and_another_player_kills_one", 200, HarderWorldTests::twoPlayers);
        tests.test("harder_world_it_is_gone_once_its_player_logs_out", 20, HarderWorldTests::logsOut);
        tests.test("harder_world_it_is_gone_once_its_player_changes_dimension", 20, HarderWorldTests::changesDimension);
        tests.test("harder_world_it_is_gone_once_its_player_respawns_in_another_dimension", 20, HarderWorldTests::respawnsElsewhere);
        tests.test("harder_world_it_is_never_saved_so_none_is_left_after_a_restart", 1, HarderWorldTests::restart);
    }

    private static void picksUp(GameTestHelper helper) {
        // With nothing to take from its player, an item on the ground is its haul, and its seizure.
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        // At its feet, so it picks it up as its goals next choose, with no walk to it.
        var item = helper.spawnItem(Items.DIAMOND, BAILIFF.getBottomCenter());
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    if (!item.isRemoved()) {
                        throw helper.assertionException("the diamond still lies at the Bailiff's feet");
                    }
                    Bailiffs.assertCarries(helper, bailiff, new ItemStack(Items.DIAMOND));
                    helper.assertTrue(Discoveries.record().isKnown(player, Discoveries.SEIZURE), "the pick-up did not discover the seizure");
                })
                .thenSucceed();
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
            Bailiffs.assertCarries(helper, bailiff, new ItemStack(Items.EMERALD, 7));
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
        assertEndedOwing(helper, bailiff, player, "logged out");
    }

    private static void changesDimension(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        player.teleportTo(nether, 0.5, 64, 0.5, Set.of(), 0, 0, true);
        assertEndedOwing(helper, bailiff, player, "changed dimension");
    }

    private static void respawnsElsewhere(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        // Above the Nether's roof, where the air is free to respawn in; a respawn changes dimension
        // without the event a teleport posts, so the Bailiff finds its player gone as it ticks.
        var roof = new BlockPos(0, 130, 0);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                new LevelData.RespawnData(GlobalPos.of(Level.NETHER, roof), 0, 0), true), false);
        player.kill(helper.getLevel());
        var server = helper.getLevel().getServer();
        var respawned = server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        if (respawned.level().dimension() != Level.NETHER) {
            helper.fail("the player respawned in " + respawned.level().dimension().identifier() + ", not the Nether");
            return;
        }
        helper.succeedWhen(() -> {
            if (!bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff stayed after its player respawned in another dimension");
            }
            if (Wickedness.of(respawned) < Wickedness.threshold(respawned)) {
                throw helper.assertionException("the Bailiff whose player respawned elsewhere left them at "
                        + Wickedness.of(respawned) + " Wickedness, under their Threshold of " + Wickedness.threshold(respawned));
            }
        });
    }

    /**
     * Asserts that {@code bailiff}, which took nothing, was gone the moment its player {@code left},
     * and left them owing.
     */
    private static void assertEndedOwing(GameTestHelper helper, Bailiff bailiff, ServerPlayer player, String left) {
        if (!bailiff.isRemoved()) {
            helper.fail("the Bailiff stayed after its player " + left);
            return;
        }
        if (Wickedness.of(player) < Wickedness.threshold(player)) {
            helper.fail("the Bailiff whose player " + left + " left them at " + Wickedness.of(player)
                    + " Wickedness, under their Threshold of " + Wickedness.threshold(player));
            return;
        }
        helper.succeed();
    }

    private static void shielded(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        var bailiff = Bailiffs.comeFor(helper, SHIELD_SIDE, player);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, bailiff.getEyePosition());
        player.setYHeadRot(player.getYRot());
        player.raise(InteractionHand.OFF_HAND);
        bailiff.doHurtTarget(helper.getLevel(), player);
        if (Bailiffs.stacksIn(player.getInventory()) != 1) {
            helper.fail("on Peaceful the Bailiff stole through the shield its player raised against it");
            return;
        }
        player.stopUsingItem();
        bailiff.doHurtTarget(helper.getLevel(), player);
        if (Bailiffs.stacksIn(player.getInventory()) != 0) {
            helper.fail("on Peaceful the Bailiff stole nothing once its player lowered their shield");
            return;
        }
        helper.succeed();
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
}

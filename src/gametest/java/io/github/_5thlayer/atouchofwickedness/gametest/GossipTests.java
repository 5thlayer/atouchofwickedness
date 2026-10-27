// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;

/**
 * Each villager who sees a Wicked kill gains vanilla's minor negative gossip about the killer, the
 * kill's effective weight times the scale, 5 by default: a cow, weighing 2, adds 10. A villager behind a
 * wall sees nothing and gains none. The game-test pack weighs a mooshroom 11.
 */
final class GossipTests {

    private static final BlockPos KILLER = new BlockPos(4, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);
    private static final BlockPos WITNESS = new BlockPos(4, 1, 7);
    private static final BlockPos HIDDEN = new BlockPos(8, 1, 4);

    private GossipTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("gossip_villager_who_sees_a_kill_gossips_and_one_behind_a_wall_does_not", 20, GossipTests::seeingAKill);
        tests.test("gossip_heavier_kill_adds_more", 20, GossipTests::heavierKill);
    }

    private static void seeingAKill(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
            }
        }
        var player = killer(helper, KILLER);
        var witness = helper.spawnWithNoFreeWill(EntityType.VILLAGER, WITNESS);
        var hidden = helper.spawnWithNoFreeWill(EntityType.VILLAGER, HIDDEN);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        int seen = gossip(witness, player);
        if (seen != 10) {
            helper.fail("the villager who saw a cow killed holds " + seen + " minor negative gossip about the killer, not 10");
            return;
        }
        int unseen = gossip(hidden, player);
        if (unseen != 0) {
            helper.fail("the villager behind the wall holds " + unseen + " minor negative gossip about the killer, not 0");
            return;
        }
        helper.succeed();
    }

    private static void heavierKill(GameTestHelper helper) {
        var witness = helper.spawnWithNoFreeWill(EntityType.VILLAGER, WITNESS);
        var cowKiller = killer(helper, KILLER);
        WickedKillTests.killInMelee(helper, cowKiller, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        var mooshroomKiller = killer(helper, new BlockPos(2, 1, 4));
        WickedKillTests.killInMelee(helper, mooshroomKiller, helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, CREATURE));
        int light = gossip(witness, cowKiller);
        int heavy = gossip(witness, mooshroomKiller);
        if (heavy <= light) {
            helper.fail("a mooshroom's kill left " + heavy + " minor negative gossip, no more than a cow's " + light);
            return;
        }
        helper.succeed();
    }

    private static ServerPlayer killer(GameTestHelper helper, BlockPos at) {
        var player = Players.join(helper, at);
        // Far above anything a kill adds, so no Bailiff comes.
        Wickedness.setThreshold(player, 1_000);
        return player;
    }

    /** @return the minor negative gossip {@code villager} holds about {@code player} */
    private static int gossip(Villager villager, ServerPlayer player) {
        return -villager.getGossips().getReputation(player.getUUID(), type -> type == GossipType.MINOR_NEGATIVE);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.discovery.Discoveries;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Discovery (suite ADR-0006): each of the Pillar's own triggers discovers its thing, the advancement
 * {@code atouchofwickedness:discovery/<id>}, for the player it happened to and for no one else, and
 * with the {@code discovery} config off everything is known. Holding one of the Pillar's items is
 * vanilla's {@code inventory_changed}, so no test of it is here.
 */
final class DiscoveryTests {

    private static final BlockPos KILLER = new BlockPos(4, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);
    private static final BlockPos WITNESS = new BlockPos(4, 1, 7);
    private static final BlockPos BYSTANDER = new BlockPos(1, 1, 1);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);

    private DiscoveryTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("discovery_a_wicked_kill_a_villager_sees_is_its_killers_alone", 20, DiscoveryTests::wickedKill);
        tests.test("discovery_each_standing_on_reaching_it_or_one_above", 100, DiscoveryTests::standings);
        tests.test("discovery_the_bailiff_coming_seizing_and_leaving_owing", 20, DiscoveryTests::bailiff);
        tests.test("discovery_atonement_only_once_it_lowers_wickedness", 20, DiscoveryTests::atonement);
        tests.test("discovery_switched_off_everything_is_known", 20, DiscoveryTests::switchedOff);
    }

    /** A cow killed before a villager discovers the Wicked kill and its gossip for the killer, and nothing for a bystander. */
    private static void wickedKill(GameTestHelper helper) {
        var killer = Players.join(helper, KILLER);
        var bystander = Players.join(helper, BYSTANDER);
        // Far above anything a kill adds, so no Bailiff comes.
        Wickedness.setThreshold(killer, 1_000);
        helper.spawnWithNoFreeWill(EntityType.VILLAGER, WITNESS);
        expectKnown(helper, killer, Discoveries.WICKED_KILL, false);
        WickedKillTests.killInMelee(helper, killer, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        expectKnown(helper, killer, Discoveries.WICKED_KILL, true);
        expectKnown(helper, killer, Discoveries.GOSSIP, true);
        expectKnown(helper, killer, Discoveries.ATONEMENT, false);
        expectKnown(helper, bystander, Discoveries.WICKED_KILL, false);
        expectKnown(helper, bystander, Discoveries.GOSSIP, false);
        helper.succeed();
    }

    /** Marked is discovered on reaching it, and a leap from Marked to Forfeit discovers Overdue on the way. */
    private static void standings(GameTestHelper helper) {
        var player = Players.join(helper, KILLER);
        Wickedness.setThreshold(player, 100);
        helper.startSequence()
                .thenExecute(() -> Wickedness.set(player, 60))
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.MARKED, true))
                .thenExecute(() -> {
                    expectKnown(helper, player, Discoveries.OVERDUE, false);
                    Wickedness.set(player, 95);
                })
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.FORFEIT, true))
                .thenExecute(() -> {
                    expectKnown(helper, player, Discoveries.OVERDUE, true);
                    expectKnown(helper, player, Discoveries.BAILIFF, false);
                })
                .thenSucceed();
    }

    /**
     * A Bailiff come for a player discovers it, and Forfeit, which they reached to bring it; its hit
     * discovers the seizure. Another's, which found nothing to take as its visit ended, discovers owing.
     */
    private static void bailiff(GameTestHelper helper) {
        var robbed = Players.join(helper, KILLER);
        Players.stock(robbed, new ItemStack(Items.IRON_INGOT, 12));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, robbed);
        expectKnown(helper, robbed, Discoveries.BAILIFF, true);
        expectKnown(helper, robbed, Discoveries.FORFEIT, true);
        expectKnown(helper, robbed, Discoveries.SEIZURE, false);
        Bailiffs.hit(helper, bailiff, robbed);
        expectKnown(helper, robbed, Discoveries.SEIZURE, true);
        expectKnown(helper, robbed, Discoveries.OWING, false);

        var owing = Players.join(helper, BYSTANDER);
        Bailiffs.comeFor(helper, CREATURE, owing);
        Bailiff.endVisits(helper.getLevel(), owing);
        expectKnown(helper, owing, Discoveries.OWING, true);
        expectKnown(helper, owing, Discoveries.SEIZURE, false);
        helper.succeed();
    }

    /** An Atonement with no Wickedness to lower discovers nothing; one that lowers it discovers Atonement. */
    private static void atonement(GameTestHelper helper) {
        var player = Players.join(helper, KILLER);
        Wickedness.setThreshold(player, 1_000);
        Wickedness.atone(player, 10);
        expectKnown(helper, player, Discoveries.ATONEMENT, false);
        Wickedness.set(player, 20);
        Wickedness.atone(player, 10);
        expectKnown(helper, player, Discoveries.ATONEMENT, true);
        helper.succeed();
    }

    /** With Discovery off, a new player knows every thing; set and set back in one tick, so no other test sees it. */
    private static void switchedOff(GameTestHelper helper) {
        var player = Players.join(helper, BYSTANDER);
        ModConfigSpec.BooleanValue discovery = Configs.common("discovery");
        boolean before = discovery.getAsBoolean();
        discovery.set(false);
        try {
            for (var id : Discoveries.IDS) {
                expectKnown(helper, player, id, true);
            }
        } finally {
            discovery.set(before);
        }
        expectKnown(helper, player, Discoveries.WICKED_KILL, false);
        helper.succeed();
    }

    private static void expectKnown(GameTestHelper helper, ServerPlayer player, String id, boolean known) {
        helper.assertTrue(Discoveries.record().isKnown(player, id) == known, id + (known ? " is not known" : " is known too soon"));
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A fleeing Bailiff far enough from its player stands still charging its teleport, then vanishes with
 * what it took, dropping none of it. When it flees and how long it charges are its Visit's, pinned in
 * {@code VisitTest}.
 */
final class FleeTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);

    private FleeTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("flee_far_from_its_player_it_charges_then_vanishes_with_its_goods", 200, FleeTests::vanishes);
    }

    private static void vanishes(GameTestHelper helper) {
        // High in the air, beyond the vanishing distance of a Bailiff on the platform.
        var player = Players.join(helper, PLAYER.above(30));
        for (int count = 1; count <= 5; count++) {
            Players.stock(player, new ItemStack(Items.DIAMOND, count));
        }
        Bailiff bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        // Hit as it turns to taunt, it flees at once.
        player.attack(bailiff);
        var around = new AABB(bailiff.blockPosition()).inflate(64);
        Vec3[] chargedFrom = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!bailiff.isCharging()) {
                        throw helper.assertionException("the Bailiff has not started charging its teleport");
                    }
                })
                .thenExecute(() -> chargedFrom[0] = bailiff.position())
                // Its player's chance to kill it from afar: it stands still and stays.
                .thenExecuteFor(Math.max(0, WickednessConfig.ticksToTeleport() - 2), () -> {
                    if (bailiff.isRemoved()) {
                        throw helper.assertionException("the Bailiff vanished before it had charged its teleport");
                    }
                    if (bailiff.position().subtract(chargedFrom[0]).horizontalDistance() > 0.01) {
                        throw helper.assertionException("the Bailiff moved while it charged its teleport");
                    }
                })
                .thenWaitUntil(() -> {
                    if (!bailiff.isRemoved()) {
                        throw helper.assertionException("the Bailiff is still here");
                    }
                })
                .thenExecute(() -> {
                    var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Items.DIAMOND));
                    if (!dropped.isEmpty()) {
                        throw helper.assertionException("the Bailiff left " + dropped.size() + " stacks of diamonds behind");
                    }
                })
                .thenSucceed();
    }
}

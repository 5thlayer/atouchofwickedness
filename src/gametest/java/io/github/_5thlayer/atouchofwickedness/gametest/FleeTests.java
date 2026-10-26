// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Bailiff flees once it has taken a stack and taunted its player, or 60 seconds after it arrived, and once far enough
 * from its player it stands still charging its teleport, then vanishes with what it took, dropping
 * none of it.
 */
final class FleeTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);

    private FleeTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("flee_after_one_stack_and_a_taunt", 200, FleeTests::afterOneStack);
        tests.test("flee_after_sixty_seconds", 1_260, FleeTests::afterSixtySeconds);
        tests.test("flee_far_from_its_player_it_charges_then_vanishes_with_its_goods", 200, FleeTests::vanishes);
    }

    private static void afterOneStack(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.COBBLESTONE, 1), new ItemStack(Items.COBBLESTONE, 2));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        if (bailiff.isFleeing()) {
            helper.fail("the Bailiff fled before it taunted its player");
            return;
        }
        helper.succeedWhen(() -> {
            if (!bailiff.isFleeing()) {
                throw helper.assertionException("the Bailiff has not fled");
            }
            if (player.heard.stream().noneMatch(sound -> sound.getSound().is(BailiffContent.TAUNTS.getKey()))) {
                throw helper.assertionException("the Bailiff fled without taunting its player");
            }
        });
    }

    private static void afterSixtySeconds(GameTestHelper helper) {
        // Out of its reach in the air, so it never lands a hit and its time runs out.
        var player = Players.join(helper, PLAYER.above(12));
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.runAtTickTime(1_190, () -> {
            if (bailiff.isFleeing()) {
                helper.fail("the Bailiff fled before its 60 seconds were up");
            }
        });
        helper.runAtTickTime(1_210, () -> {
            if (!bailiff.isFleeing()) {
                helper.fail("the Bailiff had not fled 60 seconds after it arrived");
                return;
            }
            helper.succeed();
        });
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

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * A Bailiff flees once it has taken 5 stacks, or 60 seconds after it arrived, and a few seconds out
 * of its player's sight it vanishes with what it took, dropping none of it.
 */
final class FleeTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);

    private FleeTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("flee_after_five_stacks", 20, FleeTests::afterFiveStacks);
        tests.test("flee_after_sixty_seconds", 1_260, FleeTests::afterSixtySeconds);
        tests.test("flee_vanishes_with_its_goods_out_of_sight", 300, FleeTests::vanishes);
    }

    private static void afterFiveStacks(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        for (int count = 1; count <= 7; count++) {
            Players.stock(player, new ItemStack(Items.COBBLESTONE, count));
        }
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        for (int hit = 1; hit <= 4; hit++) {
            Bailiffs.hit(helper, bailiff, player);
        }
        if (bailiff.isFleeing()) {
            helper.fail("the Bailiff fled after 4 stacks");
            return;
        }
        Bailiffs.hit(helper, bailiff, player);
        if (!bailiff.isFleeing()) {
            helper.fail("the Bailiff did not flee after 5 stacks");
            return;
        }
        // Fleeing, it takes nothing more.
        bailiff.doHurtTarget(helper.getLevel(), player);
        if (Bailiffs.stacksIn(player.getInventory()) != 2) {
            helper.fail("the player has " + Bailiffs.stacksIn(player.getInventory()) + " stacks left, not 2");
            return;
        }
        helper.succeed();
    }

    private static void afterSixtySeconds(GameTestHelper helper) {
        // Out of its reach in the air, so it takes nothing and its time runs out.
        var player = Players.join(helper, PLAYER.above(12));
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
        var player = Players.join(helper, PLAYER);
        for (int count = 1; count <= 5; count++) {
            Players.stock(player, new ItemStack(Items.DIAMOND, count));
        }
        Bailiff bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        for (int hit = 1; hit <= 5; hit++) {
            Bailiffs.hit(helper, bailiff, player);
        }
        // Walled in, the player sees nothing.
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0 || y == 2) {
                        helper.setBlock(PLAYER.offset(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        var around = new AABB(bailiff.blockPosition()).inflate(64);
        helper.succeedWhen(() -> {
            if (!bailiff.isRemoved()) {
                throw helper.assertionException("the Bailiff is still here");
            }
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, item -> item.getItem().is(Items.DIAMOND));
            if (!dropped.isEmpty()) {
                throw helper.assertionException("the Bailiff left " + dropped.size() + " stacks of diamonds behind");
            }
        });
    }
}

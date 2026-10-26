// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A Bailiff robs the Seizable containers its player opened near where it arrived before it goes
 * for them: never one they didn't open, another player's, an ender chest or a shulker box, nor one
 * out of reach. Hit while it robs, it turns on its player.
 */
final class RobberyTests {

    private static final BlockPos PLAYER = new BlockPos(6, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);
    private static final BlockPos OPENED = new BlockPos(2, 1, 6);
    private static final BlockPos BESIDE = new BlockPos(4, 1, 6);
    /** Straight above the platform, beyond the robbing radius of the Bailiff on it. */
    private static final BlockPos OUT_OF_REACH = new BlockPos(4, 41, 4);

    private RobberyTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("robbery_the_chests_the_player_opened_are_robbed_then_the_player_but_not_one_beside_them", 400, RobberyTests::openedIsRobbed);
        tests.test("robbery_another_players_chest_is_never_robbed", 300, RobberyTests::anotherPlayers);
        tests.test("robbery_an_ender_chest_or_a_shulker_box_is_never_robbed", 300, RobberyTests::enderChestAndShulkerBox);
        tests.test("robbery_hit_while_robbing_it_turns_on_its_player", 400, RobberyTests::hitWhileRobbing);
        tests.test("robbery_with_no_container_in_reach_it_goes_for_its_player", 200, RobberyTests::noneInReach);
        tests.test("robbery_a_block_a_datapack_adds_to_the_tag_is_robbed", 300, RobberyTests::datapackTag);
    }

    private static void openedIsRobbed(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var alsoOpened = new BlockPos(2, 1, 2);
        stocked(helper, OPENED, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        stocked(helper, alsoOpened, Blocks.BARREL, new ItemStack(Items.COAL, 5), new ItemStack(Items.COPPER_INGOT, 2));
        stocked(helper, BESIDE, Blocks.CHEST, new ItemStack(Items.EMERALD, 4), new ItemStack(Items.IRON_INGOT, 6));
        open(helper, player, OPENED);
        open(helper, player, alsoOpened);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.failIfEver(() -> assertStacks(helper, BESIDE, 2, "the Bailiff robbed a chest its player never opened"));
        // Four stacks from the two it robs, and the fifth of its quota from its player.
        helper.succeedWhen(() -> {
            assertStacks(helper, OPENED, 0, "the Bailiff has not robbed the chest its player opened");
            assertStacks(helper, alsoOpened, 0, "the Bailiff has not robbed the barrel its player opened");
            assertRobbedOf(helper, player);
        });
    }

    private static void anotherPlayers(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var other = Players.join(helper, new BlockPos(2, 1, 2));
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        stocked(helper, OPENED, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        open(helper, other, OPENED);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.failIfEver(() -> assertStacks(helper, OPENED, 2, "the Bailiff robbed a chest another player opened"));
        helper.succeedWhen(() -> assertRobbedOf(helper, player));
    }

    private static void enderChestAndShulkerBox(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 3));
        helper.setBlock(OPENED, Blocks.ENDER_CHEST);
        stocked(helper, BESIDE, Blocks.SHULKER_BOX, new ItemStack(Items.GOLD_INGOT, 9));
        open(helper, player, OPENED);
        open(helper, player, BESIDE);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.failIfEver(() -> {
            if (player.getEnderChestInventory().getItem(0).isEmpty()) {
                throw helper.assertionException("the Bailiff robbed the player's ender chest");
            }
            assertStacks(helper, BESIDE, 1, "the Bailiff robbed a shulker box");
        });
        // A container it robbed it robs before it goes for its player.
        helper.succeedWhen(() -> assertRobbedOf(helper, player));
    }

    private static void hitWhileRobbing(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        int quota = WickednessConfig.quota();
        for (int stack = 0; stack < quota; stack++) {
            Players.stock(player, new ItemStack(Items.EMERALD, stack + 1));
        }
        var stacks = new ItemStack[quota + 3];
        for (int stack = 0; stack < stacks.length; stack++) {
            stacks[stack] = new ItemStack(Items.LAPIS_LAZULI, stack + 1);
        }
        stocked(helper, OPENED, Blocks.CHEST, stacks);
        open(helper, player, OPENED);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        // Its hits mustn't kill the player before it has met its quota.
        helper.onEachTick(() -> player.setHealth(player.getMaxHealth()));
        int[] leftAtTheHit = new int[1];
        helper.startSequence()
                .thenWaitUntil(() -> assertStacks(helper, OPENED, stacks.length - 1, "the Bailiff has not started robbing the chest"))
                .thenExecute(() -> {
                    leftAtTheHit[0] = stacksIn(helper, OPENED);
                    player.attack(bailiff);
                    if (bailiff.isRobbing()) {
                        throw helper.assertionException("hit while robbing, the Bailiff went on robbing");
                    }
                })
                .thenWaitUntil(() -> {
                    if (!bailiff.isFleeing()) {
                        throw helper.assertionException("the Bailiff has not met its quota");
                    }
                })
                .thenExecute(() -> {
                    assertStacks(helper, OPENED, leftAtTheHit[0], "the Bailiff robbed the chest after its player hit it");
                    int fromTheChest = stacks.length - leftAtTheHit[0];
                    int fromThePlayer = quota - Bailiffs.stacksIn(player.getInventory());
                    if (fromThePlayer != quota - fromTheChest) {
                        throw helper.assertionException("after " + fromTheChest + " from the chest the Bailiff took "
                                + fromThePlayer + " stacks from its player, not the rest of its quota");
                    }
                })
                .thenSucceed();
    }

    private static void noneInReach(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        stocked(helper, OUT_OF_REACH, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3));
        open(helper, player, OUT_OF_REACH);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        if (bailiff.isRobbing()) {
            helper.fail("the Bailiff set out to rob a chest beyond its robbing radius");
            return;
        }
        helper.startSequence()
                .thenWaitUntil(() -> assertRobbedOf(helper, player))
                // Outside the structure, so nothing else clears it.
                .thenExecute(() -> helper.setBlock(OUT_OF_REACH, Blocks.AIR))
                .thenSucceed();
    }

    private static void datapackTag(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        // The game-test pack adds the dispenser to the tag.
        stocked(helper, OPENED, Blocks.DISPENSER, new ItemStack(Items.ARROW, 16));
        open(helper, player, OPENED);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> assertStacks(helper, OPENED, 0, "the Bailiff has not robbed the dispenser its player opened"));
    }

    /** Places {@code block} at {@code relative}, holding {@code stacks} in its first slots. */
    private static void stocked(GameTestHelper helper, BlockPos relative, Block block, ItemStack... stacks) {
        helper.setBlock(relative, block);
        var container = container(helper, relative);
        for (int slot = 0; slot < stacks.length; slot++) {
            container.setItem(slot, stacks[slot]);
        }
    }

    /** The player opens the block at {@code relative}, as with a right click, and closes it again. */
    private static void open(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        helper.useBlock(relative, player);
        player.closeContainer();
    }

    private static Container container(GameTestHelper helper, BlockPos relative) {
        return (Container) helper.getBlockEntity(relative, BlockEntity.class);
    }

    private static int stacksIn(GameTestHelper helper, BlockPos relative) {
        var container = container(helper, relative);
        int stacks = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                stacks++;
            }
        }
        return stacks;
    }

    private static void assertStacks(GameTestHelper helper, BlockPos relative, int expected, String message) {
        int stacks = stacksIn(helper, relative);
        if (stacks != expected) {
            throw helper.assertionException(message + ": it holds " + stacks + " stacks, not " + expected);
        }
    }

    private static void assertRobbedOf(GameTestHelper helper, ServerPlayer player) {
        if (Bailiffs.stacksIn(player.getInventory()) != 0) {
            throw helper.assertionException("the Bailiff has not taken the player's stack");
        }
    }
}

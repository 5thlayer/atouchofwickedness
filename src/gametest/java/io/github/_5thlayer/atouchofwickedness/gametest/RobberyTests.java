// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * A Bailiff with Seizable containers its player opened near where it arrived takes one stack from
 * the nearest, taunts its player and flees, and never mugs them: never from one they didn't open,
 * another player's, an ender chest or a shulker box, nor one out of reach. Hit by its player as it
 * robs, it closes the chest and flees at once.
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
        tests.test("robbery_one_stack_from_the_nearest_container_the_player_opened_then_a_taunt", 400, RobberyTests::openedIsRobbed);
        tests.test("robbery_another_players_chest_is_never_robbed", 300, RobberyTests::anotherPlayers);
        tests.test("robbery_an_ender_chest_or_a_shulker_box_is_never_robbed", 300, RobberyTests::enderChestAndShulkerBox);
        tests.test("robbery_hit_as_it_robs_it_closes_the_chest_and_flees_with_nothing", 60, RobberyTests::hitWhileRobbing);
        tests.test("robbery_with_no_container_in_reach_it_goes_for_its_player", 200, RobberyTests::noneInReach);
        tests.test("robbery_a_block_a_datapack_adds_to_the_tag_is_robbed", 300, RobberyTests::datapackTag);
    }

    private static void openedIsRobbed(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var farther = new BlockPos(2, 1, 2);
        Containers.stocked(helper, OPENED, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        Containers.stocked(helper, farther, Blocks.BARREL, new ItemStack(Items.COAL, 5), new ItemStack(Items.COPPER_INGOT, 2));
        // Nearer the Bailiff than either, but never opened.
        Containers.stocked(helper, BESIDE, Blocks.CHEST, new ItemStack(Items.EMERALD, 4), new ItemStack(Items.IRON_INGOT, 6));
        Containers.open(helper, player, OPENED);
        Containers.open(helper, player, farther);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!bailiff.isFleeing()) {
                        throw helper.assertionException("the Bailiff has not fled");
                    }
                })
                .thenExecute(() -> {
                    assertStacks(helper, OPENED, 1, "the Bailiff did not take one stack from the nearest chest its player opened");
                    assertStacks(helper, farther, 2, "the Bailiff robbed a second container");
                    assertStacks(helper, BESIDE, 2, "the Bailiff robbed a chest its player never opened");
                    if (Bailiffs.stacksIn(player.getInventory()) != 1) {
                        throw helper.assertionException("the Bailiff mugged its player after robbing their chest");
                    }
                    if (player.heard.stream().noneMatch(sound -> sound.getSound().is(BailiffContent.TAUNTS.getKey()))) {
                        throw helper.assertionException("the player never heard the Bailiff taunt them");
                    }
                    if (player.actionBar.stream().noneMatch(shown -> shown.getContents() instanceof TranslatableContents translatable
                            && translatable.getKey().equals("entity.atouchofwickedness.bailiff.taunt"))) {
                        throw helper.assertionException("the player was never told the Bailiff took its due");
                    }
                })
                .thenSucceed();
    }

    private static void anotherPlayers(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var other = Players.join(helper, new BlockPos(2, 1, 2));
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        Containers.stocked(helper, OPENED, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        Containers.open(helper, other, OPENED);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.failIfEver(() -> assertStacks(helper, OPENED, 2, "the Bailiff robbed a chest another player opened"));
        helper.succeedWhen(() -> assertRobbedOf(helper, player));
    }

    private static void enderChestAndShulkerBox(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.LAPIS_LAZULI, 3));
        helper.setBlock(OPENED, Blocks.ENDER_CHEST);
        Containers.stocked(helper, BESIDE, Blocks.SHULKER_BOX, new ItemStack(Items.GOLD_INGOT, 9));
        Containers.open(helper, player, OPENED);
        Containers.open(helper, player, BESIDE);
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
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        Containers.stocked(helper, OPENED, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        Containers.open(helper, player, OPENED);
        // Within reach of the chest, so it opens it as its goals next choose, with no walk to it.
        var bailiff = Bailiffs.comeFor(helper, BESIDE, player);
        var chest = helper.absolutePos(OPENED);
        helper.startSequence()
                .thenIdle(4)
                .thenExecute(() -> {
                    if (ChestBlockEntity.getOpenCount(helper.getLevel(), chest) == 0) {
                        throw helper.assertionException("the Bailiff has not opened the chest");
                    }
                    player.attack(bailiff);
                    if (!bailiff.isFleeing()) {
                        throw helper.assertionException("hit as it robbed, the Bailiff did not flee");
                    }
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    if (ChestBlockEntity.getOpenCount(helper.getLevel(), chest) != 0) {
                        throw helper.assertionException("the Bailiff left the chest open as it fled");
                    }
                })
                .thenExecuteFor(40, () -> {
                    assertStacks(helper, OPENED, 2, "the Bailiff robbed the chest after its player hit it");
                    if (Bailiffs.stacksIn(player.getInventory()) != 1) {
                        throw helper.assertionException("the Bailiff mugged its player after they hit it");
                    }
                })
                .thenSucceed();
    }

    private static void noneInReach(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        Containers.stocked(helper, OUT_OF_REACH, Blocks.CHEST, new ItemStack(Items.LAPIS_LAZULI, 3));
        Containers.open(helper, player, OUT_OF_REACH);
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
        Containers.stocked(helper, OPENED, Blocks.DISPENSER, new ItemStack(Items.ARROW, 16));
        Containers.open(helper, player, OPENED);
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> assertStacks(helper, OPENED, 0, "the Bailiff has not robbed the dispenser its player opened"));
    }

    private static void assertStacks(GameTestHelper helper, BlockPos relative, int expected, String message) {
        int stacks = Containers.stacksIn(helper, relative);
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

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * The Bailiff weighs what it takes by Value: from a container or its player it never takes a stack
 * worth nothing beside one worth taking, a container holding only junk it passes over, and a player
 * carrying only junk it taunts empty-handed, leaving them owing. On the ground it leaves junk and
 * goes on to its player, but goes for what a pack values, and for what its enchantments make worth
 * it.
 */
final class ValueTests {

    private static final BlockPos PLAYER = new BlockPos(6, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);
    private static final BlockPos NEARER = new BlockPos(2, 1, 6);
    private static final BlockPos FARTHER = new BlockPos(2, 1, 2);
    /** Beside the Bailiff, well within its pickup radius. */
    private static final BlockPos GROUND = new BlockPos(5, 1, 6);

    private ValueTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("value_robbing_it_takes_the_stack_worth_taking_and_leaves_junk", 400, ValueTests::robsWhatIsWorthTaking);
        tests.test("value_a_container_holding_only_junk_is_passed_over", 600, ValueTests::passesOverJunk);
        tests.test("value_mugging_it_takes_the_stack_worth_taking_and_leaves_junk", 20, ValueTests::mugsWhatIsWorthTaking);
        tests.test("value_a_player_carrying_only_junk_is_taunted_empty_handed_and_left_owing", 600, ValueTests::onlyJunkOnThePlayer);
        tests.test("value_junk_on_the_ground_is_left_and_it_goes_for_its_player", 200, ValueTests::leavesJunkOnTheGround);
        tests.test("value_an_item_a_pack_values_on_the_ground_is_picked_up", 100, ValueTests::picksUpWhatAPackValues);
        tests.test("value_an_item_its_enchantments_make_worth_it_on_the_ground_is_picked_up", 100, ValueTests::picksUpEnchanted);
    }

    private static void robsWhatIsWorthTaking(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Containers.stocked(helper, NEARER, Blocks.CHEST, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIAMOND));
        Containers.open(helper, player, NEARER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (!bailiff.isFleeing()) {
                throw helper.assertionException("the Bailiff has not fled");
            }
            assertHolds(helper, NEARER, List.of(Items.COBBLESTONE), "the Bailiff did not take the diamond and leave the cobblestone");
            Bailiffs.assertCarries(helper, bailiff, new ItemStack(Items.DIAMOND));
        });
    }

    private static void passesOverJunk(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Containers.stocked(helper, NEARER, Blocks.CHEST, new ItemStack(Items.DIRT, 64), new ItemStack(Items.GRAVEL, 32));
        Containers.stocked(helper, FARTHER, Blocks.CHEST, new ItemStack(Items.GOLD_INGOT, 9));
        Containers.open(helper, player, NEARER);
        Containers.open(helper, player, FARTHER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (!bailiff.isFleeing()) {
                throw helper.assertionException("the Bailiff has not fled");
            }
            assertHolds(helper, NEARER, List.of(Items.DIRT, Items.GRAVEL), "the Bailiff took junk from the nearer chest");
            assertHolds(helper, FARTHER, List.of(), "the Bailiff did not go on to rob the farther chest");
            Bailiffs.assertCarries(helper, bailiff, new ItemStack(Items.GOLD_INGOT, 9));
        });
    }

    private static void mugsWhatIsWorthTaking(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.EMERALD, 3));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        var inventory = player.getInventory();
        if (inventory.findSlotMatchingItem(new ItemStack(Items.EMERALD, 3)) >= 0) {
            helper.fail("the Bailiff did not take the player's emeralds");
            return;
        }
        if (inventory.findSlotMatchingItem(new ItemStack(Items.COBBLESTONE, 64)) < 0) {
            helper.fail("the Bailiff took the player's cobblestone");
            return;
        }
        helper.succeed();
    }

    private static void onlyJunkOnThePlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.DIRT, 64), new ItemStack(Items.ROTTEN_FLESH, 5));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!bailiff.isTaunting()) {
                        throw helper.assertionException("the Bailiff has not taunted a player carrying only junk");
                    }
                })
                .thenExecute(() -> {
                    if (player.actionBar.stream().noneMatch(shown -> shown.getContents() instanceof TranslatableContents translatable
                            && translatable.getKey().equals("entity.atouchofwickedness.bailiff.taunt_empty_handed"))) {
                        throw helper.assertionException("the player was never told the Bailiff will return");
                    }
                    if (Bailiffs.stacksIn(player.getInventory()) != 2) {
                        throw helper.assertionException("the Bailiff took junk from its player");
                    }
                    // Out of its way and hit, it flees and vanishes the sooner.
                    var feet = player.position().add(0, 30, 0);
                    player.teleportTo(helper.getLevel(), feet.x, feet.y, feet.z, Set.of(), 0, 0, true);
                    player.attack(bailiff);
                })
                .thenWaitUntil(() -> {
                    if (!bailiff.isRemoved()) {
                        throw helper.assertionException("the Bailiff has not vanished");
                    }
                })
                .thenExecute(() -> {
                    if (Wickedness.of(player) < Wickedness.threshold(player)) {
                        throw helper.assertionException("the empty-handed Bailiff left the player at " + Wickedness.of(player)
                                + " Wickedness, under their Threshold of " + Wickedness.threshold(player));
                    }
                })
                .thenSucceed();
    }

    private static void leavesJunkOnTheGround(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        Bailiffs.comeFor(helper, BAILIFF, player);
        var junk = helper.spawnItem(Items.COBBLESTONE, GROUND.getBottomCenter());
        junk.setItem(new ItemStack(Items.COBBLESTONE, 64));
        helper.failIfEver(() -> {
            if (junk.isRemoved()) {
                throw helper.assertionException("the Bailiff picked up cobblestone from the ground");
            }
        });
        helper.succeedWhen(() -> {
            if (Bailiffs.stacksIn(player.getInventory()) != 0) {
                throw helper.assertionException("leaving the junk, the Bailiff has not taken the player's emeralds");
            }
        });
    }

    private static void picksUpWhatAPackValues(GameTestHelper helper) {
        // Melon seeds are worth nothing by default, which the game-test pack overrides.
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var seeds = helper.spawnItem(Items.MELON_SEEDS, GROUND.getBottomCenter());
        helper.succeedWhen(() -> {
            assertPickedUp(helper, seeds);
            Bailiffs.assertCarries(helper, bailiff, new ItemStack(Items.MELON_SEEDS));
        });
    }

    private static void picksUpEnchanted(GameTestHelper helper) {
        // Unvalued, a book is worth the unlisted Value, under the floor, until its stored enchantments add to it.
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);
        var item = helper.spawnItem(Items.ENCHANTED_BOOK, GROUND.getBottomCenter());
        item.setItem(book.copy());
        helper.succeedWhen(() -> {
            assertPickedUp(helper, item);
            Bailiffs.assertCarries(helper, bailiff, book);
        });
    }

    /** Asserts the container at {@code relative} holds a stack of each of {@code items}, in order, and nothing else. */
    private static void assertHolds(GameTestHelper helper, BlockPos relative, List<Item> items, String message) {
        var container = Containers.at(helper, relative);
        var held = new ArrayList<Item>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                held.add(container.getItem(slot).getItem());
            }
        }
        if (!held.equals(items)) {
            throw helper.assertionException(message + ": it holds " + held + ", not " + items);
        }
    }

    private static void assertPickedUp(GameTestHelper helper, ItemEntity item) {
        if (!item.isRemoved()) {
            throw helper.assertionException(item.getItem() + " still lies on the ground");
        }
    }
}

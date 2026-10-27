// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Bailiff weighs what it takes by Value: from a container or its player it never takes a stack
 * worth nothing beside one worth taking, a container holding only junk it passes over, and a player
 * carrying only junk it taunts empty-handed. On the ground it leaves junk and what a pack hasn't
 * valued, but goes for what a pack has, and for what its enchantments make worth it.
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
        tests.test("value_a_player_carrying_only_junk_is_taunted_empty_handed", 200, ValueTests::onlyJunkOnThePlayer);
        tests.test("value_junk_and_unvalued_items_on_the_ground_are_left", 60, ValueTests::leavesJunkOnTheGround);
        tests.test("value_an_item_a_pack_values_on_the_ground_is_picked_up", 100, ValueTests::picksUpWhatAPackValues);
        tests.test("value_an_item_its_enchantments_make_worth_it_on_the_ground_is_picked_up", 100, ValueTests::picksUpEnchanted);
    }

    private static void robsWhatIsWorthTaking(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        stocked(helper, NEARER, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIAMOND));
        open(helper, player, NEARER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (!bailiff.isFleeing()) {
                throw helper.assertionException("the Bailiff has not fled");
            }
            assertHolds(helper, NEARER, List.of(Items.COBBLESTONE), "the Bailiff did not take the diamond and leave the cobblestone");
            assertCarries(helper, bailiff, new ItemStack(Items.DIAMOND));
        });
    }

    private static void passesOverJunk(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        stocked(helper, NEARER, new ItemStack(Items.DIRT, 64), new ItemStack(Items.GRAVEL, 32));
        stocked(helper, FARTHER, new ItemStack(Items.GOLD_INGOT, 9));
        open(helper, player, NEARER);
        open(helper, player, FARTHER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (!bailiff.isFleeing()) {
                throw helper.assertionException("the Bailiff has not fled");
            }
            assertHolds(helper, NEARER, List.of(Items.DIRT, Items.GRAVEL), "the Bailiff took junk from the nearer chest");
            assertHolds(helper, FARTHER, List.of(), "the Bailiff did not go on to rob the farther chest");
            assertCarries(helper, bailiff, new ItemStack(Items.GOLD_INGOT, 9));
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
        helper.succeedWhen(() -> {
            if (!bailiff.isTaunting()) {
                throw helper.assertionException("the Bailiff has not taunted a player carrying only junk");
            }
            if (player.actionBar.stream().noneMatch(shown -> shown.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals("entity.atouchofwickedness.bailiff.taunt_empty_handed"))) {
                throw helper.assertionException("the player was never told the Bailiff will return");
            }
            if (Bailiffs.stacksIn(player.getInventory()) != 2) {
                throw helper.assertionException("the Bailiff took junk from its player");
            }
        });
    }

    private static void leavesJunkOnTheGround(GameTestHelper helper) {
        // With nothing to take from its player, an item on the ground worth going for would be its haul.
        var player = Players.join(helper, PLAYER);
        Bailiffs.comeFor(helper, BAILIFF, player);
        var junk = helper.spawnItem(Items.COBBLESTONE, GROUND.getBottomCenter());
        junk.setItem(new ItemStack(Items.COBBLESTONE, 64));
        // Unvalued, so worth the unlisted Value, under the floor.
        var stick = helper.spawnItem(Items.STICK, GROUND.getBottomCenter());
        // Unvalued too, with no stored enchantments to make it worth going for.
        var book = helper.spawnItem(Items.ENCHANTED_BOOK, GROUND.getBottomCenter());
        helper.startSequence()
                .thenExecuteFor(50, () -> {
                    for (var left : List.of(junk, stick, book)) {
                        if (left.isRemoved()) {
                            throw helper.assertionException("the Bailiff picked up " + left.getItem() + " from the ground");
                        }
                    }
                })
                .thenSucceed();
    }

    private static void picksUpWhatAPackValues(GameTestHelper helper) {
        // The game-test pack values flint, which is otherwise unvalued and under the floor.
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var flint = helper.spawnItem(Items.FLINT, GROUND.getBottomCenter());
        helper.succeedWhen(() -> {
            assertPickedUp(helper, flint);
            assertCarries(helper, bailiff, new ItemStack(Items.FLINT));
        });
    }

    private static void picksUpEnchanted(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);
        var item = helper.spawnItem(Items.ENCHANTED_BOOK, GROUND.getBottomCenter());
        item.setItem(book.copy());
        helper.succeedWhen(() -> {
            assertPickedUp(helper, item);
            assertCarries(helper, bailiff, book);
        });
    }

    /** Places a chest at {@code relative}, holding {@code stacks} in its first slots. */
    private static void stocked(GameTestHelper helper, BlockPos relative, ItemStack... stacks) {
        helper.setBlock(relative, Blocks.CHEST);
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

    /** Asserts the container at {@code relative} holds a stack of each of {@code items}, in order, and nothing else. */
    private static void assertHolds(GameTestHelper helper, BlockPos relative, List<Item> items, String message) {
        var container = container(helper, relative);
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

    private static void assertCarries(GameTestHelper helper, Bailiff bailiff, ItemStack expected) {
        var report = bailiff.report(helper.getLevel());
        var carried = report == null ? List.<ItemStack>of() : report.carried();
        if (carried.size() != 1 || !ItemStack.matches(carried.getFirst(), expected)) {
            throw helper.assertionException("the Bailiff carries " + carried + ", not " + expected);
        }
    }
}

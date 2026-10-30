// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.ItemValues;
import io.github._5thlayer.atouchofwickedness.bailiff.Value;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * What a stack shows the Bailiff is read from the data map, a pack's entries included, and from its
 * enchantments, a book's stored ones included; and what it reads of its player's inventory as it
 * arrives decides whether it taunts them at once, empty-handed. {@code ValueTest} pins how Value prices and draws.
 */
final class ValueTests {

    private static final BlockPos PLAYER = new BlockPos(6, 1, 2);
    /** Within the Bailiff's taunting reach of the player. */
    private static final BlockPos BESIDE = new BlockPos(6, 1, 4);

    private ValueTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("value_the_data_map_values_an_item", 20, ValueTests::dataMap);
        tests.test("value_enchantments_add_levels_stored_ones_included", 20, ValueTests::enchantments);
        tests.test("value_a_player_carrying_only_junk_is_taunted_at_once", 20, ValueTests::onlyJunkOnThePlayer);
    }

    private static void dataMap(GameTestHelper helper) {
        // Melon seeds are worth nothing by default, which the game-test pack overrides.
        var seeds = ItemValues.stack(new ItemStack(Items.MELON_SEEDS, 3));
        if (!seeds.equals(new Value.Stack(50, 0, 3))) {
            helper.fail("three melon seeds read as " + seeds + ", not the game-test pack's 50 each");
            return;
        }
        var stick = ItemValues.stack(new ItemStack(Items.STICK));
        if (stick.listed() != null) {
            helper.fail("a stick the data map doesn't list read as listed at " + stick.listed());
            return;
        }
        helper.succeed();
    }

    private static void enchantments(GameTestHelper helper) {
        var sharpness = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(sharpness, 2);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(sharpness, 1);
        if (ItemValues.stack(sword).enchantmentLevels() != 2) {
            helper.fail("a sword with Sharpness II read as " + ItemValues.stack(sword).enchantmentLevels() + " levels");
            return;
        }
        if (ItemValues.stack(book).enchantmentLevels() != 1) {
            helper.fail("a book storing Sharpness I read as " + ItemValues.stack(book).enchantmentLevels() + " levels");
            return;
        }
        helper.succeed();
    }

    private static void onlyJunkOnThePlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.DIRT, 64), new ItemStack(Items.ROTTEN_FLESH, 5));
        // Arriving within taunting reach of its player, it needs no walk to start shaking its head.
        var bailiff = Bailiffs.comeFor(helper, BESIDE, player);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    if (!bailiff.isTaunting()) {
                        throw helper.assertionException("the Bailiff came for a player carrying only junk and did not taunt them at once");
                    }
                    if (player.actionBar.stream().noneMatch(shown -> shown.getContents() instanceof TranslatableContents translatable
                            && translatable.getKey().equals("entity.atouchofwickedness.bailiff.taunt_empty_handed"))) {
                        throw helper.assertionException("the player was never told the Bailiff will return");
                    }
                })
                .thenSucceed();
    }
}

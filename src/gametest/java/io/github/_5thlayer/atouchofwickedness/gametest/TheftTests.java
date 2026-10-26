// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.Set;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A Bailiff with nothing of its player's to rob goes for them, and its first hit that lands takes
 * one stack from their main inventory or hotbar, but never the held item, their worn armour or
 * their off hand, and no more. With nothing it may take it taunts them empty-handed, and vanishing
 * with nothing leaves the debt standing.
 */
final class TheftTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(5, 1, 5);

    private TheftTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("theft_one_hit_takes_one_stack_never_held_armour_or_off_hand_and_no_more", 20, TheftTests::oneHit);
        tests.test("theft_a_first_hit_that_finds_only_the_held_item_takes_nothing_and_ends_the_mugging", 20, TheftTests::onlyHeld);
        tests.test("theft_the_bailiff_goes_for_its_player", 200, TheftTests::goesForItsPlayer);
        tests.test("theft_with_nothing_to_take_it_taunts_empty_handed_and_the_debt_stands", 600, TheftTests::nothingToTake);
    }

    private static void oneHit(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var inventory = player.getInventory();
        inventory.setItem(0, new ItemStack(Items.DIRT, 64));
        inventory.setSelectedSlot(0);
        inventory.setItem(9, new ItemStack(Items.BREAD, 5));
        inventory.setItem(30, new ItemStack(Items.IRON_INGOT, 12));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        // The second hit, as the Bailiff turns to taunt, takes nothing more.
        for (int hit = 1; hit <= 2; hit++) {
            Bailiffs.hit(helper, bailiff, player);
            int left = Bailiffs.stacksIn(inventory);
            if (left != 2) {
                helper.fail("after " + hit + " hits the player has " + left + " stacks left, not 2");
                return;
            }
            if (!inventory.getSelectedItem().is(Items.DIRT)) {
                helper.fail("the Bailiff took the held item");
                return;
            }
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                || !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                || !player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD)) {
            helper.fail("the Bailiff took the player's armour or off hand");
            return;
        }
        helper.succeed();
    }

    private static void onlyHeld(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var inventory = player.getInventory();
        inventory.setItem(4, new ItemStack(Items.DIAMOND_SWORD));
        inventory.setSelectedSlot(4);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        // It comes to mug a player with bread to take, who eats it before the Bailiff's hit lands.
        Players.stock(player, new ItemStack(Items.BREAD));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        inventory.removeItem(inventory.findSlotMatchingItem(new ItemStack(Items.BREAD)), 1);
        Bailiffs.hit(helper, bailiff, player);
        if (bailiff.getTarget() != null) {
            helper.fail("finding nothing to take, the Bailiff went on mugging its player");
            return;
        }
        if (!inventory.getItem(4).is(Items.DIAMOND_SWORD)) {
            helper.fail("the Bailiff took the player's only stack, the held item");
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                || !player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.SHIELD)) {
            helper.fail("with only the held item to take, the Bailiff took the player's armour or off hand");
            return;
        }
        helper.succeed();
    }

    private static void goesForItsPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (Bailiffs.stacksIn(player.getInventory()) != 0) {
                throw helper.assertionException("the Bailiff has not taken the player's emeralds");
            }
        });
    }

    private static void nothingToTake(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
        player.getInventory().setSelectedSlot(0);
        // A real visit, so the cooldown after it holds the next one off.
        Wickedness.setThreshold(player, 3);
        Wickedness.set(player, 3);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (Bailiffs.of(player).isEmpty()) {
                        throw helper.assertionException("no Bailiff arrived for a player at their Threshold");
                    }
                })
                .thenExecute(() -> {
                    // Onto this test's platform, out of the other tests' structures, which clear their entities.
                    var onPlatform = helper.absoluteVec(BAILIFF.getBottomCenter());
                    Bailiffs.of(player).getFirst().teleportTo(onPlatform.x, onPlatform.y, onPlatform.z);
                })
                .thenWaitUntil(() -> {
                    if (Bailiffs.of(player).isEmpty() || !Bailiffs.of(player).getFirst().isTaunting()) {
                        throw helper.assertionException("the Bailiff has not taunted a player with nothing to take");
                    }
                })
                .thenExecute(() -> {
                    if (player.actionBar.stream().noneMatch(shown -> shown.getContents() instanceof TranslatableContents translatable
                            && translatable.getKey().equals("entity.atouchofwickedness.bailiff.taunt_empty_handed"))) {
                        throw helper.assertionException("the player was never told the Bailiff will return");
                    }
                    if (!player.getInventory().getItem(0).is(Items.IRON_SWORD)) {
                        throw helper.assertionException("the Bailiff took the held item");
                    }
                    // Out of its way and hit, it flees and vanishes the sooner.
                    var feet = player.position().add(0, 30, 0);
                    player.teleportTo(helper.getLevel(), feet.x, feet.y, feet.z, Set.of(), 0, 0, true);
                    player.attack(Bailiffs.of(player).getFirst());
                })
                .thenWaitUntil(() -> {
                    if (!Bailiffs.of(player).isEmpty()) {
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
}

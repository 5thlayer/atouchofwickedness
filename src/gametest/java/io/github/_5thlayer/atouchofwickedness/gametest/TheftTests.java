// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A Bailiff with nothing of its player's to rob goes for them, and its first hit that lands takes
 * one stack from their main inventory or hotbar, but never the held item, their worn armour or
 * their off hand, and no more.
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
}

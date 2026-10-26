// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A Bailiff goes for its player, and each of its hits that lands takes one stack from their main
 * inventory or hotbar, but never the held item, their worn armour or their off hand.
 */
final class TheftTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(5, 1, 5);

    private TheftTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("theft_each_hit_takes_a_stack_never_held_armour_or_off_hand", 20, TheftTests::eachHit);
        tests.test("theft_a_hit_on_a_player_holding_their_only_stack_takes_nothing", 20, TheftTests::onlyHeld);
        tests.test("theft_the_bailiff_goes_for_its_player", 200, TheftTests::goesForItsPlayer);
    }

    private static void eachHit(GameTestHelper helper) {
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
        for (int hit = 1; hit <= 2; hit++) {
            Bailiffs.hit(helper, bailiff, player);
            int left = Bailiffs.stacksIn(inventory);
            if (left != 3 - hit) {
                helper.fail("after " + hit + " hits the player has " + left + " stacks left, not " + (3 - hit));
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
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        if (!inventory.getItem(4).is(Items.DIAMOND_SWORD)) {
            helper.fail("the Bailiff took the player's only stack, the held item");
            return;
        }
        helper.succeed();
    }

    private static void goesForItsPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        player.getInventory().setItem(1, new ItemStack(Items.EMERALD, 7));
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.succeedWhen(() -> {
            if (!player.getInventory().getItem(1).isEmpty()) {
                throw helper.assertionException("the Bailiff has not taken the player's emeralds");
            }
        });
    }
}

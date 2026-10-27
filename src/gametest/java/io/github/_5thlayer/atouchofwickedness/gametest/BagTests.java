// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * A killed Bailiff drops a Bailiff's Bag at the {@code bagChance} in the common config, which the test
 * forces to 1; the chance itself and its Looting bonus are vanilla's condition, read in review. The Bag
 * fills to four times what a bundle holds, and its bar reads its fill against that, while a bundle still
 * fills to one.
 */
final class BagTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(4, 1, 4);
    /** The first of the main inventory's slots, in the player's inventory menu as in their inventory. */
    private static final int FIRST_SLOT = 9;

    private BagTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("bag_dropped_by_a_killed_bailiff", 20, BagTests::dropped);
        tests.test("bag_holds_four_bundles", 1, BagTests::holdsFour);
        tests.test("bag_leaves_a_bundle_holding_one", 1, BagTests::bundleHoldsOne);
    }

    private static void dropped(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        var around = new AABB(bailiff.blockPosition()).inflate(4);
        ModConfigSpec.DoubleValue chance = Configs.common("bailiff.bagChance");
        double before = chance.getAsDouble();
        // Certain only within this tick, so no other test's Bailiff drops one.
        chance.set(1.0);
        try {
            WickedKillTests.killInMelee(helper, player, bailiff);
        } finally {
            chance.set(before);
        }
        int bags = DropTests.count(helper, around, WickednessItems.BAILIFFS_BAG.get());
        if (bags != 1) {
            helper.fail("with a chance of 1 the killed Bailiff dropped " + bags + " Bags");
            return;
        }
        helper.succeed();
    }

    private static void holdsFour(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bag = new ItemStack(WickednessItems.BAILIFFS_BAG.get());
        fill(player, bag, 2);
        // Half full: vanilla's bar of 13 is 1 plus 12 times the fill.
        int width = bag.getItem().getBarWidth(bag);
        if (width != 7) {
            helper.fail("two stacks in a Bag draw a bar " + width + " wide, not half of it at 7");
            return;
        }
        int left = fill(player, bag, 5);
        if (held(bag) != 4 * 64 || left != 64) {
            helper.fail("five stacks of cobblestone put into a Bag leave " + held(bag) + " in it and " + left + " out");
            return;
        }
        helper.succeed();
    }

    private static void bundleHoldsOne(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bundle = new ItemStack(Items.BUNDLE);
        int left = fill(player, bundle, 2);
        if (held(bundle) != 64 || left != 64) {
            helper.fail("two stacks of cobblestone put into a bundle leave " + held(bundle) + " in it and " + left + " out");
            return;
        }
        helper.succeed();
    }

    /**
     * Fills {@code into}, carried, from {@code stacks} stacks of cobblestone, as a player clicking it on
     * each in turn, and answers how many are left in the last.
     */
    private static int fill(ServerPlayer player, ItemStack into, int stacks) {
        var inventory = player.getInventory();
        for (int i = 0; i < stacks; i++) {
            inventory.setItem(FIRST_SLOT + i, new ItemStack(Items.COBBLESTONE, 64));
            into.getItem().overrideStackedOnOther(into, player.inventoryMenu.getSlot(FIRST_SLOT + i), ClickAction.PRIMARY, player);
        }
        return inventory.getItem(FIRST_SLOT + stacks - 1).getCount();
    }

    private static int held(ItemStack bundle) {
        return bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopyStream().mapToInt(ItemStack::getCount).sum();
    }
}

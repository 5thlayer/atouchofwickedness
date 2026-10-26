// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** A killed Bailiff drops every stack it stole, and 1 to 3 Seals. */
final class DropTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(4, 1, 4);

    private DropTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("drops_stolen_stacks_and_seals_when_killed", 20, DropTests::killed);
    }

    private static void killed(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        player.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
        player.getInventory().setItem(1, new ItemStack(Items.REDSTONE, 33));
        var bailiff = Bailiffs.comeFor(helper, BAILIFF, player);
        Bailiffs.hit(helper, bailiff, player);
        Bailiffs.hit(helper, bailiff, player);
        var around = new AABB(bailiff.blockPosition()).inflate(4);
        WickedKillTests.killInMelee(helper, player, bailiff);
        if (count(helper, around, Items.GOLD_INGOT) != 9 || count(helper, around, Items.REDSTONE) != 33) {
            helper.fail("the killed Bailiff dropped " + count(helper, around, Items.GOLD_INGOT) + " gold and "
                    + count(helper, around, Items.REDSTONE) + " redstone, not the 9 and 33 it stole");
            return;
        }
        int seals = count(helper, around, WickednessItems.SEAL.get());
        if (seals < 1 || seals > 3) {
            helper.fail("the killed Bailiff dropped " + seals + " Seals");
            return;
        }
        helper.succeed();
    }

    private static int count(GameTestHelper helper, AABB around, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }
}

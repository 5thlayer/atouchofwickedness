// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.WickednessItems;
import io.github._5thlayer.atouchofwickedness.strongbox.Strongbox;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Four Seals around a chest craft a Strongbox, which a player opens and stores in as in a chest, and
 * which a Bailiff never robs, even one a datapack adds to the Seizable tag.
 */
final class StrongboxTests {

    private static final BlockPos PLAYER = new BlockPos(6, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);
    private static final BlockPos STRONGBOX = new BlockPos(2, 1, 6);

    private StrongboxTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("strongbox_crafts_from_four_seals_and_a_chest", 1, StrongboxTests::crafts);
        tests.test("strongbox_stores_what_its_player_puts_in", 1, StrongboxTests::stores);
        tests.test("strongbox_is_never_robbed", 300, StrongboxTests::neverRobbed);
    }

    private static void crafts(GameTestHelper helper) {
        var level = helper.getLevel();
        var seal = new ItemStack(WickednessItems.SEAL.get());
        var input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, seal, ItemStack.EMPTY,
                seal, new ItemStack(Items.CHEST), seal,
                ItemStack.EMPTY, seal, ItemStack.EMPTY));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) {
            helper.fail("no recipe takes four Seals around a chest");
            return;
        }
        var result = recipe.get().value().assemble(input);
        if (!result.is(WickednessItems.STRONGBOX.get()) || result.getCount() != 1) {
            helper.fail("four Seals and a chest craft " + result + ", not a Strongbox");
            return;
        }
        helper.succeed();
    }

    private static void stores(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        helper.setBlock(STRONGBOX, Strongbox.BLOCK.get());
        helper.useBlock(STRONGBOX, player);
        if (!(player.containerMenu instanceof ChestMenu menu) || menu.getContainer().getContainerSize() != 27) {
            helper.fail("opening a Strongbox showed " + player.containerMenu + ", not a chest's 27 slots");
            return;
        }
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5));
        player.closeContainer();
        var held = container(helper).getItem(0);
        if (!held.is(Items.DIAMOND) || held.getCount() != 5) {
            helper.fail("the Strongbox holds " + held + ", not the 5 diamonds its player put in");
            return;
        }
        helper.succeed();
    }

    private static void neverRobbed(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        helper.setBlock(STRONGBOX, Strongbox.BLOCK.get());
        container(helper).setItem(0, new ItemStack(Items.GOLD_INGOT, 9));
        // The game-test pack adds the Strongbox to the Seizable tag, which must not make it Seizable.
        helper.useBlock(STRONGBOX, player);
        player.closeContainer();
        Bailiffs.comeFor(helper, BAILIFF, player);
        helper.failIfEver(() -> {
            if (container(helper).getItem(0).isEmpty()) {
                throw helper.assertionException("the Bailiff robbed a Strongbox");
            }
        });
        // A container it robbed it robs before it goes for its player.
        helper.succeedWhen(() -> {
            if (Bailiffs.stacksIn(player.getInventory()) != 0) {
                throw helper.assertionException("the Bailiff has not taken the player's stack");
            }
        });
    }

    private static Container container(GameTestHelper helper) {
        return (Container) helper.getBlockEntity(STRONGBOX, BlockEntity.class);
    }
}

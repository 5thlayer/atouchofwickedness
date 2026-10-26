// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.WickednessItems;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A Seal and a stick craft a Tally Stick. Used, it shows its player's Standing in the action bar,
 * never the number, Clean below ½ of the Threshold, Marked from ½, Overdue from ¾ and Forfeit from
 * 90%, and wears 1 of its 16 uses. A Seal repairs it in an anvil.
 */
final class TallyStickTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);

    private TallyStickTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("tally_stick_crafts_from_a_seal_and_a_stick", 1, TallyStickTests::crafts);
        tests.test("tally_stick_shows_the_standing_and_wears_a_use", 1, TallyStickTests::shows);
        tests.test("tally_stick_repairs_with_a_seal_in_an_anvil", 1, TallyStickTests::repairs);
    }

    private static void crafts(GameTestHelper helper) {
        var level = helper.getLevel();
        var input = CraftingInput.of(2, 1, List.of(new ItemStack(WickednessItems.SEAL.get()), new ItemStack(Items.STICK)));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) {
            helper.fail("no recipe takes a Seal and a stick");
            return;
        }
        var result = recipe.get().value().assemble(input);
        if (!result.is(WickednessItems.TALLY_STICK.get()) || result.getCount() != 1) {
            helper.fail("a Seal and a stick craft " + result + ", not a Tally Stick");
            return;
        }
        helper.succeed();
    }

    private static void shows(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var stick = new ItemStack(WickednessItems.TALLY_STICK.get());
        if (stick.getMaxDamage() != 16) {
            helper.fail("a Tally Stick lasts " + stick.getMaxDamage() + " uses, not 16");
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, stick);
        Wickedness.setThreshold(player, 80);
        // 39 is below ½ of 80, 40 is ½, 60 is ¾ and 72 is 90%.
        int[] wickedness = {39, 40, 60, 72};
        String[] standing = {"clean", "marked", "overdue", "forfeit"};
        for (int i = 0; i < wickedness.length; i++) {
            Wickedness.set(player, wickedness[i]);
            player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            var shown = player.actionBar.getLast();
            if (!(shown.getContents() instanceof TranslatableContents translatable)
                    || !translatable.getKey().equals("atouchofwickedness.standing." + standing[i])) {
                helper.fail(wickedness[i] + " Wickedness of 80 showed " + shown.getString() + ", not " + standing[i]);
                return;
            }
            if (translatable.getArgs().length != 0) {
                helper.fail("the Tally Stick showed a number: " + shown.getString());
                return;
            }
        }
        int worn = player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue();
        if (worn != 4) {
            helper.fail("four uses wore the Tally Stick by " + worn + ", not 4");
            return;
        }
        helper.succeed();
    }

    private static void repairs(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        player.experienceLevel = 30;
        var menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(PLAYER)));
        var worn = new ItemStack(WickednessItems.TALLY_STICK.get());
        worn.setDamageValue(12);
        menu.getSlot(0).set(worn);
        menu.getSlot(1).set(new ItemStack(WickednessItems.SEAL.get()));
        menu.createResult();
        var repaired = menu.getSlot(2).getItem();
        if (!repaired.is(WickednessItems.TALLY_STICK.get()) || repaired.getDamageValue() >= 12) {
            helper.fail("a Seal in an anvil made " + repaired + " of a worn Tally Stick");
            return;
        }
        helper.succeed();
    }
}

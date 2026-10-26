// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;

/** The Bailiffs of the game tests: one sent straight to a spot, and those come for a player. */
final class Bailiffs {

    private Bailiffs() {
    }

    /** A Bailiff at {@code relative}, come for {@code player}. */
    static Bailiff comeFor(GameTestHelper helper, BlockPos relative, ServerPlayer player) {
        var bailiff = helper.spawn(BailiffContent.BAILIFF.get(), relative);
        bailiff.comeFor(player);
        return bailiff;
    }

    /** @return every Bailiff in the player's level come for them */
    static List<? extends Bailiff> of(ServerPlayer player) {
        return player.level().getEntities(BailiffContent.BAILIFF.get(),
                bailiff -> bailiff.player().filter(player.getUUID()::equals).isPresent());
    }

    /** The Bailiff lands a hit on its player, who is at once ready for the next one. */
    static void hit(GameTestHelper helper, Bailiff bailiff, ServerPlayer player) {
        if (!bailiff.doHurtTarget(helper.getLevel(), player)) {
            throw helper.assertionException("the Bailiff's hit did not land");
        }
        Players.recover(player);
    }

    /** @return how many stacks the player's main inventory and hotbar hold */
    static int stacksIn(Inventory inventory) {
        return (int) inventory.getNonEquipmentItems().stream().filter(stack -> !stack.isEmpty()).count();
    }
}

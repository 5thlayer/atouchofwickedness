// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The containers of the game tests: placed stocked in a test's structure, and opened by its player. */
final class Containers {

    private Containers() {
    }

    /** Places {@code block} at {@code relative}, holding {@code stacks} in its first slots. */
    static void stocked(GameTestHelper helper, BlockPos relative, Block block, ItemStack... stacks) {
        helper.setBlock(relative, block);
        var container = at(helper, relative);
        for (int slot = 0; slot < stacks.length; slot++) {
            container.setItem(slot, stacks[slot]);
        }
    }

    /** The player opens the block at {@code relative}, as with a right click, and closes it again. */
    static void open(GameTestHelper helper, ServerPlayer player, BlockPos relative) {
        helper.useBlock(relative, player);
        player.closeContainer();
    }

    /** @return the container at {@code relative} */
    static Container at(GameTestHelper helper, BlockPos relative) {
        return (Container) helper.getBlockEntity(relative, BlockEntity.class);
    }

    /** @return how many stacks the container at {@code relative} holds */
    static int stacksIn(GameTestHelper helper, BlockPos relative) {
        var container = at(helper, relative);
        int stacks = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                stacks++;
            }
        }
        return stacks;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.strongbox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** What a Strongbox holds: a chest's 27 slots, its lid and its sounds, under the Strongbox's name. */
public final class StrongboxBlockEntity extends ChestBlockEntity {

    public StrongboxBlockEntity(BlockPos pos, BlockState state) {
        super(Strongbox.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.atouchofwickedness.strongbox");
    }
}

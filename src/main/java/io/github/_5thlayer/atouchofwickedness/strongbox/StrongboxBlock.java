// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.strongbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Strongbox's block: a chest in all but its look and that it never joins another into a double
 * one, so it always holds 27 stacks and has one texture to draw.
 */
public final class StrongboxBlock extends ChestBlock {

    public static final MapCodec<StrongboxBlock> CODEC = simpleCodec(StrongboxBlock::new);

    public StrongboxBlock(BlockBehaviour.Properties properties) {
        super(Strongbox.BLOCK_ENTITY::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
    }

    @Override
    public MapCodec<StrongboxBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StrongboxBlockEntity(pos, state);
    }

    @Override
    public boolean chestCanConnectTo(BlockState state) {
        return false;
    }
}

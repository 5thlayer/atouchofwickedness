// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.strongbox;

import java.util.function.Supplier;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Strongbox, the counterplay to the Bailiff's robbery (ADR 0003): a chest of the Pillar's own,
 * crafted from four Seals and a chest, that the Bailiff never seizes. Its item is {@code
 * WickednessItems.STRONGBOX}.
 */
public final class Strongbox {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ATouchOfWickedness.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ATouchOfWickedness.MOD_ID);

    /** Mined, sounding and burning as a chest does. */
    public static final DeferredBlock<StrongboxBlock> BLOCK = BLOCKS.registerBlock("strongbox", StrongboxBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST));

    public static final Supplier<BlockEntityType<StrongboxBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("strongbox",
            () -> new BlockEntityType<>(StrongboxBlockEntity::new, BLOCK.get()));

    private Strongbox() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}

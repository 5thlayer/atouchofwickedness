// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.List;
import java.util.function.Supplier;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.mixin.CompoundContainerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/**
 * The containers a Bailiff may rob: those whose block entity is in {@link #TAG}, and only the ones
 * its player has opened (ADR 0003). Each player's most recent ones, by dimension and position, are
 * remembered on them as they open them, saved with the player and copied through death. An ender
 * chest is never remembered, since what it opens is the player's own and no block's, and a shulker
 * box never is, even one a datapack adds to the tag, since the player carries it off with what it holds.
 */
public final class Seizable {

    /** Chests, trapped chests and barrels by default; a datapack may add to it. */
    public static final TagKey<BlockEntityType<?>> TAG =
            TagKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "seizable"));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfWickedness.MOD_ID);

    private static final Supplier<AttachmentType<Opened<GlobalPos>>> OPENED = ATTACHMENTS.register("opened_containers",
            () -> AttachmentType.builder(Opened::<GlobalPos>none)
                    .serialize(GlobalPos.CODEC.listOf().xmap(Opened::new, Opened::recent).fieldOf("recent"))
                    .copyOnDeath()
                    .build());

    private Seizable() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Seizable::onOpen);
    }

    /** @return the positions of the Seizable containers {@code player} opened in {@code level}, most recent first */
    public static List<BlockPos> opened(ServerPlayer player, ServerLevel level) {
        return player.getData(OPENED).recent().stream()
                .filter(pos -> pos.dimension() == level.dimension())
                .map(GlobalPos::pos)
                .toList();
    }

    /** @return whether {@code blockEntity} is a container the Bailiff may rob */
    public static boolean is(BlockEntity blockEntity) {
        return blockEntity instanceof Container && !(blockEntity instanceof ShulkerBoxBlockEntity)
                && blockEntity.getType().builtInRegistryHolder().is(TAG);
    }

    /**
     * @return the Seizable container at {@code pos}, both halves of a double chest together, or null
     *         when there is none there now, as when it was broken or its chunk is not loaded
     */
    static @Nullable Container at(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof BlockEntity blockEntity) || !is(blockEntity)) {
            return null;
        }
        // As a hopper sees a chest: both halves of a double one.
        if (blockEntity instanceof ChestBlockEntity && blockEntity.getBlockState().getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, blockEntity.getBlockState(), level, pos, true);
        }
        return (Container) blockEntity;
    }

    /** Remembers each Seizable container the menu a player opens shows, a double chest by one half. */
    private static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        event.getContainer().slots.stream()
                .map(slot -> slot.container)
                .distinct()
                .map(Seizable::blockOf)
                .filter(blockEntity -> blockEntity != null && is(blockEntity))
                .forEach(blockEntity -> player.setData(OPENED, player.getData(OPENED).plus(
                        GlobalPos.of(player.level().dimension(), blockEntity.getBlockPos()), WickednessConfig.containersRemembered())));
    }

    private static @Nullable BlockEntity blockOf(Container container) {
        if (container instanceof CompoundContainer compound) {
            return blockOf(((CompoundContainerAccessor) compound).atouchofwickedness$container1());
        }
        return container instanceof BlockEntity blockEntity ? blockEntity : null;
    }
}

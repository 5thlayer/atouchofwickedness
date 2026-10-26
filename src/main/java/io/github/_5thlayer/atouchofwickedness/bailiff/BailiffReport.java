// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What a Bailiff shows a viewer such as Jade, read on the server: the player it came for and the
 * stacks it has taken from them. Never that player's Wickedness, Threshold or Standing, which no
 * tooltip shows (ADR 0002).
 */
public record BailiffReport(Component player, List<ItemStack> carried) {

    public static final StreamCodec<RegistryFriendlyByteBuf, BailiffReport> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, BailiffReport::player,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, BailiffReport::carried,
            BailiffReport::new);
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Each item's Value to the Bailiff, its entry in the data map {@code atouchofwickedness:bailiff_value},
 * so a pack can value any item, a modded one included, read with what else of a stack {@link Value}
 * prices: its count and its enchantments, a book's stored ones included. An item
 * that holds items, such as a shulker box or a bundle, is worth its own Value, whatever it holds.
 */
public final class ItemValues {

    public static final Identifier VALUE_ID = Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "bailiff_value");

    /**
     * What each item is worth to the Bailiff. Unlike a creature's weight, an unlisted item is worth a
     * little rather than nothing, so a pack that values none of its items still has them stolen.
     */
    public static final DataMapType<Item, Integer> VALUE =
            DataMapType.builder(VALUE_ID, Registries.ITEM, ExtraCodecs.NON_NEGATIVE_INT).build();

    private ItemValues() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(VALUE));
    }

    /** @return what {@code stack} shows the Bailiff: its item's entry, its enchantments' levels, stored ones included, and its count */
    public static Value.Stack stack(ItemStack stack) {
        int levels = levels(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                + levels(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return new Value.Stack(stack.getItem().builtInRegistryHolder().getData(VALUE), levels, stack.getCount());
    }

    private static int levels(ItemEnchantments enchantments) {
        int levels = 0;
        for (var entry : enchantments.entrySet()) {
            levels += entry.getIntValue();
        }
        return levels;
    }
}

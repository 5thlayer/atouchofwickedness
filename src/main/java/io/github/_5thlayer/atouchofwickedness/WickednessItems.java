// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness;

import io.github._5thlayer.atouchofwickedness.strongbox.Strongbox;
import io.github._5thlayer.atouchofwickedness.wickedness.TallyStick;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's items: the Seal a killed Bailiff leaves, the Tally Stick crafted from one, and the
 * Strongbox crafted from four.
 */
public final class WickednessItems {

    /** What repairs a Tally Stick in an anvil: a Seal. */
    public static final TagKey<Item> REPAIRS_TALLY_STICK =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "repairs_tally_stick"));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfWickedness.MOD_ID);

    public static final DeferredItem<Item> SEAL = ITEMS.registerSimpleItem("seal");

    public static final DeferredItem<TallyStick> TALLY_STICK = ITEMS.registerItem("tally_stick", TallyStick::new,
            properties -> properties.durability(WickednessConfig.tallyStickUses()).repairable(REPAIRS_TALLY_STICK));

    public static final DeferredItem<BlockItem> STRONGBOX = ITEMS.registerSimpleBlockItem("strongbox", Strongbox.BLOCK);

    private WickednessItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        // The items are built before the common config loads, so its uses are set again once it has.
        modBus.addListener((ModifyDefaultComponentsEvent event) -> event.modify(TALLY_STICK,
                (components, context, item) -> components.set(DataComponents.MAX_DAMAGE, WickednessConfig.tallyStickUses())));
    }
}

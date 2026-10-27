// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bag;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.math.Fraction;

/**
 * The Bailiff's Bag, the rare trophy a killed Bailiff sometimes drops ({@link BagChance}): a bundle on
 * vanilla's bundle contents that holds {@link #CAPACITY} times what a bundle holds. Vanilla's bundle
 * fills to a weight of 1, so the mixins on its code give each fill and each reading of it the
 * holding bundle's {@link #capacity}, the bar and the tooltip's fill included.
 */
public final class BailiffsBag extends BundleItem {

    /** How many bundles' worth the Bag holds. */
    public static final Fraction CAPACITY = Fraction.getFraction(4, 1);

    public BailiffsBag(Item.Properties properties) {
        super(properties);
    }

    /** What {@code item} fills to: a Bag's {@link #CAPACITY}, any other bundle's 1. */
    public static Fraction capacity(Item item) {
        return item instanceof BailiffsBag ? CAPACITY : Fraction.ONE;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack bag) {
        TooltipDisplay display = bag.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        return !display.shows(DataComponents.BUNDLE_CONTENTS)
                ? Optional.empty()
                : Optional.ofNullable(bag.get(DataComponents.BUNDLE_CONTENTS)).map(Tooltip::new);
    }

    /** The Bag's tooltip, which the client draws as a bundle's with its fill read against the Bag's capacity. */
    public record Tooltip(BundleContents contents) implements TooltipComponent {
    }

    /**
     * What a mixin gives the vanilla objects that read a bundle's fill, which know nothing of the item
     * they read it for: the capacity to fill to, or to read the fill against.
     */
    public interface Capacity {

        void atouchofwickedness$setCapacity(Fraction capacity);
    }
}

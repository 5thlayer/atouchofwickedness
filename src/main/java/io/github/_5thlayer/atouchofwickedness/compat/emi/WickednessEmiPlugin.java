// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.compat.emi;

import java.util.List;
import java.util.stream.Stream;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.WickednessItems;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import io.github._5thlayer.atouchofwickedness.discovery.Discoveries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The Pillar in EMI: its recipes are vanilla's crafting and anvil, which EMI shows unaided, so the
 * plugin adds only information pages, on Wickedness, the Standings, Atonement and the Bailiff, which
 * hang on the Pillar's items since none is an item of its own, and on each of the items. Each reads as
 * undiscovered until the player has discovered it (suite ADR-0006), and none gives a number ADR 0002
 * hides: no Threshold, band, weight, Decay rate or Atonement's size.
 *
 * <p>Found by EMI's entrypoint scan and named nowhere else, so EMI stays a soft dependency.
 */
@EmiEntrypoint
public final class WickednessEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        info(registry, "wickedness", Discoveries.WICKED_KILL, stacks(WickednessItems.TALLY_STICK));
        info(registry, "standing", Discoveries.MARKED, stacks(WickednessItems.TALLY_STICK));
        info(registry, "atonement", Discoveries.ATONEMENT, stacks(WickednessItems.TALLY_STICK, Items.WHEAT, Items.GOLDEN_APPLE));
        // The cooldown from this side's config, as the Tally Stick's uses are.
        info(registry, "bailiff", Discoveries.BAILIFF, stacks(WickednessItems.SEAL, WickednessItems.STRONGBOX),
                Math.ceilDiv(WickednessConfig.ticksBetweenVisits(), 20 * 60));
        info(registry, "seal", Discoveries.SEAL, stacks(WickednessItems.SEAL));
        info(registry, "tally_stick", Discoveries.TALLY_STICK, stacks(WickednessItems.TALLY_STICK),
                WickednessItems.TALLY_STICK.toStack().getMaxDamage());
        info(registry, "strongbox", Discoveries.STRONGBOX, stacks(WickednessItems.STRONGBOX));
        info(registry, "bailiffs_bag", Discoveries.BAILIFFS_BAG, stacks(WickednessItems.BAILIFFS_BAG), BailiffsBag.CAPACITY.intValue());
    }

    /**
     * A page on these stacks, its text the translation {@code emi.atouchofwickedness.info.<name>}, which
     * reads as undiscovered until the player has discovered {@code discovery}.
     */
    private static void info(EmiRegistry registry, String name, String discovery, Stream<? extends EmiIngredient> stacks, Object... args) {
        List<EmiIngredient> ingredients = stacks.map(EmiIngredient.class::cast).toList();
        // EMI's own ids start with a slash for a recipe no data pack holds.
        var id = Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "/info/" + name);
        registry.addRecipe(new InfoPage(ingredients, Component.translatable("emi.atouchofwickedness.info." + name, args),
                () -> Discoveries.record().isKnownOnClient(discovery), id));
    }

    private static Stream<EmiStack> stacks(ItemLike... items) {
        return Stream.of(items).map(EmiStack::of);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scrolling over a bundle in an inventory picks the item it gives out next, for the items in the
 * {@code minecraft:bundles} tag. The Bailiff's Bag is kept out of that tag, which would let dye turn it
 * into a plain bundle, so it's let in here.
 */
@Mixin(BundleMouseActions.class)
public abstract class BundleMouseActionsMixin {

    @ModifyReturnValue(method = "matches", at = @At("RETURN"))
    private boolean atouchofwickedness$bag(boolean matches, Slot slot) {
        return matches || slot.getItem().getItem() instanceof BailiffsBag;
    }
}

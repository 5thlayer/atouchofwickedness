// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.DataResult;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bundle's tooltip reads its fill, its progress bar and its "Full", against a weight of 1, which the
 * Bailiff's Bag's tooltip raises to the Bag's capacity.
 */
@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin implements BailiffsBag.Capacity {

    @Unique
    private Fraction atouchofwickedness$capacity = Fraction.ONE;

    @Override
    public void atouchofwickedness$setCapacity(Fraction capacity) {
        atouchofwickedness$capacity = capacity;
    }

    @ModifyExpressionValue(method = "extractImage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BundleContents;weight()Lcom/mojang/serialization/DataResult;"))
    private DataResult<Fraction> atouchofwickedness$readAgainstCapacity(DataResult<Fraction> weight) {
        return weight.map(fill -> fill.divideBy(atouchofwickedness$capacity));
    }
}

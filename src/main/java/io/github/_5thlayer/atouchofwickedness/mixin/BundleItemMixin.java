// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bundle filled by a click fills to its item's capacity, which the Bailiff's Bag raises, and its bar
 * reads its fill against that capacity, so a Bag's is full only once the Bag is.
 */
@Mixin(BundleItem.class)
public abstract class BundleItemMixin {

    @ModifyExpressionValue(method = {"overrideStackedOnOther", "overrideOtherStackedOnMe"}, at = @At(value = "NEW",
            target = "(Lnet/minecraft/world/item/component/BundleContents;)Lnet/minecraft/world/item/component/BundleContents$Mutable;"))
    private BundleContents.Mutable atouchofwickedness$fillToCapacity(BundleContents.Mutable contents) {
        ((BailiffsBag.Capacity) contents).atouchofwickedness$setCapacity(BailiffsBag.capacity((BundleItem) (Object) this));
        return contents;
    }

    @ModifyExpressionValue(method = {"getBarWidth", "getBarColor"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BundleItem;getWeightSafe(Lnet/minecraft/world/item/component/BundleContents;)Lorg/apache/commons/lang3/math/Fraction;"))
    private Fraction atouchofwickedness$readAgainstCapacity(Fraction weight) {
        return weight.divideBy(BailiffsBag.capacity((BundleItem) (Object) this));
    }
}

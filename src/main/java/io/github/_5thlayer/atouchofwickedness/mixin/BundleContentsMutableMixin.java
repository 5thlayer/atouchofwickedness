// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bundle's contents, as they are filled, fill to a weight of 1, which the Bailiff's Bag raises to
 * its own capacity once {@code BundleItemMixin} hands it over. Filling to 1, vanilla merges what it
 * adds into the stack of the same item it holds, which never outgrows a stack; a Bag holds more than a
 * stack of an item, so what is added tops that stack up and the rest starts a stack of its own.
 */
@Mixin(BundleContents.Mutable.class)
public abstract class BundleContentsMutableMixin implements BailiffsBag.Capacity {

    @Shadow
    @Final
    private List<ItemStack> items;

    @Shadow
    protected abstract int findStackIndex(ItemStack added);

    @Unique
    private Fraction atouchofwickedness$capacity = Fraction.ONE;

    @Override
    public void atouchofwickedness$setCapacity(Fraction capacity) {
        atouchofwickedness$capacity = capacity;
    }

    @ModifyExpressionValue(method = "getMaxAmountToAdd",
            at = @At(value = "FIELD", target = "Lorg/apache/commons/lang3/math/Fraction;ONE:Lorg/apache/commons/lang3/math/Fraction;"))
    private Fraction atouchofwickedness$capacity(Fraction full) {
        return atouchofwickedness$capacity;
    }

    @WrapOperation(method = "tryInsert", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BundleContents$Mutable;findStackIndex(Lnet/minecraft/world/item/ItemStack;)I"))
    private int atouchofwickedness$stackWithRoom(BundleContents.Mutable contents, ItemStack added, Operation<Integer> original,
            @Local(ordinal = 0) int amountToAdd) {
        int index = original.call(contents, added);
        if (index == -1) {
            return -1;
        }
        var held = items.get(index);
        return held.getCount() + amountToAdd <= held.getMaxStackSize() ? index : -1;
    }

    /** Inserts as much of {@code added} as tops up the stack it would merge into, and then the rest. */
    @WrapMethod(method = "tryInsert")
    private int atouchofwickedness$topUp(ItemStack added, Operation<Integer> original) {
        int index = findStackIndex(added);
        if (index == -1) {
            return original.call(added);
        }
        var held = items.get(index);
        int room = held.getMaxStackSize() - held.getCount();
        if (room <= 0 || room >= added.getCount()) {
            return original.call(added);
        }
        var topUp = added.split(room);
        int inserted = original.call(topUp);
        boolean full = !topUp.isEmpty();
        added.grow(topUp.getCount());
        return full ? inserted : inserted + original.call(added);
    }
}

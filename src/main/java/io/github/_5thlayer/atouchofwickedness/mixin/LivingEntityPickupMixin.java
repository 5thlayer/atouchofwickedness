// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import io.github._5thlayer.atouchofwickedness.wickedness.KindActs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mob picking up an item ends in {@code onItemPickup}, where vanilla credits the item's thrower with
 * its advancement, before the item leaves the ground; NeoForge posts no event for it. A villager's is
 * a gift when the item is food.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPickupMixin {

    @Inject(method = "onItemPickup", at = @At("HEAD"))
    private void atouchofwickedness$gift(ItemEntity item, CallbackInfo callback) {
        KindActs.pickedUp((LivingEntity) (Object) this, item);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import java.util.UUID;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The player who started curing a zombie villager, whom vanilla keeps to itself. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccessor {

    @Accessor("conversionStarter")
    @Nullable UUID atouchofwickedness$conversionStarter();
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The first of a double chest's halves, which vanilla keeps to itself. */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {

    @Accessor("container1")
    Container atouchofwickedness$container1();
}

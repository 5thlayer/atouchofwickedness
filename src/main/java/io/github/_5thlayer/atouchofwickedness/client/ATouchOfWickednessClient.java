// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.client;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import io.github._5thlayer.atouchofwickedness.strongbox.Strongbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Pillar's client entry point: it draws the Bailiff ({@link BailiffRenderer}) and the Strongbox
 * ({@link StrongboxRenderer}).
 */
@Mod(value = ATouchOfWickedness.MOD_ID, dist = Dist.CLIENT)
public final class ATouchOfWickednessClient {

    public ATouchOfWickednessClient(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(BailiffContent.BAILIFF.get(), BailiffRenderer::new);
            event.registerBlockEntityRenderer(Strongbox.BLOCK_ENTITY.get(), StrongboxRenderer::new);
        });
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.client;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.bag.BailiffsBag;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import io.github._5thlayer.atouchofwickedness.strongbox.Strongbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * The Pillar's client entry point: it draws the Bailiff ({@link BailiffRenderer}), the Zs it sleeps
 * under ({@link SnoreParticle}) and the Strongbox ({@link StrongboxRenderer}), and the Bailiff's Bag's
 * tooltip as a bundle's, its fill read against the Bag's capacity.
 */
@Mod(value = ATouchOfWickedness.MOD_ID, dist = Dist.CLIENT)
public final class ATouchOfWickednessClient {

    public ATouchOfWickednessClient(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(BailiffContent.BAILIFF.get(), BailiffRenderer::new);
            event.registerBlockEntityRenderer(Strongbox.BLOCK_ENTITY.get(), StrongboxRenderer::new);
        });
        modBus.addListener((RegisterParticleProvidersEvent event) ->
                event.registerSpriteSet(BailiffContent.SNORE.get(), SnoreParticle.Provider::new));
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) ->
                event.register(BailiffsBag.Tooltip.class, tooltip -> {
                    var bundle = new ClientBundleTooltip(tooltip.contents());
                    ((BailiffsBag.Capacity) bundle).atouchofwickedness$setCapacity(BailiffsBag.CAPACITY);
                    return bundle;
                }));
    }
}

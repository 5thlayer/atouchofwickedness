// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.client;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * Draws the Bailiff on vanilla's wandering-trader model, its skin retinted a deep crimson (ADR 0004):
 * the trader's blue robe goes a near-black violet and its tan skin a raw red. The tint is applied to vanilla's own skin as it is drawn, so the Pillar
 * ships none of Mojang's art; a bespoke model and skin can replace it without touching behaviour.
 */
public final class BailiffRenderer extends MobRenderer<Bailiff, VillagerRenderState, VillagerModel> {

    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/wandering_trader/wandering_trader.png");

    /** The tint over the trader's blue robe and tan skin, in ARGB: strong enough that it no longer reads as a trader. */
    private static final int TINT = 0xFFB0485A;

    public BailiffRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return SKIN;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    protected int getModelTint(VillagerRenderState state) {
        return TINT;
    }
}

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
 * Draws the Bailiff on vanilla's wandering-trader model, its skin retinted to a bailiff's ashen
 * grey-violet (ADR 0004). The tint is applied to vanilla's own skin as it is drawn, so the Pillar
 * ships none of Mojang's art; a bespoke model and skin can replace it without touching behaviour.
 */
public final class BailiffRenderer extends MobRenderer<Bailiff, VillagerRenderState, VillagerModel> {

    private static final Identifier SKIN = Identifier.withDefaultNamespace("textures/entity/wandering_trader/wandering_trader.png");

    /** The tint over the trader's blue robe and tan skin: ARGB, a cold, bruised grey. */
    private static final int TINT = 0xFF8E7F99;

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

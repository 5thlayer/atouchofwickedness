// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.client;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.strongbox.StrongboxBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/**
 * Draws the Strongbox on vanilla's chest model, lid and all, with the Pillar's own texture,
 * {@code textures/entity/chest/strongbox.png}, which the chest atlas gathers with vanilla's. A
 * Strongbox is never a double chest, so it has no left or right texture.
 */
public final class StrongboxRenderer extends ChestRenderer<StrongboxBlockEntity> {

    private static final SpriteId SPRITE = Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "strongbox"));

    public StrongboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SpriteId getCustomSprite(StrongboxBlockEntity strongbox, ChestRenderState state) {
        return SPRITE;
    }
}

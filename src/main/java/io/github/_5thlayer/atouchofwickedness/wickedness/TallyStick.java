// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Crafted from a Seal and a stick, it tells its user their Standing in the action bar, never the
 * number, and wears a notch each time it is read. A Seal repairs it in an anvil.
 */
public final class TallyStick extends Item {

    public TallyStick(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(Wickedness.standing(serverPlayer).translationKey()));
            player.getItemInHand(hand).hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}

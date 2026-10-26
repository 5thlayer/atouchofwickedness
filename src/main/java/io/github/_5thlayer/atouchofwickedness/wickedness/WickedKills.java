// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * A Wicked kill: a creature a player kills in melee adds its weight to the player's Wickedness. The
 * weight is the creature type's entry in the data map {@code atouchofwickedness:wickedness_weight},
 * and nothing when it has none, so a pack can weigh any creature, a modded one included.
 */
public final class WickedKills {

    public static final Identifier WEIGHT_ID = Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "wickedness_weight");

    /** What a Wicked kill of each creature type adds; unlisted types add nothing. */
    public static final DataMapType<EntityType<?>, Integer> WEIGHT =
            DataMapType.builder(WEIGHT_ID, Registries.ENTITY_TYPE, ExtraCodecs.NON_NEGATIVE_INT).build();

    private WickedKills() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(WEIGHT));
        NeoForge.EVENT_BUS.addListener(WickedKills::onDeath);
    }

    /** @return what a Wicked kill of a creature of {@code type} adds */
    public static int weightOf(EntityType<?> type) {
        return WickednessWeight.of(type.builtInRegistryHolder().getData(WEIGHT));
    }

    private static void onDeath(LivingDeathEvent event) {
        var source = event.getSource();
        // In melee: the player is the one who struck, not what they shot or sent.
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
            return;
        }
        int weight = weightOf(event.getEntity().getType());
        if (weight > 0) {
            Wickedness.add(player, weight);
        }
    }
}

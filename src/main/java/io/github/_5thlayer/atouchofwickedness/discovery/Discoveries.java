// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.discovery;

import java.util.List;
import java.util.function.Supplier;

import io.github._5thlayer.atouchof.discovery.Discovery;
import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.wickedness.Standing;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's Discoveries (suite ADR-0006), each the advancement {@code atouchofwickedness:discovery/<id>}
 * in its own tab, which the Runtime keeps as the record. What discovers each is its advancement's
 * criteria: holding one of the Pillar's items is vanilla's {@code inventory_changed}, and the rest are
 * the Pillar's own triggers here. A child sits under a parent only where meeting the child means the
 * parent was met first, since vanilla shows an undone advancement, its description too, once any
 * child is done. The {@code discovery} config switches them all off, every thing then known.
 */
public final class Discoveries {

    /** A first Wicked kill. */
    public static final String WICKED_KILL = "wicked_kill";
    /** Each Standing past Clean, the first time the player reaches it or one above it. */
    public static final String MARKED = "marked";
    public static final String OVERDUE = "overdue";
    public static final String FORFEIT = "forfeit";
    /** A Bailiff come for the player. */
    public static final String BAILIFF = "bailiff";
    /** A Bailiff seizing a stack from the player or from a container they opened. */
    public static final String SEIZURE = "seizure";
    /** A Bailiff's visit leaving the player owing. */
    public static final String OWING = "owing";
    /** A kind act lowering the player's Wickedness. */
    public static final String ATONEMENT = "atonement";
    /** A villager gossiping about one of the player's Wicked kills. */
    public static final String GOSSIP = "gossip";
    /** Holding each of the Pillar's items. */
    public static final String SEAL = "seal";
    public static final String TALLY_STICK = "tally_stick";
    public static final String STRONGBOX = "strongbox";
    public static final String BAILIFFS_BAG = "bailiffs_bag";

    public static final List<String> IDS = List.of(WICKED_KILL, MARKED, OVERDUE, FORFEIT, BAILIFF, SEIZURE, OWING,
            ATONEMENT, GOSSIP, SEAL, TALLY_STICK, STRONGBOX, BAILIFFS_BAG);

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, ATouchOfWickedness.MOD_ID);

    /** {@code atouchofwickedness:wicked_kill}: the player made a Wicked kill. */
    private static final Supplier<PlayerTrigger> WICKED_KILL_TRIGGER = TRIGGERS.register("wicked_kill", PlayerTrigger::new);
    /** {@code atouchofwickedness:standing}: the player's Standing became the one it names, or one above it. */
    private static final Supplier<StandingTrigger> STANDING = TRIGGERS.register("standing", StandingTrigger::new);
    /** {@code atouchofwickedness:bailiff_came}: a Bailiff came for the player. */
    private static final Supplier<PlayerTrigger> BAILIFF_CAME = TRIGGERS.register("bailiff_came", PlayerTrigger::new);
    /** {@code atouchofwickedness:seized}: a Bailiff seized a stack of the player's. */
    private static final Supplier<PlayerTrigger> SEIZED = TRIGGERS.register("seized", PlayerTrigger::new);
    /** {@code atouchofwickedness:owing}: a Bailiff's visit left the player owing. */
    private static final Supplier<PlayerTrigger> OWING_TRIGGER = TRIGGERS.register("owing", PlayerTrigger::new);
    /** {@code atouchofwickedness:atoned}: a kind act lowered the player's Wickedness. */
    private static final Supplier<PlayerTrigger> ATONED = TRIGGERS.register("atoned", PlayerTrigger::new);
    /** {@code atouchofwickedness:gossiped}: a villager gossiped about the player's Wicked kill. */
    private static final Supplier<PlayerTrigger> GOSSIPED = TRIGGERS.register("gossiped", PlayerTrigger::new);

    private static Discovery record;

    private Discoveries() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        record = Discovery.register(modBus, ATouchOfWickedness.MOD_ID, IDS, WickednessConfig::discovery);
    }

    /** The Runtime's record of the Pillar's Discoveries: what each player knows, and what this client was told. */
    public static Discovery record() {
        return record;
    }

    public static void wickedKill(ServerPlayer player) {
        WICKED_KILL_TRIGGER.get().trigger(player);
    }

    public static void standing(ServerPlayer player, Standing standing) {
        STANDING.get().trigger(player, standing);
    }

    public static void bailiffCame(ServerPlayer player) {
        BAILIFF_CAME.get().trigger(player);
    }

    public static void seized(ServerPlayer player) {
        SEIZED.get().trigger(player);
    }

    public static void owing(ServerPlayer player) {
        OWING_TRIGGER.get().trigger(player);
    }

    public static void atoned(ServerPlayer player) {
        ATONED.get().trigger(player);
    }

    public static void gossiped(ServerPlayer player) {
        GOSSIPED.get().trigger(player);
    }
}

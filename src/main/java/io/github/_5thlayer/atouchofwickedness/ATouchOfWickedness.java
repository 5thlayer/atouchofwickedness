// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness;

import io.github._5thlayer.atouchofwickedness.bag.BagChance;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import io.github._5thlayer.atouchofwickedness.bailiff.ItemValues;
import io.github._5thlayer.atouchofwickedness.bailiff.Seizable;
import io.github._5thlayer.atouchofwickedness.strongbox.Strongbox;
import io.github._5thlayer.atouchofwickedness.wickedness.KindActs;
import io.github._5thlayer.atouchofwickedness.wickedness.WickedKills;
import io.github._5thlayer.atouchofwickedness.wickedness.FleeTheForfeit;
import io.github._5thlayer.atouchofwickedness.wickedness.WarningSounds;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import io.github._5thlayer.atouchofwickedness.wickedness.WickednessCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Pillar's entry point. It registers Wickedness, its Warnings and the animals fleeing a Forfeit
 * player, the Wicked kills that raise it and the kind acts that atone for it, the Bailiff that comes
 * when it reaches the Threshold and the Seizable containers it robs, the Strongbox it never robs, the
 * Seal, the Tally Stick and the Bailiff's Bag, the {@code /wickedness} command, the common config and the Pillar's own
 * creative tab. The game tests hook themselves in from the {@code gametest} source set, which never
 * ships (libworks ADR 0011).
 */
@Mod(ATouchOfWickedness.MOD_ID)
public final class ATouchOfWickedness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofwickedness";

    public ATouchOfWickedness(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, WickednessConfig.SPEC);
        Wickedness.register(modBus);
        WarningSounds.register(modBus);
        FleeTheForfeit.register();
        WickedKills.register(modBus);
        KindActs.register();
        BailiffContent.register(modBus);
        Seizable.register(modBus);
        ItemValues.register(modBus);
        Strongbox.register(modBus);
        WickednessItems.register(modBus);
        BagChance.register(modBus);
        WickednessCommand.register();
        WickednessTab.register(modBus);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness;

import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import io.github._5thlayer.atouchofwickedness.wickedness.KindActs;
import io.github._5thlayer.atouchofwickedness.wickedness.WickedKills;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import io.github._5thlayer.atouchofwickedness.wickedness.WickednessCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Pillar's entry point. It registers Wickedness, the Wicked kills that raise it and the kind acts
 * that atone for it, the Bailiff that comes when it reaches the Threshold, the Seal and the Tally
 * Stick, the {@code /wickedness} command, the common config and the Pillar's own creative tab. The
 * game tests hook themselves in from the {@code gametest} source set, which never ships (libworks
 * ADR 0011).
 */
@Mod(ATouchOfWickedness.MOD_ID)
public final class ATouchOfWickedness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofwickedness";

    public ATouchOfWickedness(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, WickednessConfig.SPEC);
        Wickedness.register(modBus);
        WickedKills.register(modBus);
        KindActs.register();
        BailiffContent.register(modBus);
        WickednessItems.register(modBus);
        WickednessCommand.register();
        WickednessTab.register(modBus);
    }
}

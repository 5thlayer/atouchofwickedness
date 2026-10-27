// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The Pillar's common config, for a test to set one of its numbers for a while. */
final class Configs {

    private Configs() {
    }

    /** The common config's value at {@code path}, such as {@code bailiff.bagChance}. */
    static <T extends ModConfigSpec.ConfigValue<?>> T common(String path) {
        var spec = (ModConfigSpec) ModConfigs.getModConfigs(ATouchOfWickedness.MOD_ID).stream()
                .filter(config -> config.getType() == ModConfig.Type.COMMON).findFirst().orElseThrow().getSpec();
        return spec.getValues().get(path);
    }
}

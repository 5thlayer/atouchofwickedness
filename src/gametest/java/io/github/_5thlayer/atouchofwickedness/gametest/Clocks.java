// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.Map;
import java.util.WeakHashMap;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.server.level.ServerPlayer;

/**
 * The clocks of the game tests: each player's Decay and gifts read a clock of their own, which
 * stands at 0 until a test moves it, so no test reads or moves the world's clock, which every test
 * running alongside it shares, and no Decay creeps into a test that didn't ask for it.
 */
final class Clocks {

    /** Each player's clock, read and written on the server thread alone. */
    private static final Map<ServerPlayer, Long> TICKS = new WeakHashMap<>();

    private Clocks() {
    }

    /** Has the players' Decay and gifts read their clocks here, in place of the overworld's. */
    static void install() {
        Wickedness.tellClockBy(player -> TICKS.getOrDefault(player, 0L));
    }

    /** {@code ticks} pass on {@code player}'s clock at once, as sleeping skips a night. */
    static void pass(ServerPlayer player, long ticks) {
        TICKS.merge(player, ticks, Long::sum);
    }
}

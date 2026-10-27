// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * The gossip a Wicked kill spreads: each villager who sees it gains vanilla's minor negative gossip
 * about the killer, the kill's effective weight times the scale, so prices rise and iron golems turn on
 * the killer by vanilla's own rules. Plain Java, so JUnit tests it.
 */
public final class Gossip {

    private Gossip() {
    }

    /** @return the minor negative gossip a villager who sees a kill of effective weight {@code weight} gains */
    public static int about(int weight, double scale) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(weight * scale));
    }
}

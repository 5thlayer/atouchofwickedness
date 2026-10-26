// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * The wait after a Bailiff arrives for a player before another may come for them, counted on the
 * game's clock from the arrival. Plain Java, so JUnit tests it.
 */
public final class VisitCooldown {

    /** No Bailiff has arrived for the player yet. */
    public static final long NEVER = Long.MIN_VALUE;

    private VisitCooldown() {
    }

    /**
     * @param arrivedAt the game time the player's last Bailiff arrived, or {@link #NEVER}
     * @return whether the cooldown of {@code ticks} after that arrival is over at {@code now}; a clock
     *         behind the arrival, as in a world restored from a backup, counts as over
     */
    public static boolean over(long arrivedAt, long now, long ticks) {
        return arrivedAt == NEVER || now < arrivedAt || now - arrivedAt >= ticks;
    }
}

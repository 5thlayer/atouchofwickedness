// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * When a Warning sounds, and how far from its player: as a player enters Marked, Overdue or
 * Forfeit, and again each time their band's interval passes while they stay in it, counted on the
 * game's clock from the last one. A player falling into a lower band waits out that band's
 * interval; a Clean player hears none. Plain Java, so JUnit tests it.
 */
public final class Warning {

    /** No Warning has sounded for the player yet. */
    public static final long NEVER = Long.MIN_VALUE;

    private Warning() {
    }

    /**
     * A band's Warnings.
     *
     * @param interval the ticks between them
     * @param distance how far from the player, in blocks, they sound
     */
    public record Band(long interval, double distance) {
    }

    /** The Warnings of each band but Clean, which has none. */
    public record Rules(Band marked, Band overdue, Band forfeit) {

        /** @return the Warnings of {@code standing}, which is not Clean */
        public Band of(Standing standing) {
            return switch (standing) {
                case CLEAN -> throw new IllegalArgumentException("a Clean player hears no Warning");
                case MARKED -> marked;
                case OVERDUE -> overdue;
                case FORFEIT -> forfeit;
            };
        }
    }

    /**
     * @param was      the player's Standing when last seen
     * @param is       the player's Standing now
     * @param warnedAt the game time of the player's last Warning, or {@link #NEVER}
     * @param now      the game time now
     * @param rules    each band's Warnings
     * @return whether a Warning sounds for the player now; a clock behind the last Warning, as in a
     *         world restored from a backup, counts as its interval passed
     */
    public static boolean due(Standing was, Standing is, long warnedAt, long now, Rules rules) {
        if (is == Standing.CLEAN) {
            return false;
        }
        return is.compareTo(was) > 0 || warnedAt == NEVER || now < warnedAt || now - warnedAt >= rules.of(is).interval();
    }
}

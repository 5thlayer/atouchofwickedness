// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * When a Warning sounds: as a player enters Marked, Overdue or Forfeit, and again each time their
 * band's interval passes while they stay in it, counted on the game's clock from the last one. A
 * player falling into a lower band waits out that band's interval; a Clean player hears none.
 * Plain Java, so JUnit tests it.
 */
public final class Warning {

    /** No Warning has sounded for the player yet. */
    public static final long NEVER = Long.MIN_VALUE;

    private Warning() {
    }

    /**
     * The ticks between Warnings in each band.
     *
     * @param marked  between Warnings while Marked
     * @param overdue between Warnings while Overdue
     * @param forfeit between Warnings while Forfeit
     */
    public record Intervals(long marked, long overdue, long forfeit) {

        /** @return the ticks between Warnings in {@code standing}, which is not Clean */
        long of(Standing standing) {
            return switch (standing) {
                case CLEAN -> throw new IllegalArgumentException("a Clean player hears no Warning");
                case MARKED -> marked;
                case OVERDUE -> overdue;
                case FORFEIT -> forfeit;
            };
        }
    }

    /**
     * @param before   the player's Standing when last seen
     * @param now      the player's Standing now
     * @param warnedAt the game time of the player's last Warning, or {@link #NEVER}
     * @param time     the game time now
     * @return whether a Warning sounds for the player now; a clock behind the last Warning, as in a
     *         world restored from a backup, counts as its interval passed
     */
    public static boolean due(Standing before, Standing now, long warnedAt, long time, Intervals intervals) {
        if (now == Standing.CLEAN) {
            return false;
        }
        return now.compareTo(before) > 0 || warnedAt == NEVER || time < warnedAt || time - warnedAt >= intervals.of(now);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * How a Warning sounds in each band, and how often: the {@link Tally} has when one sounds.
 */
public final class Warning {

    private Warning() {
    }

    /**
     * How a band's Warnings sound.
     *
     * @param interval the ticks between them
     * @param distance how far from the player, in blocks, they sound
     */
    public record Sounding(long interval, double distance) {
    }

    /** The Warnings of each band but Clean, which has none. */
    public record Rules(Sounding marked, Sounding overdue, Sounding forfeit) {

        /** @return the Warnings of {@code standing}, which is not Clean */
        Sounding of(Standing standing) {
            return switch (standing) {
                case CLEAN -> throw new IllegalArgumentException("a Clean player hears no Warning");
                case MARKED -> marked;
                case OVERDUE -> overdue;
                case FORFEIT -> forfeit;
            };
        }
    }
}

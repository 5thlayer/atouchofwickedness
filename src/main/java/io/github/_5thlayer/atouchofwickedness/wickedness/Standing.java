// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.Locale;

/**
 * The rough band of a player's Wickedness against their Threshold, which the Tally Stick shows and
 * never the number: Clean, Marked, Overdue or Forfeit. Plain Java, so JUnit tests it.
 */
public enum Standing {
    CLEAN,
    MARKED,
    OVERDUE,
    FORFEIT;

    /**
     * Where each band starts, as a fraction of the Threshold.
     *
     * @param marked  where Marked starts, ½ by default
     * @param overdue where Overdue starts, ¾ by default
     * @param forfeit where Forfeit starts, 90% by default
     */
    public record Bands(double marked, double overdue, double forfeit) {
    }

    /** @return the Standing of {@code wickedness} against {@code threshold} */
    public static Standing of(int wickedness, int threshold, Bands bands) {
        if (reaches(wickedness, threshold, bands.forfeit())) {
            return FORFEIT;
        }
        if (reaches(wickedness, threshold, bands.overdue())) {
            return OVERDUE;
        }
        if (reaches(wickedness, threshold, bands.marked())) {
            return MARKED;
        }
        return CLEAN;
    }

    /** The translation key of the Standing's name in the action bar. */
    public String translationKey() {
        return "atouchofwickedness.standing." + name().toLowerCase(Locale.ROOT);
    }

    private static boolean reaches(int wickedness, int threshold, double fraction) {
        // A hair under, so 90% of 80, which doubles hold as 72.00000000000001, starts at 72.
        return wickedness >= fraction * threshold - 1e-9;
    }
}

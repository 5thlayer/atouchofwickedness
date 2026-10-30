// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.Locale;

/**
 * The rough band of a player's Wickedness against their Threshold, which the Tally Stick shows and
 * never the number: Clean, Marked, Overdue or Forfeit. The {@link Tally} reads it.
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

    /** The translation key of the Standing's name in the action bar. */
    public String translationKey() {
        return "atouchofwickedness.standing." + name().toLowerCase(Locale.ROOT);
    }
}

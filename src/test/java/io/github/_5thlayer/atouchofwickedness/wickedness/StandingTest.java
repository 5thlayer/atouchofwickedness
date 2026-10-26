// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A player's Standing is Clean below ½ of their Threshold, Marked from ½, Overdue from ¾ and Forfeit from 90%. */
class StandingTest {

    private static final Standing.Bands BANDS = new Standing.Bands(0.5, 0.75, 0.9);

    @Test
    void nothingIsClean() {
        assertEquals(Standing.CLEAN, Standing.of(0, 80, BANDS));
    }

    @Test
    void justBelowHalfIsClean() {
        assertEquals(Standing.CLEAN, Standing.of(39, 80, BANDS));
    }

    @Test
    void halfIsMarked() {
        assertEquals(Standing.MARKED, Standing.of(40, 80, BANDS));
    }

    @Test
    void threeQuartersIsOverdue() {
        assertEquals(Standing.OVERDUE, Standing.of(60, 80, BANDS));
    }

    @Test
    void justBelowNinetyPercentIsOverdue() {
        assertEquals(Standing.OVERDUE, Standing.of(71, 80, BANDS));
    }

    @Test
    void ninetyPercentIsForfeit() {
        assertEquals(Standing.FORFEIT, Standing.of(72, 80, BANDS));
    }

    @Test
    void anOddThresholdRoundsNoBandEarly() {
        // Half of 75 is 37.5: 37 is still Clean, 38 is Marked.
        assertEquals(Standing.CLEAN, Standing.of(37, 75, BANDS));
        assertEquals(Standing.MARKED, Standing.of(38, 75, BANDS));
    }

    @Test
    void theBandsFollowTheConfig() {
        var early = new Standing.Bands(0.1, 0.2, 0.3);
        assertEquals(Standing.FORFEIT, Standing.of(30, 100, early));
    }
}

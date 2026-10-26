// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A Warning sounds as a player enters Marked, Overdue or Forfeit, and again each time their band's
 * interval passes while they stay in it; never while Clean.
 */
class WarningTest {

    private static final Warning.Rules RULES = new Warning.Rules(
            new Warning.Band(2_400, 24), new Warning.Band(1_200, 12), new Warning.Band(600, 4));

    @Test
    void cleanNeverWarns() {
        assertFalse(Warning.due(Standing.CLEAN, Standing.CLEAN, Warning.NEVER, 100_000, RULES));
        assertFalse(Warning.due(Standing.MARKED, Standing.CLEAN, 0, 100_000, RULES));
    }

    @Test
    void enteringEachBandWarnsAtOnce() {
        assertTrue(Warning.due(Standing.CLEAN, Standing.MARKED, Warning.NEVER, 1_000, RULES));
        assertTrue(Warning.due(Standing.MARKED, Standing.OVERDUE, 999, 1_000, RULES));
        assertTrue(Warning.due(Standing.OVERDUE, Standing.FORFEIT, 999, 1_000, RULES));
    }

    @Test
    void leapingPastABandWarnsOfTheOneEntered() {
        assertTrue(Warning.due(Standing.CLEAN, Standing.FORFEIT, Warning.NEVER, 1_000, RULES));
    }

    @Test
    void stayingInABandWaitsOutItsInterval() {
        assertFalse(Warning.due(Standing.MARKED, Standing.MARKED, 1_000, 3_399, RULES));
        assertTrue(Warning.due(Standing.MARKED, Standing.MARKED, 1_000, 3_400, RULES));
        assertFalse(Warning.due(Standing.OVERDUE, Standing.OVERDUE, 1_000, 2_199, RULES));
        assertTrue(Warning.due(Standing.OVERDUE, Standing.OVERDUE, 1_000, 2_200, RULES));
        assertFalse(Warning.due(Standing.FORFEIT, Standing.FORFEIT, 1_000, 1_599, RULES));
        assertTrue(Warning.due(Standing.FORFEIT, Standing.FORFEIT, 1_000, 1_600, RULES));
    }

    @Test
    void fallingIntoALowerBandWaitsOutThatBandsInterval() {
        assertFalse(Warning.due(Standing.FORFEIT, Standing.OVERDUE, 1_000, 1_600, RULES));
        assertTrue(Warning.due(Standing.FORFEIT, Standing.OVERDUE, 1_000, 2_200, RULES));
    }

    @Test
    void aClockBehindTheLastWarningWarns() {
        assertTrue(Warning.due(Standing.MARKED, Standing.MARKED, 1_000, 999, RULES));
    }
}

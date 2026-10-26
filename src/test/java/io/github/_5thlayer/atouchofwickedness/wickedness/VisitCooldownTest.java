// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** No other Bailiff comes for a player until the cooldown after the last one's arrival is over. */
class VisitCooldownTest {

    @Test
    void aPlayerNeverVisitedIsNotWaiting() {
        assertTrue(VisitCooldown.over(VisitCooldown.NEVER, 0, 6_000));
        assertTrue(VisitCooldown.over(VisitCooldown.NEVER, Long.MAX_VALUE, 6_000));
    }

    @Test
    void theTickOfTheArrivalIsWithinTheCooldown() {
        assertFalse(VisitCooldown.over(1_000, 1_000, 6_000));
    }

    @Test
    void theLastTickBeforeTheEndIsWithinTheCooldown() {
        assertFalse(VisitCooldown.over(1_000, 6_999, 6_000));
    }

    @Test
    void theCooldownIsOverAsItsTicksRunOut() {
        assertTrue(VisitCooldown.over(1_000, 7_000, 6_000));
        assertTrue(VisitCooldown.over(1_000, 100_000, 6_000));
    }

    @Test
    void noCooldownLetsTheNextComeAtOnce() {
        assertTrue(VisitCooldown.over(1_000, 1_000, 0));
    }

    @Test
    void aClockBehindTheArrivalCountsAsOver() {
        assertTrue(VisitCooldown.over(1_000, 999, 6_000));
    }
}

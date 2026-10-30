// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Healing a pet lowers Wickedness by an amount for each whole heart it regains; the {@link TallyTest} has the rest of Atonement. */
class AtonementTest {

    @Test
    void healingTakesOffPerWholeHeartRegained() {
        assertEquals(8, Atonement.healing(16, 1));
        assertEquals(2, Atonement.healing(5, 1));
        assertEquals(16, Atonement.healing(16, 2));
    }

    @Test
    void healingLessThanAHeartTakesOffNothing() {
        assertEquals(0, Atonement.healing(1, 1));
        assertEquals(0, Atonement.healing(0, 1));
    }
}

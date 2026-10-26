// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github._5thlayer.atouchofwickedness.wickedness.Decay.State;
import org.junit.jupiter.api.Test;

/**
 * Wickedness falls by the daily Decay over each in-game day of 24000 ticks, a whole point at a time,
 * carrying the part of a point between steps, and stops at 0 with nothing banked.
 */
class DecayTest {

    @Test
    void aDayTakesOffTheDailyDecay() {
        assertEquals(new State(40, 0), Decay.over(new State(50, 0), Decay.TICKS_PER_DAY, 10));
    }

    @Test
    void aPointFallsEveryTenthOfADay() {
        assertEquals(new State(49, 0), Decay.over(new State(50, 0), 2_400, 10));
        assertEquals(new State(50, 23_990), Decay.over(new State(50, 0), 2_399, 10));
        assertEquals(new State(49, 0), Decay.over(new State(50, 23_990), 1, 10));
    }

    @Test
    void aDayTickByTickTakesOffTheSameAsAtOnce() {
        var state = new State(50, 0);
        for (int tick = 0; tick < Decay.TICKS_PER_DAY; tick++) {
            state = Decay.over(state, 1, 10);
        }
        assertEquals(new State(40, 0), state);
    }

    @Test
    void itStopsAt0WithNothingBanked() {
        assertEquals(new State(0, 0), Decay.over(new State(3, 0), Decay.TICKS_PER_DAY, 10));
        assertEquals(new State(0, 0), Decay.over(new State(0, 0), 2_399, 10));
    }

    @Test
    void noTimeOrTimeTurnedBackTakesOffNothing() {
        assertEquals(new State(50, 7), Decay.over(new State(50, 7), 0, 10));
        assertEquals(new State(50, 7), Decay.over(new State(50, 7), -Decay.TICKS_PER_DAY, 10));
    }

    @Test
    void noDailyDecayTakesOffNothing() {
        assertEquals(new State(50, 0), Decay.over(new State(50, 0), Decay.TICKS_PER_DAY, 0));
    }

    @Test
    void theDayIsTheClockInWholeDays() {
        assertEquals(0, Decay.dayOf(0));
        assertEquals(0, Decay.dayOf(Decay.TICKS_PER_DAY - 1));
        assertEquals(1, Decay.dayOf(Decay.TICKS_PER_DAY));
        assertEquals(3, Decay.dayOf(3 * Decay.TICKS_PER_DAY + 100));
    }

    @Test
    void aVeryLongTimeTakesOffEverything() {
        assertEquals(new State(0, 0), Decay.over(new State(Integer.MAX_VALUE, 0), Long.MAX_VALUE, 1_000_000));
    }
}

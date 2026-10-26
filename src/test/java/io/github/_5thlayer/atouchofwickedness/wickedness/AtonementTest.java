// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github._5thlayer.atouchofwickedness.wickedness.Atonement.Gifts;
import org.junit.jupiter.api.Test;

/**
 * Each Atonement takes its amount off Wickedness, never below 0: breeding 3, taming 10, curing a
 * zombie villager 40 and a gift 2 by default, and feeding a hurt pet 1 for each whole heart it
 * regains. Only so many gifts count each in-game day.
 */
class AtonementTest {

    private static final int DAY = 24_000;

    @Test
    void eachAtonementTakesOffItsAmount() {
        assertEquals(47, Atonement.lowered(50, 3));
        assertEquals(40, Atonement.lowered(50, 10));
        assertEquals(10, Atonement.lowered(50, 40));
        assertEquals(48, Atonement.lowered(50, 2));
    }

    @Test
    void anAtonementStopsAt0() {
        assertEquals(0, Atonement.lowered(30, 40));
        assertEquals(0, Atonement.lowered(0, 3));
    }

    @Test
    void feedingTakesOffPerWholeHeartRegained() {
        assertEquals(8, Atonement.feeding(16, 1));
        assertEquals(2, Atonement.feeding(5, 1));
        assertEquals(16, Atonement.feeding(16, 2));
    }

    @Test
    void feedingAPetThatRegainsLessThanAHeartTakesOffNothing() {
        assertEquals(0, Atonement.feeding(1, 1));
        assertEquals(0, Atonement.feeding(0, 1));
    }

    @Test
    void theDayIsTheClockInWholeDays() {
        assertEquals(0, Gifts.dayOf(0));
        assertEquals(0, Gifts.dayOf(DAY - 1));
        assertEquals(1, Gifts.dayOf(DAY));
        assertEquals(3, Gifts.dayOf(3 * DAY + 100));
    }

    @Test
    void theCapCountsTheDaysGifts() {
        var gifts = Gifts.NONE;
        for (int gift = 1; gift <= 5; gift++) {
            assertTrue(gifts.counts(2, 5), "gift " + gift + " of the day");
            gifts = gifts.plusOne(2);
        }
        assertFalse(gifts.counts(2, 5), "a sixth gift on the same day");
    }

    @Test
    void theCapStartsAfreshOnANewDay() {
        var full = new Gifts(5, 2);
        assertTrue(full.counts(3, 5));
        assertEquals(new Gifts(1, 3), full.plusOne(3));
    }

    @Test
    void theCapStartsAfreshWhenTheClockIsTurnedBack() {
        assertTrue(new Gifts(5, 2).counts(1, 5));
    }

    @Test
    void noCapCountsNoGift() {
        assertFalse(Gifts.NONE.counts(0, 0));
    }
}

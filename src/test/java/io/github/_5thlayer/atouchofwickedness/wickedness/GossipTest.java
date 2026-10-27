// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github._5thlayer.atouchofwickedness.wickedness.Gossip.Rules;
import org.junit.jupiter.api.Test;

/**
 * A villager who sees a Wicked kill gains the kill's effective weight times the scale in minor
 * negative gossip, rounded to the nearest whole number.
 */
class GossipTest {

    private static Rules rules(double scale) {
        return new Rules(16, scale);
    }

    @Test
    void aKillAddsItsWeightTimesTheScale() {
        assertEquals(10, Gossip.of(2, rules(5.0)));
        assertEquals(150, Gossip.of(30, rules(5.0)));
    }

    @Test
    void theGossipIsRoundedToTheNearestWholeNumber() {
        assertEquals(3, Gossip.of(2, rules(1.25)));
        assertEquals(2, Gossip.of(2, rules(1.2)));
    }

    @Test
    void aWeightlessKillAddsNothing() {
        assertEquals(0, Gossip.of(0, rules(5.0)));
    }

    @Test
    void aScaleOfNothingAddsNothing() {
        assertEquals(0, Gossip.of(30, rules(0)));
    }

    @Test
    void theGossipNeverOverflows() {
        assertEquals(Integer.MAX_VALUE, Gossip.of(Integer.MAX_VALUE, rules(100)));
    }
}

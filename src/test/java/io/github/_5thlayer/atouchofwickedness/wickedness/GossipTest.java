// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A villager who sees a Wicked kill gains the kill's effective weight times the scale in minor
 * negative gossip, rounded to the nearest whole number.
 */
class GossipTest {

    @Test
    void aKillAddsItsWeightTimesTheScale() {
        assertEquals(10, Gossip.about(2, 5.0));
        assertEquals(150, Gossip.about(30, 5.0));
    }

    @Test
    void aHeavierKillAddsMore() {
        assertEquals(25, Gossip.about(5, 5.0));
    }

    @Test
    void theGossipIsRoundedToTheNearestWholeNumber() {
        assertEquals(3, Gossip.about(2, 1.25));
        assertEquals(2, Gossip.about(2, 1.2));
    }

    @Test
    void aWeightlessKillAddsNothing() {
        assertEquals(0, Gossip.about(0, 5.0));
    }

    @Test
    void aScaleOfNothingAddsNothing() {
        assertEquals(0, Gossip.about(30, 0));
    }

    @Test
    void theGossipNeverOverflows() {
        assertEquals(Integer.MAX_VALUE, Gossip.about(Integer.MAX_VALUE, 100));
    }
}

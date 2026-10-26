// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** A player's opened containers keep the most recent first, each once, and no more than the length. */
class OpenedTest {

    @Test
    void theLatestOpenedComesFirst() {
        var opened = Opened.<String>none().plus("a", 32).plus("b", 32);
        assertEquals(List.of("b", "a"), opened.recent());
    }

    @Test
    void openingOneAgainBringsItToTheFrontOnce() {
        var opened = Opened.<String>none().plus("a", 32).plus("b", 32).plus("a", 32);
        assertEquals(List.of("a", "b"), opened.recent());
    }

    @Test
    void atTheLengthTheOldestFallsOff() {
        var opened = Opened.<String>none().plus("a", 2).plus("b", 2).plus("c", 2);
        assertEquals(List.of("c", "b"), opened.recent());
    }

    @Test
    void reopeningAtTheLengthLosesNothing() {
        var opened = Opened.<String>none().plus("a", 2).plus("b", 2).plus("a", 2);
        assertEquals(List.of("a", "b"), opened.recent());
    }

    @Test
    void aShorterLengthTrimsTheOldest() {
        var opened = Opened.<String>none().plus("a", 3).plus("b", 3).plus("c", 3).plus("d", 1);
        assertEquals(List.of("d"), opened.recent());
    }
}

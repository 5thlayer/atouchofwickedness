// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** The Threshold is drawn uniformly from its range, both ends included, from a random the caller supplies. */
class ThresholdTest {

    @Test
    void theLowestDrawIsTheBottomOfTheRange() {
        assertEquals(50, Threshold.draw(50, 100, bound -> 0));
    }

    @Test
    void theHighestDrawIsTheTopOfTheRange() {
        assertEquals(100, Threshold.draw(50, 100, bound -> bound - 1));
    }

    @Test
    void everyValueInTheRangeCanBeDrawn() {
        var random = new Random(3);
        var drawn = new HashSet<Integer>();
        for (int i = 0; i < 5_000; i++) {
            drawn.add(Threshold.draw(50, 100, random::nextInt));
        }
        assertEquals(51, drawn.size());
        assertEquals(50, drawn.stream().mapToInt(Integer::intValue).min().orElseThrow());
        assertEquals(100, drawn.stream().mapToInt(Integer::intValue).max().orElseThrow());
    }

    @Test
    void aRangeOfOneValueAlwaysDrawsIt() {
        assertEquals(70, Threshold.draw(70, 70, bound -> bound - 1));
    }

    @Test
    void anUpsideDownRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Threshold.draw(100, 50, bound -> 0));
    }
}

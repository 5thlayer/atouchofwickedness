// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** A creature weighs its entry in the data map, and nothing when it has none. */
class WickednessWeightTest {

    private static final Map<String, Integer> ENTRIES = Map.of("cow", 2, "villager", 30);

    @Test
    void aListedCreatureWeighsItsEntry() {
        assertEquals(2, WickednessWeight.of(ENTRIES.get("cow")));
        assertEquals(30, WickednessWeight.of(ENTRIES.get("villager")));
    }

    @Test
    void anUnlistedCreatureWeighsNothing() {
        assertEquals(0, WickednessWeight.of(ENTRIES.get("zombie")));
    }
}

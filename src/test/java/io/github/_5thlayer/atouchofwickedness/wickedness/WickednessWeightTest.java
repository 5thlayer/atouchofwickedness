// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import io.github._5thlayer.atouchofwickedness.wickedness.WickednessWeight.Rules;
import io.github._5thlayer.atouchofwickedness.wickedness.WickednessWeight.Victim;
import org.junit.jupiter.api.Test;

/**
 * A creature weighs its entry in the data map, and nothing when it has none; a tamed one weighs the
 * tamed weight, a baby the baby multiple, and one that was going for its killer nothing. Each hit on
 * a creature weighing the torment weight or more costs the hit cost.
 */
class WickednessWeightTest {

    private static final Map<String, Integer> ENTRIES = Map.of("cow", 2, "villager", 30, "wolf", 15);

    private static final Rules RULES = new Rules(50, 2.0, 30, 3);

    private static final Victim WILD = new Victim(false, false, false);
    private static final Victim TAMED = new Victim(true, false, false);
    private static final Victim BABY = new Victim(false, true, false);
    private static final Victim DEFENDING = new Victim(false, false, true);

    @Test
    void aListedCreatureWeighsItsEntry() {
        assertEquals(2, WickednessWeight.of(ENTRIES.get("cow")));
        assertEquals(30, WickednessWeight.of(ENTRIES.get("villager")));
    }

    @Test
    void anUnlistedCreatureWeighsNothing() {
        assertEquals(0, WickednessWeight.of(ENTRIES.get("zombie")));
    }

    @Test
    void anAdultWildCreatureWeighsItsEntry() {
        assertEquals(2, WickednessWeight.effective(2, WILD, RULES));
    }

    @Test
    void aTamedCreatureWeighsTheTamedWeightWhateverItsEntry() {
        assertEquals(50, WickednessWeight.effective(15, TAMED, RULES));
        assertEquals(50, WickednessWeight.effective(0, TAMED, RULES));
        assertEquals(50, WickednessWeight.effective(50, TAMED, RULES));
    }

    @Test
    void aBabyWeighsItsWeightTimesTheMultiplier() {
        assertEquals(4, WickednessWeight.effective(2, BABY, RULES));
        assertEquals(100, WickednessWeight.effective(50, BABY, RULES));
        assertEquals(100, WickednessWeight.effective(15, new Victim(true, true, false), RULES));
        assertEquals(0, WickednessWeight.effective(0, BABY, RULES));
    }

    @Test
    void aFractionalMultiplierRoundsToTheNearest() {
        assertEquals(8, WickednessWeight.effective(5, BABY, new Rules(50, 1.5, 30, 3)));
        assertEquals(3, WickednessWeight.effective(2, BABY, new Rules(50, 1.5, 30, 3)));
    }

    @Test
    void aCreatureGoingForItsKillerWeighsNothing() {
        assertEquals(0, WickednessWeight.effective(15, DEFENDING, RULES));
        assertEquals(0, WickednessWeight.effective(15, new Victim(true, true, true), RULES));
    }

    @Test
    void aHitCostsOnlyFromTheTormentWeightUp() {
        assertEquals(3, WickednessWeight.hitCost(30, RULES));
        assertEquals(3, WickednessWeight.hitCost(100, RULES));
        assertEquals(0, WickednessWeight.hitCost(29, RULES));
        assertEquals(0, WickednessWeight.hitCost(0, RULES));
    }

    @Test
    void aHitOnNothingWeighedCostsNothingEvenWithNoTormentWeight() {
        assertEquals(0, WickednessWeight.hitCost(0, new Rules(50, 2.0, 0, 3)));
        assertEquals(3, WickednessWeight.hitCost(1, new Rules(50, 2.0, 0, 3)));
    }
}

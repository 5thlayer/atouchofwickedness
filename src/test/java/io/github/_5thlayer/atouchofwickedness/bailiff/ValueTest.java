// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import io.github._5thlayer.atouchofwickedness.bailiff.Value.Rules;
import org.junit.jupiter.api.Test;

/**
 * An item is worth its entry in the data map, or the unlisted Value when it has none; each level of
 * its enchantments adds the bonus, unless it is worth nothing. A stack is worth its items' Value
 * times its count. An item on the ground is worth going for at the floor and above, and the draw
 * lands on each stack in proportion to what it is worth, never on one worth nothing.
 */
class ValueTest {

    private static final Map<String, Integer> ENTRIES = Map.of("diamond", 100, "dirt", 0);

    private static final Rules RULES = new Rules(1, 20, 5);

    @Test
    void aListedItemIsWorthItsEntry() {
        assertEquals(100, Value.of(ENTRIES.get("diamond"), RULES));
        assertEquals(0, Value.of(ENTRIES.get("dirt"), RULES));
    }

    @Test
    void anUnlistedItemIsWorthTheUnlistedValue() {
        assertEquals(1, Value.of(ENTRIES.get("stick"), RULES));
    }

    @Test
    void eachEnchantmentLevelAddsTheBonus() {
        assertEquals(100, Value.perItem(100, 0, RULES));
        assertEquals(160, Value.perItem(100, 3, RULES));
    }

    @Test
    void anItemWorthNothingStaysWorthNothingEnchanted() {
        assertEquals(0, Value.perItem(0, 5, RULES));
    }

    @Test
    void aStackIsWorthItsItemsValueTimesItsCount() {
        assertEquals(6_400, Value.ofStack(100, 64));
        assertEquals(0, Value.ofStack(0, 64));
    }

    @Test
    void anItemOnTheGroundIsWorthGoingForAtTheFloorAndAbove() {
        assertFalse(Value.worthPickingUp(4, RULES));
        assertTrue(Value.worthPickingUp(5, RULES));
        assertTrue(Value.worthPickingUp(100, RULES));
    }

    @Test
    void anItemWorthNothingIsNeverWorthGoingForWhateverTheFloor() {
        assertFalse(Value.worthPickingUp(0, new Rules(1, 20, 0)));
    }

    @Test
    void anyWorthIsWhetherAnyStackIsWorthSomething() {
        assertFalse(Value.anyWorth(new long[] {}));
        assertFalse(Value.anyWorth(new long[] {0, 0}));
        assertTrue(Value.anyWorth(new long[] {0, 1}));
    }

    @Test
    void nothingWorthAnythingDrawsNothing() {
        assertEquals(-1, Value.draw(new long[] {}, 0.5));
        assertEquals(-1, Value.draw(new long[] {0, 0, 0}, 0.5));
    }

    @Test
    void theOnlyStackWorthAnythingIsAlwaysDrawn() {
        var values = new long[] {0, 7, 0};
        assertEquals(1, Value.draw(values, 0));
        assertEquals(1, Value.draw(values, 0.5));
        assertEquals(1, Value.draw(values, Math.nextDown(1.0)));
    }

    @Test
    void eachStackIsDrawnInProportionToItsValue() {
        // 1 of 4 for the first, 3 of 4 for the second.
        var values = new long[] {1, 0, 3};
        assertEquals(0, Value.draw(values, 0));
        assertEquals(0, Value.draw(values, Math.nextDown(0.25)));
        assertEquals(2, Value.draw(values, 0.25));
        assertEquals(2, Value.draw(values, Math.nextDown(1.0)));
    }

    @Test
    void aRollRoundingUpToTheTotalStillLandsOnTheLastStack() {
        var values = new long[] {Long.MAX_VALUE / 2, Long.MAX_VALUE / 2};
        assertEquals(1, Value.draw(values, Math.nextDown(1.0)));
    }
}

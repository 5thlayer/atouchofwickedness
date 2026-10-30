// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.github._5thlayer.atouchofwickedness.bailiff.Value.Rules;
import io.github._5thlayer.atouchofwickedness.bailiff.Value.Stack;
import org.junit.jupiter.api.Test;

/**
 * An item is worth its entry in the data map, or the unlisted Value when it has none; each level of
 * its enchantments adds the bonus, unless it is worth nothing. A stack is worth its items' Value
 * times its count, and the one the Bailiff takes is drawn in proportion to it, never one worth
 * nothing and never its player's held one. An item on the ground is worth going for when each of
 * its items is worth the floor or more.
 */
class ValueTest {

    private static final Rules RULES = new Rules(1, 20, 5);

    private static final Stack EMPTY = new Stack(null, 0, 0);

    private static Stack listed(int value, int count) {
        return new Stack(value, 0, count);
    }

    private static Stack unlisted(int count) {
        return new Stack(null, 0, count);
    }

    private static Stack enchanted(int value, int levels) {
        return new Stack(value, levels, 1);
    }

    @Test
    void aListedItemIsWorthItsEntry() {
        // 3 against an unlisted 1.
        var stacks = List.of(listed(3, 1), unlisted(1));
        assertEquals(0, Value.fromContainer(stacks, RULES, Math.nextDown(0.75)));
        assertEquals(1, Value.fromContainer(stacks, RULES, 0.75));
    }

    @Test
    void anUnlistedItemIsWorthTheUnlistedValue() {
        var stacks = List.of(unlisted(1), listed(3, 1));
        assertEquals(0, Value.fromContainer(stacks, RULES, Math.nextDown(0.25)));
        assertEquals(1, Value.fromContainer(stacks, RULES, 0.25));
    }

    @Test
    void eachEnchantmentLevelAddsTheBonus() {
        // 40 plus 3 levels of 20 is 100, against 100.
        var stacks = List.of(enchanted(40, 3), listed(100, 1));
        assertEquals(0, Value.fromContainer(stacks, RULES, Math.nextDown(0.5)));
        assertEquals(1, Value.fromContainer(stacks, RULES, 0.5));
    }

    @Test
    void anItemWorthNothingIsNeverTakenEnchanted() {
        assertEquals(-1, Value.fromContainer(List.of(enchanted(0, 5)), RULES, 0.5));
    }

    @Test
    void aStackIsWorthItsItemsValueTimesItsCount() {
        // 64 at 1 each against one at 64.
        var stacks = List.of(listed(1, 64), listed(64, 1));
        assertEquals(0, Value.fromContainer(stacks, RULES, Math.nextDown(0.5)));
        assertEquals(1, Value.fromContainer(stacks, RULES, 0.5));
    }

    @Test
    void anEmptySlotIsWorthNothing() {
        assertEquals(-1, Value.fromContainer(List.of(EMPTY, EMPTY), RULES, 0.5));
        assertEquals(1, Value.fromContainer(List.of(EMPTY, unlisted(1), EMPTY), RULES, 0));
    }

    @Test
    void nothingWorthAnythingDrawsNothing() {
        assertEquals(-1, Value.fromContainer(List.of(), RULES, 0.5));
        assertEquals(-1, Value.fromContainer(List.of(listed(0, 64), listed(0, 1), EMPTY), RULES, 0.5));
    }

    @Test
    void theOnlyStackWorthAnythingIsAlwaysDrawn() {
        var stacks = List.of(listed(0, 64), listed(7, 1), EMPTY);
        assertEquals(1, Value.fromContainer(stacks, RULES, 0));
        assertEquals(1, Value.fromContainer(stacks, RULES, 0.5));
        assertEquals(1, Value.fromContainer(stacks, RULES, Math.nextDown(1.0)));
    }

    @Test
    void eachStackIsDrawnInProportionToItsValue() {
        // 1 of 4 for the first, 3 of 4 for the third.
        var stacks = List.of(listed(1, 1), EMPTY, listed(3, 1));
        assertEquals(0, Value.fromContainer(stacks, RULES, 0));
        assertEquals(0, Value.fromContainer(stacks, RULES, Math.nextDown(0.25)));
        assertEquals(2, Value.fromContainer(stacks, RULES, 0.25));
        assertEquals(2, Value.fromContainer(stacks, RULES, Math.nextDown(1.0)));
    }

    @Test
    void aRollRoundingUpToTheTotalStillLandsOnTheLastStack() {
        var dearest = listed(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(1, Value.fromContainer(List.of(dearest, dearest), RULES, Math.nextDown(1.0)));
    }

    @Test
    void everySlotOfAContainerMayBeDrawn() {
        var stacks = List.of(listed(1, 1), listed(1, 1));
        assertEquals(0, Value.fromContainer(stacks, RULES, 0));
        assertEquals(1, Value.fromContainer(stacks, RULES, Math.nextDown(1.0)));
    }

    @Test
    void theHeldStackIsNeverDrawnFromThePlayer() {
        var main = List.of(listed(1, 1), listed(1_000, 1), listed(1, 1));
        for (double roll : new double[] {0, 0.5, Math.nextDown(1.0)}) {
            assertTrue(Value.fromPlayer(main, 1, RULES, roll) != 1);
        }
        assertEquals(0, Value.fromPlayer(main, 1, RULES, Math.nextDown(0.5)));
        assertEquals(2, Value.fromPlayer(main, 1, RULES, 0.5));
    }

    @Test
    void aPlayerHoldingTheirOnlyStackWorthAnythingHasNothingToTake() {
        var main = List.of(EMPTY, listed(0, 64), listed(100, 1));
        assertEquals(-1, Value.fromPlayer(main, 2, RULES, 0.5));
        assertFalse(Value.playerHasTakeable(main, 2, RULES));
    }

    @Test
    void aPlayerHasSomethingToTakeWhenAStackButTheHeldOneIsWorthAnything() {
        var main = List.of(EMPTY, listed(0, 64), listed(100, 1));
        assertTrue(Value.playerHasTakeable(main, 0, RULES));
        assertEquals(2, Value.fromPlayer(main, 0, RULES, 0.5));
    }

    @Test
    void aPlayerWithNothingWorthAnythingHasNothingToTake() {
        assertFalse(Value.playerHasTakeable(List.of(EMPTY, listed(0, 64), unlisted(0)), 0, RULES));
    }

    @Test
    void anItemOnTheGroundIsWorthGoingForAtTheFloorAndAbove() {
        assertFalse(Value.worthPickingUp(listed(4, 1), RULES));
        assertTrue(Value.worthPickingUp(listed(5, 1), RULES));
        assertTrue(Value.worthPickingUp(listed(100, 1), RULES));
    }

    @Test
    void anItemOnTheGroundIsJudgedEachNotByItsStack() {
        assertFalse(Value.worthPickingUp(listed(1, 64), RULES));
    }

    @Test
    void enchantmentsMayLiftAnItemOnTheGroundOverTheFloor() {
        assertFalse(Value.worthPickingUp(new Stack(null, 0, 1), RULES));
        assertTrue(Value.worthPickingUp(new Stack(null, 1, 1), RULES));
    }

    @Test
    void anItemWorthNothingIsNeverWorthGoingForWhateverTheFloor() {
        assertFalse(Value.worthPickingUp(listed(0, 1), new Rules(1, 20, 0)));
        assertFalse(Value.worthPickingUp(enchanted(0, 5), new Rules(1, 20, 0)));
    }

    @Test
    void anEmptyStackIsNeverWorthGoingFor() {
        assertFalse(Value.worthPickingUp(new Stack(100, 0, 0), RULES));
    }
}

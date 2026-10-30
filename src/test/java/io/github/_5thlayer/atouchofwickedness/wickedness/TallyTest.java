// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;

import io.github._5thlayer.atouchofwickedness.wickedness.Tally.Look;
import io.github._5thlayer.atouchofwickedness.wickedness.Tally.Rules;
import org.junit.jupiter.api.Test;

/**
 * A Tally by the rules: a Threshold drawn from 50 to 100; a Bailiff due at the Threshold once the
 * cooldown after the last is over, resetting Wickedness and drawing a new Threshold as it arrives, and
 * one that leaves with nothing leaving its player owing; Atonements and the day's gifts lowering
 * Wickedness at once; Decay of 10 a day, a point at a time; and the Standings, Clean, Marked from ½,
 * Overdue from ¾ and Forfeit from 90%, with each band's Warnings.
 */
class TallyTest {

    private static final Standing.Bands BANDS = new Standing.Bands(0.5, 0.75, 0.9);

    private static final Warning.Rules WARNINGS = new Warning.Rules(
            new Warning.Sounding(2_400, 24), new Warning.Sounding(1_200, 12), new Warning.Sounding(600, 4));

    private static final Rules RULES = new Rules(50, 100, 6_000, 10, BANDS, WARNINGS, 2, 5);

    private static final long DAY = Tally.TICKS_PER_DAY;

    // The Threshold

    @Test
    void aNewPlayerHasNoWickedness() {
        assertEquals(0, Tally.fresh(RULES, bound -> 0).wickedness());
    }

    @Test
    void theLowestDrawIsTheBottomOfTheRange() {
        assertEquals(50, Tally.fresh(RULES, bound -> 0).threshold());
    }

    @Test
    void theHighestDrawIsTheTopOfTheRange() {
        assertEquals(100, Tally.fresh(RULES, bound -> bound - 1).threshold());
    }

    @Test
    void everyValueInTheRangeCanBeDrawn() {
        var random = new Random(3);
        var drawn = new HashSet<Integer>();
        for (int i = 0; i < 5_000; i++) {
            drawn.add(Tally.fresh(RULES, random::nextInt).threshold());
        }
        assertEquals(51, drawn.size());
        assertEquals(50, drawn.stream().mapToInt(Integer::intValue).min().orElseThrow());
        assertEquals(100, drawn.stream().mapToInt(Integer::intValue).max().orElseThrow());
    }

    @Test
    void aRangeOfOneValueAlwaysDrawsIt() {
        assertEquals(70, Tally.fresh(range(70, 70), bound -> bound - 1).threshold());
    }

    @Test
    void anUpsideDownRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Tally.fresh(range(100, 50), bound -> 0));
    }

    @Test
    void theThresholdSetIsAtLeast1() {
        var tally = tally(0, 80);
        tally.setThreshold(0);
        assertEquals(1, tally.threshold());
        tally.setThreshold(87);
        assertEquals(87, tally.threshold());
    }

    // Wickedness set and added

    @Test
    void wickednessSetIsNeverBelow0() {
        var tally = tally(20, 80);
        tally.set(-5);
        assertEquals(0, tally.wickedness());
        tally.set(17);
        assertEquals(17, tally.wickedness());
    }

    @Test
    void wickednessAddedIsNeverBelow0NorOverflows() {
        var tally = tally(20, 80);
        tally.add(3);
        assertEquals(23, tally.wickedness());
        tally.add(-30);
        assertEquals(0, tally.wickedness());
        tally.set(Integer.MAX_VALUE - 1);
        tally.add(5);
        assertEquals(Integer.MAX_VALUE, tally.wickedness());
    }

    // The Bailiff

    @Test
    void belowTheThresholdNoBailiffIsDue() {
        assertFalse(tally(79, 80).arrivalDue(0, RULES));
    }

    @Test
    void aPlayerNeverVisitedIsDueAtTheThreshold() {
        assertTrue(tally(80, 80).arrivalDue(0, RULES));
        assertTrue(tally(80, 80).arrivalDue(Long.MAX_VALUE, RULES));
        assertTrue(tally(500, 80).arrivalDue(0, RULES));
    }

    @Test
    void arrivingResetsWickednessAndDrawsANewThreshold() {
        var tally = tally(90, 80);
        tally.arrived(1_000, RULES, bound -> bound - 1);
        assertEquals(0, tally.wickedness());
        assertEquals(100, tally.threshold());
        assertEquals(1_000, tally.arrivedAt());
    }

    @Test
    void theTickOfTheArrivalIsWithinTheCooldown() {
        assertFalse(arrivedAt(1_000).arrivalDue(1_000, RULES));
    }

    @Test
    void theLastTickBeforeTheEndIsWithinTheCooldown() {
        assertFalse(arrivedAt(1_000).arrivalDue(6_999, RULES));
    }

    @Test
    void theCooldownIsOverAsItsTicksRunOut() {
        assertTrue(arrivedAt(1_000).arrivalDue(7_000, RULES));
        assertTrue(arrivedAt(1_000).arrivalDue(100_000, RULES));
    }

    @Test
    void noCooldownLetsTheNextComeAtOnce() {
        var rules = new Rules(50, 100, 0, 10, BANDS, WARNINGS, 2, 5);
        var tally = tally(0, 80);
        tally.arrived(1_000, rules, bound -> 0);
        tally.set(tally.threshold());
        assertTrue(tally.arrivalDue(1_000, rules));
    }

    @Test
    void aClockBehindTheArrivalCountsAsOver() {
        assertTrue(arrivedAt(1_000).arrivalDue(999, RULES));
    }

    @Test
    void aBailiffLeavingWithNothingLeavesItsPlayerOwing() {
        var under = tally(10, 80);
        under.owe();
        assertEquals(80, under.wickedness());
        var over = tally(90, 80);
        over.owe();
        assertEquals(90, over.wickedness());
    }

    // Atonement

    @Test
    void anAtonementTakesOffItsAmount() {
        var tally = tally(50, 80);
        assertTrue(tally.atone(3));
        assertEquals(47, tally.wickedness());
        assertTrue(tally.atone(40));
        assertEquals(7, tally.wickedness());
    }

    @Test
    void anAtonementStopsAt0() {
        var tally = tally(30, 80);
        assertTrue(tally.atone(40));
        assertEquals(0, tally.wickedness());
    }

    @Test
    void anAtonementThatLowersNothingIsNoAtonement() {
        assertFalse(tally(0, 80).atone(3));
        var tally = tally(30, 80);
        assertFalse(tally.atone(0));
        assertFalse(tally.atone(-5));
        assertEquals(30, tally.wickedness());
    }

    @Test
    void aGiftTakesOffItsAmountUntilTheCap() {
        var tally = tally(50, 80);
        for (int gift = 1; gift <= 5; gift++) {
            assertTrue(tally.gift(2 * DAY, RULES), "gift " + gift + " of the day");
        }
        assertFalse(tally.gift(2 * DAY + 100, RULES), "a sixth gift on the same day");
        assertEquals(40, tally.wickedness());
    }

    @Test
    void theCapStartsAfreshOnANewDay() {
        var tally = tally(50, 80);
        for (int gift = 1; gift <= 5; gift++) {
            tally.gift(2 * DAY + DAY - 1, RULES);
        }
        assertTrue(tally.gift(3 * DAY, RULES));
        assertEquals(1, tally.gifts());
        assertEquals(3, tally.giftDay());
    }

    @Test
    void theCapStartsAfreshWhenTheClockIsTurnedBack() {
        var tally = tally(50, 80);
        for (int gift = 1; gift <= 5; gift++) {
            tally.gift(2 * DAY, RULES);
        }
        assertTrue(tally.gift(DAY, RULES));
    }

    @Test
    void noCapCountsNoGift() {
        var tally = tally(50, 80);
        assertFalse(tally.gift(0, new Rules(50, 100, 6_000, 10, BANDS, WARNINGS, 2, 0)));
        assertEquals(50, tally.wickedness());
    }

    @Test
    void aGiftWithNothingToLowerStillCounts() {
        var tally = tally(0, 80);
        assertFalse(tally.gift(0, RULES));
        assertEquals(1, tally.gifts());
    }

    // Decay

    @Test
    void theFirstReadingOfTheClockOnlyStartsDecay() {
        var tally = tally(50, 80);
        tally.decay(10 * DAY, RULES);
        assertEquals(50, tally.wickedness());
    }

    @Test
    void aDayTakesOffTheDailyDecay() {
        assertEquals(40, decayed(50, DAY, RULES).wickedness());
    }

    @Test
    void aPointFallsEveryTenthOfADay() {
        var tally = decayed(50, 2_399, RULES);
        assertEquals(50, tally.wickedness());
        tally.decay(2_400, RULES);
        assertEquals(49, tally.wickedness());
    }

    @Test
    void aDayTickByTickTakesOffTheSameAsAtOnce() {
        var tally = tally(50, 80);
        for (long clock = 0; clock <= DAY; clock++) {
            tally.decay(clock, RULES);
        }
        assertEquals(40, tally.wickedness());
    }

    @Test
    void itStopsAt0WithNothingBanked() {
        assertEquals(0, decayed(3, DAY, RULES).wickedness());
        var tally = decayed(0, 2_399, RULES);
        tally.set(5);
        tally.decay(2_400, RULES);
        assertEquals(5, tally.wickedness());
    }

    @Test
    void noTimeOrTimeTurnedBackTakesOffNothing() {
        var tally = decayed(50, 1_000, RULES);
        tally.decay(1_000, RULES);
        tally.decay(0, RULES);
        assertEquals(50, tally.wickedness());
        // Counted again from where the clock was turned back to: these 1,400 ticks and the 1,000 before make a point.
        tally.decay(1_399, RULES);
        assertEquals(50, tally.wickedness());
        tally.decay(1_400, RULES);
        assertEquals(49, tally.wickedness());
    }

    @Test
    void noDailyDecayTakesOffNothing() {
        assertEquals(50, decayed(50, DAY, new Rules(50, 100, 6_000, 0, BANDS, WARNINGS, 2, 5)).wickedness());
    }

    @Test
    void aVeryLongTimeTakesOffEverything() {
        var rules = new Rules(50, 100, 6_000, 1_000_000, BANDS, WARNINGS, 2, 5);
        assertEquals(0, decayed(Integer.MAX_VALUE, Long.MAX_VALUE, rules).wickedness());
    }

    // The Standings

    @Test
    void nothingIsClean() {
        assertEquals(Standing.CLEAN, tally(0, 80).standing(RULES));
    }

    @Test
    void justBelowHalfIsClean() {
        assertEquals(Standing.CLEAN, tally(39, 80).standing(RULES));
    }

    @Test
    void halfIsMarked() {
        assertEquals(Standing.MARKED, tally(40, 80).standing(RULES));
    }

    @Test
    void threeQuartersIsOverdue() {
        assertEquals(Standing.OVERDUE, tally(60, 80).standing(RULES));
    }

    @Test
    void justBelowNinetyPercentIsOverdue() {
        assertEquals(Standing.OVERDUE, tally(71, 80).standing(RULES));
    }

    @Test
    void ninetyPercentIsForfeit() {
        assertEquals(Standing.FORFEIT, tally(72, 80).standing(RULES));
    }

    @Test
    void anOddThresholdRoundsNoBandEarly() {
        // Half of 75 is 37.5: 37 is still Clean, 38 is Marked.
        assertEquals(Standing.CLEAN, tally(37, 75).standing(RULES));
        assertEquals(Standing.MARKED, tally(38, 75).standing(RULES));
    }

    @Test
    void theBandsFollowTheRules() {
        var early = new Rules(50, 100, 6_000, 10, new Standing.Bands(0.1, 0.2, 0.3), WARNINGS, 2, 5);
        assertEquals(Standing.FORFEIT, tally(30, 100).standing(early));
    }

    // The Warnings

    @Test
    void cleanNeverWarnsNorChanges() {
        assertEquals(new Look(Standing.CLEAN, false, false), tally(0, 100).look(100_000, RULES));
    }

    @Test
    void joiningMarkedOrWorseWarnsAndChangesAtOnce() {
        assertEquals(new Look(Standing.MARKED, true, true), tally(60, 100).look(1_000, RULES));
    }

    @Test
    void enteringEachBandWarnsAtOnce() {
        var tally = tally(60, 100);
        tally.look(1_000, RULES);
        tally.set(80);
        assertEquals(new Look(Standing.OVERDUE, true, true), tally.look(1_001, RULES));
        tally.set(95);
        assertEquals(new Look(Standing.FORFEIT, true, true), tally.look(1_002, RULES));
    }

    @Test
    void leapingPastABandWarnsOfTheOneEntered() {
        var tally = tally(0, 100);
        tally.look(1_000, RULES);
        tally.set(95);
        assertEquals(new Look(Standing.FORFEIT, true, true), tally.look(1_001, RULES));
    }

    @Test
    void stayingInABandWaitsOutItsInterval() {
        assertStays(60, 2_400);
        assertStays(80, 1_200);
        assertStays(95, 600);
    }

    @Test
    void fallingIntoALowerBandWaitsOutThatBandsInterval() {
        var tally = tally(95, 100);
        tally.look(1_000, RULES);
        tally.set(80);
        assertEquals(new Look(Standing.OVERDUE, true, false), tally.look(1_600, RULES));
        assertEquals(new Look(Standing.OVERDUE, false, true), tally.look(2_200, RULES));
    }

    @Test
    void fallingToCleanChangesWithNoWarning() {
        var tally = tally(60, 100);
        tally.look(1_000, RULES);
        tally.set(10);
        assertEquals(new Look(Standing.CLEAN, true, false), tally.look(100_000, RULES));
    }

    @Test
    void aClockBehindTheLastWarningWarns() {
        var tally = tally(60, 100);
        tally.look(1_000, RULES);
        assertTrue(tally.look(999, RULES).warns());
    }

    /** Asserts that a player staying at {@code wickedness} of 100 hears its Warning once each {@code interval}. */
    private static void assertStays(int wickedness, long interval) {
        var tally = tally(wickedness, 100);
        assertTrue(tally.look(1_000, RULES).warns());
        assertEquals(new Look(tally.standing(RULES), false, false), tally.look(1_000 + interval - 1, RULES));
        assertTrue(tally.look(1_000 + interval, RULES).warns());
    }

    private static Tally tally(int wickedness, int threshold) {
        var tally = Tally.fresh(RULES, bound -> 0);
        tally.setThreshold(threshold);
        tally.set(wickedness);
        return tally;
    }

    /** @return a Tally whose last Bailiff arrived at {@code now}, back at its new Threshold */
    private static Tally arrivedAt(long now) {
        var tally = tally(80, 80);
        tally.arrived(now, RULES, bound -> 0);
        tally.set(tally.threshold());
        return tally;
    }

    /** @return a Tally of {@code wickedness} once Decay read the clock at 0 and then at {@code clock} */
    private static Tally decayed(int wickedness, long clock, Rules rules) {
        var tally = tally(wickedness, Integer.MAX_VALUE);
        tally.decay(0, rules);
        tally.decay(clock, rules);
        return tally;
    }

    private static Rules range(int min, int max) {
        return new Rules(min, max, 6_000, 10, BANDS, WARNINGS, 2, 5);
    }
}

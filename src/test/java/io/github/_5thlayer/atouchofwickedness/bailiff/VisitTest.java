// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github._5thlayer.atouchofwickedness.bailiff.Visit.Outcome;
import io.github._5thlayer.atouchofwickedness.bailiff.Visit.Phase;
import io.github._5thlayer.atouchofwickedness.bailiff.Visit.Rules;
import org.junit.jupiter.api.Test;

/**
 * A Visit by the rules: it sleeps through the night, its time on site counting from its waking; it
 * robs, mugs or taunts as it arrives, and mugs or taunts once it has robbed every container in vain;
 * it goes to taunt with its haul or after its hit on its player, and flees once it has taunted, once
 * struck by its player as it robs or taunts, or once its time on site is up; and fleeing, it charges
 * its teleport once far enough or after running long enough, then vanishes, leaving its player owing
 * with no haul.
 */
class VisitTest {

    private static final Rules RULES = new Rules(1_200, 24, 40);

    /** Near enough to its player to taunt them, and too near to charge its teleport. */
    private static final double NEAR = 2;

    /** Far from its player, though short of the vanishing distance. */
    private static final double FAR = 10;

    @Test
    void itRobsWhenItHasContainersToRob() {
        assertEquals(Phase.ROBBING, arrived(true, true).phase());
        assertEquals(Phase.ROBBING, arrived(true, false).phase());
    }

    @Test
    void withNoContainerItMugsAPlayerWithSomethingWorthTaking() {
        assertEquals(Phase.MUGGING, arrived(false, true).phase());
    }

    @Test
    void withNoContainerItTauntsAPlayerWithNothingWorthTaking() {
        assertEquals(Phase.TAUNTING, arrived(false, false).phase());
    }

    @Test
    void arrivingAtNightItSleepsUntilDawn() {
        var visit = new Visit();
        visit.arrived(RULES, true, true, true);
        assertEquals(Phase.ASLEEP, visit.phase());
        assertFalse(visit.wantsLoose());
        // However long the night, it neither works nor runs out of time.
        ticks(visit, 5_000, true, FAR);
        assertEquals(Phase.ASLEEP, visit.phase());
        visit.tick(false, FAR, true, false);
        assertEquals(Phase.ROBBING, visit.phase());
    }

    @Test
    void itsTimeOnSiteStartsAsItWakesAtDawn() {
        var visit = new Visit();
        visit.arrived(RULES, false, true, true);
        ticks(visit, 500, true, FAR);
        visit.tick(false, FAR, true, false);
        ticks(visit, RULES.ticksOnSite() - 1, false, FAR);
        assertEquals(Phase.MUGGING, visit.phase());
        visit.tick(false, FAR, true, false);
        assertEquals(Phase.FLEEING, visit.phase());
    }

    @Test
    void itsTimeOnSiteStartsAsTheFirstHitWakesIt() {
        var visit = new Visit();
        visit.arrived(RULES, false, true, true);
        ticks(visit, 500, true, FAR);
        visit.struck(false);
        ticks(visit, RULES.ticksOnSite() - 1, true, FAR);
        assertEquals(Phase.MUGGING, visit.phase());
        visit.tick(true, FAR, true, false);
        assertEquals(Phase.FLEEING, visit.phase());
    }

    @Test
    void asleepABlowDealsDoubleAndWakesItWithoutSendingItFleeing() {
        var visit = new Visit();
        visit.arrived(RULES, true, true, true);
        assertEquals(6, visit.damage(3));
        // Even its own player's blow wakes it to get to work.
        visit.struck(true);
        assertEquals(Phase.ROBBING, visit.phase());
        assertEquals(3, visit.damage(3));
    }

    @Test
    void itFleesWhateverItHasOnceItsTimeOnSiteIsUp() {
        var visit = arrived(false, true);
        ticks(visit, RULES.ticksOnSite() - 1, false, FAR);
        assertEquals(Phase.MUGGING, visit.phase());
        visit.tick(false, FAR, true, false);
        assertEquals(Phase.FLEEING, visit.phase());
    }

    @Test
    void struckByItsPlayerAsItRobsOrTauntsItFlees() {
        var robbing = arrived(true, true);
        robbing.struck(true);
        assertEquals(Phase.FLEEING, robbing.phase());

        var taunting = arrived(false, false);
        taunting.struck(true);
        assertEquals(Phase.FLEEING, taunting.phase());
    }

    @Test
    void struckByAnotherOrAsItMugsItCarriesOn() {
        var robbing = arrived(true, true);
        robbing.struck(false);
        assertEquals(Phase.ROBBING, robbing.phase());

        var mugging = arrived(false, true);
        mugging.struck(true);
        assertEquals(Phase.MUGGING, mugging.phase());
    }

    @Test
    void itsHaulSendsItToTaunt() {
        var visit = arrived(true, true);
        visit.took();
        assertEquals(Phase.TAUNTING, visit.phase());
        assertFalse(visit.wantsLoose());
        assertFalse(visit.leavesOwing());
    }

    @Test
    void itsHitThatLandsSendsItToTauntHaulOrNot() {
        var visit = arrived(false, true);
        visit.hitLanded();
        assertEquals(Phase.TAUNTING, visit.phase());
        assertTrue(visit.leavesOwing());
    }

    @Test
    void robbingWithNoContainerLeftItMugsAPlayerWithSomethingWorthTaking() {
        var visit = arrived(true, true);
        visit.containersRanOut(true);
        assertEquals(Phase.MUGGING, visit.phase());
    }

    @Test
    void robbingWithNoContainerLeftItTauntsAPlayerWithNothingWorthTaking() {
        var visit = arrived(true, true);
        visit.containersRanOut(false);
        assertEquals(Phase.TAUNTING, visit.phase());
        assertTrue(visit.leavesOwing());
    }

    @Test
    void nearItsPlayerItShakesItsHeadAtOnceThenFlees() {
        var visit = arrived(false, false);
        assertEquals(Outcome.TAUNTS, visit.tick(false, NEAR, true, false));
        assertTrue(visit.isShaking());
        ticks(visit, Visit.TICKS_TAUNTING - 1, false, NEAR);
        assertEquals(Phase.TAUNTING, visit.phase());
        visit.tick(false, NEAR, true, false);
        assertEquals(Phase.FLEEING, visit.phase());
        assertFalse(visit.isShaking());
    }

    @Test
    void farFromItsPlayerItTauntsOnceItHasTriedLongEnoughToGetNear() {
        var visit = arrived(false, false);
        for (int tick = 0; tick < Visit.TICKS_TO_REACH_ITS_PLAYER; tick++) {
            assertEquals(Outcome.NONE, visit.tick(false, FAR, true, false));
        }
        assertFalse(visit.isShaking());
        assertEquals(Outcome.TAUNTS, visit.tick(false, FAR, true, false));
    }

    @Test
    void aDeadPlayerItTauntsOnceTheyAreBack() {
        var visit = arrived(false, false);
        for (int tick = 0; tick < Visit.TICKS_TO_REACH_ITS_PLAYER * 2; tick++) {
            assertEquals(Outcome.NONE, visit.tick(false, NEAR, false, false));
        }
        assertEquals(Outcome.TAUNTS, visit.tick(false, NEAR, true, false));
    }

    @Test
    void goingForAnItemOnTheGroundItStartsItsTauntOverOnceDone() {
        var visit = arrived(false, false);
        visit.tick(false, NEAR, true, false);
        ticks(visit, Visit.TICKS_TAUNTING * 2, true, NEAR, true);
        assertFalse(visit.isShaking());
        assertEquals(Phase.TAUNTING, visit.phase());
        assertEquals(Outcome.TAUNTS, visit.tick(false, NEAR, true, false));
    }

    @Test
    void anItemPickedUpAsItTauntsEmptyHandedIsItsHaulAndItTauntsAfresh() {
        var visit = arrived(false, false);
        visit.tick(false, NEAR, true, false);
        visit.took();
        assertFalse(visit.isShaking());
        assertFalse(visit.leavesOwing());
        assertEquals(Outcome.TAUNTS, visit.tick(false, NEAR, true, false));
    }

    @Test
    void fleeingFarEnoughItChargesItsTeleportThenVanishes() {
        var visit = fleeing();
        assertFalse(visit.isCharging());
        visit.tick(false, RULES.vanishDistance(), true, false);
        assertTrue(visit.isCharging());
        for (int tick = 2; tick < RULES.ticksToTeleport(); tick++) {
            // Charging, it stays wherever its player comes to.
            assertEquals(Outcome.NONE, visit.tick(false, NEAR, true, false));
        }
        assertEquals(Outcome.VANISHES, visit.tick(false, NEAR, true, false));
    }

    @Test
    void fleeingLongEnoughItChargesWhereverItGotTo() {
        var visit = fleeing();
        ticks(visit, Visit.TICKS_TO_GET_AWAY - 1, false, FAR);
        assertFalse(visit.isCharging());
        visit.tick(false, FAR, true, false);
        assertTrue(visit.isCharging());
    }

    @Test
    void withNoChargingTimeItVanishesAsItStartsCharging() {
        var visit = new Visit();
        visit.arrived(new Rules(1_200, 24, 0), false, true, false);
        visit.struck(true);
        visit.hitLanded();
        visit.struck(true);
        assertEquals(Phase.FLEEING, visit.phase());
        assertEquals(Outcome.VANISHES, visit.tick(false, 24, true, false));
        assertTrue(visit.isCharging());
    }

    @Test
    void itLeavesItsPlayerOwingOnlyWithNoHaul() {
        assertTrue(arrived(true, true).leavesOwing());
        var robbed = arrived(true, true);
        robbed.took();
        assertFalse(robbed.leavesOwing());
    }

    @Test
    void itPicksUpItemsOnlyAwakeUnfledAndEmptyHanded() {
        assertTrue(arrived(true, true).wantsLoose());
        assertFalse(fleeing().wantsLoose());
    }

    private static Visit arrived(boolean robs, boolean hasTakeable) {
        var visit = new Visit();
        visit.arrived(RULES, robs, hasTakeable, false);
        return visit;
    }

    /** A Visit fleeing empty-handed, its player having struck it as it taunted. */
    private static Visit fleeing() {
        var visit = arrived(false, false);
        visit.struck(true);
        return visit;
    }

    private static void ticks(Visit visit, int ticks, boolean night, double distance) {
        ticks(visit, ticks, night, distance, false);
    }

    private static void ticks(Visit visit, int ticks, boolean night, double distance, boolean gathering) {
        for (int tick = 0; tick < ticks; tick++) {
            visit.tick(night, distance, true, gathering);
        }
    }
}

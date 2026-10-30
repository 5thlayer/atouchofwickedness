// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

/**
 * A Bailiff's Visit, as the rules have it, apart from the world it walks in: what it is about
 * (asleep, robbing, mugging, taunting or fleeing), how long it has been on site, its taunt, its
 * flight and the teleport it charges, and whether it has its haul, so whether the Visit leaves its
 * player owing as it ends. Its {@link Bailiff} tells it what happens, one call per thing, and does
 * what it says.
 * <p>
 * Arriving at night, it sleeps until dawn, and its time on site starts as it wakes, or as the first
 * hit on it, by anyone, wakes it. Awake, it robs the containers its player opened, or with none mugs
 * its player, or with nothing of theirs worth taking taunts them at once. Its haul, a stack taken from
 * a container, its player or the ground, sends it to taunt; so does its first hit on its player that
 * lands, haul or not. Robbing with no container left and nothing taken, it mugs. Taunting, it walks up
 * to its player, while they are alive and while it isn't picking something up, until it is near enough
 * or has tried long enough, shakes its head at them, and then flees. Hit by its player as it robs or
 * taunts, it flees at once, and once its time on site is up it flees whatever it has. Fleeing, once
 * far enough from its player or after running long enough, it charges its teleport, and once charged
 * it vanishes.
 */
final class Visit {

    /** Ticks it taunts its player for before it flees. */
    static final int TICKS_TAUNTING = 40;

    /** Ticks it tries to get near its player to taunt them before it taunts from where it is. */
    static final int TICKS_TO_REACH = 200;

    /** How near, in blocks, it comes to its player to taunt them. */
    static final double TAUNT_DISTANCE = 3;

    /** Ticks it runs for before it charges its teleport wherever it got to, as when cornered. */
    static final int TICKS_TO_GET_AWAY = 400;

    /**
     * The config's numbers for a Visit, read as the Bailiff arrives.
     *
     * @param ticksOnSite ticks awake on site before it flees whatever it has
     * @param vanishDistance how far, in blocks, it flees from its player before it charges its teleport
     * @param ticksToTeleport ticks it stands charging its teleport before it vanishes
     */
    record Rules(int ticksOnSite, double vanishDistance, int ticksToTeleport) {
    }

    /** What the Bailiff is about, which its goals read. */
    enum Phase {
        ASLEEP, ROBBING, MUGGING, TAUNTING, FLEEING
    }

    /** What the Bailiff does once, as a tick of its Visit has it. */
    enum Outcome {
        /** Nothing but what its phase and charging have it do. */
        NONE,
        /** It shakes its head at its player, with its sound and its message. */
        TAUNTS,
        /** It vanishes, with what it carries, ending the Visit. */
        VANISHES
    }

    private Rules rules = new Rules(0, 0, 0);
    /** What it is about when awake. */
    private Phase phase = Phase.MUGGING;
    private boolean asleep;
    private boolean haul;
    private int ticksOnSite;
    /** Ticks taunting, or trying to get near enough to, since the taunt started. */
    private int ticksTaunting;
    private boolean shaking;
    private int ticksFleeing;
    /** Ticks it has stood charging its teleport, or -1 before it starts. */
    private int ticksCharging = -1;

    /**
     * The Bailiff arrives: to rob the containers its player opened when it {@code robs}, else to mug
     * its player when they have something worth taking, else to taunt them; at {@code night}, asleep.
     */
    void arrived(Rules rules, boolean robs, boolean hasTakeable, boolean night) {
        this.rules = rules;
        phase = robs ? Phase.ROBBING : hasTakeable ? Phase.MUGGING : Phase.TAUNTING;
        asleep = night;
        ticksOnSite = 0;
    }

    /**
     * A tick of the Visit while its player is in the Bailiff's level, dead or alive.
     *
     * @param night whether it is night where the Bailiff stands
     * @param distance how far, in blocks, it stands from its player
     * @param playerAlive whether its player is alive
     * @param gathering whether it is going for an item on the ground
     * @return what it does once this tick
     */
    Outcome tick(boolean night, double distance, boolean playerAlive, boolean gathering) {
        if (asleep) {
            if (!night) {
                wake();
            }
            return Outcome.NONE;
        }
        if (phase != Phase.FLEEING) {
            var outcome = phase == Phase.TAUNTING ? taunt(distance, playerAlive, gathering) : Outcome.NONE;
            if (phase != Phase.FLEEING && ++ticksOnSite >= rules.ticksOnSite()) {
                flee();
            }
            return outcome;
        }
        if (!isCharging() && (distance >= rules.vanishDistance() || ++ticksFleeing >= TICKS_TO_GET_AWAY)) {
            ticksCharging = 0;
        }
        if (isCharging() && ++ticksCharging >= rules.ticksToTeleport()) {
            return Outcome.VANISHES;
        }
        return Outcome.NONE;
    }

    private Outcome taunt(double distance, boolean playerAlive, boolean gathering) {
        if (gathering) {
            // Picking something up, it starts its taunt over once it is done.
            startTaunt();
            return Outcome.NONE;
        }
        if (!playerAlive) {
            // A dead player it taunts once they are back.
            return Outcome.NONE;
        }
        ticksTaunting++;
        if (shaking) {
            if (ticksTaunting >= TICKS_TAUNTING) {
                flee();
            }
            return Outcome.NONE;
        }
        if (distance > TAUNT_DISTANCE && ticksTaunting <= TICKS_TO_REACH) {
            return Outcome.NONE;
        }
        shaking = true;
        ticksTaunting = 0;
        return Outcome.TAUNTS;
    }

    /**
     * The Bailiff is struck by someone, and hurt: asleep, it wakes, and its time on site starts; robbing
     * or taunting, struck {@code byItsPlayer}, it flees.
     */
    void struck(boolean byItsPlayer) {
        if (asleep) {
            wake();
            return;
        }
        if (byItsPlayer && (phase == Phase.ROBBING || phase == Phase.TAUNTING)) {
            flee();
        }
    }

    /** @return the damage a blow of {@code damage} from someone deals it: double, asleep */
    float damage(float damage) {
        return asleep ? damage * 2 : damage;
    }

    /** Its hit on its player landed: mugging, it has had its go, whether or not it took a stack. */
    void hitLanded() {
        if (phase == Phase.MUGGING) {
            toTaunt();
        }
    }

    /** It took a stack, its haul, and goes to taunt its player. */
    void took() {
        haul = true;
        toTaunt();
    }

    /** Robbing, it has no container left to try: with nothing taken, it mugs. */
    void containersRanOut() {
        if (phase == Phase.ROBBING) {
            phase = Phase.MUGGING;
        }
    }

    /** @return what it is about */
    Phase phase() {
        return asleep ? Phase.ASLEEP : phase;
    }

    /** @return whether it is shaking its head at its player */
    boolean isShaking() {
        return phase == Phase.TAUNTING && shaking;
    }

    /** @return whether it stands charging its teleport */
    boolean isCharging() {
        return ticksCharging >= 0;
    }

    /** @return whether it would pick up an item on the ground: awake, not fleeing, and with no haul yet */
    boolean wantsLoose() {
        return !haul && !asleep && phase != Phase.FLEEING;
    }

    /** @return whether its player is left owing as the Visit ends, however it ends: with no haul */
    boolean leavesOwing() {
        return !haul;
    }

    private void wake() {
        asleep = false;
        ticksOnSite = 0;
    }

    private void toTaunt() {
        phase = Phase.TAUNTING;
        startTaunt();
    }

    private void startTaunt() {
        ticksTaunting = 0;
        shaking = false;
    }

    private void flee() {
        phase = Phase.FLEEING;
        shaking = false;
    }
}

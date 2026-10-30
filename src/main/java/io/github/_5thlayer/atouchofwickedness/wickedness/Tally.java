// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.function.IntUnaryOperator;

/**
 * A player's Tally, as the rules have it, apart from the world: their Wickedness and Threshold, the
 * Decay carried towards the next point, the gifts that counted on their latest day of giving and when
 * their last Bailiff arrived, all saved with the player; and, only while they are online, the
 * Standing last seen, the last Warning and the clock Decay last read. {@link Wickedness} tells it what
 * happens, one call per thing, and does what it says. Plain Java, so JUnit tests it.
 * <p>
 * Wickedness never falls below 0, and the Threshold, drawn uniformly from the rules' range when the
 * player first joins and again as each Bailiff arrives, never below 1. A Bailiff is due once
 * Wickedness is at the Threshold and the cooldown after the last arrival is over; arriving, it resets
 * Wickedness to 0, and one that leaves with nothing leaves the player owing, back up to their
 * Threshold at the least. An Atonement lowers Wickedness at once by its amount, and only so many gifts
 * count each in-game day. Decay lowers it by a set amount each in-game day, a whole point at a time as
 * the day passes, and stops at 0 with nothing banked, so time spent clean never pays for later kills.
 * A Warning sounds as the player enters Marked, Overdue or Forfeit, and again each time their band's
 * interval passes while they stay in it; a player falling into a lower band waits out that band's
 * interval, and a Clean player hears none. A clock behind the time it counts from, as in a world
 * restored from a backup, counts as the wait over.
 */
final class Tally {

    /** No Bailiff has arrived for the player yet, no Warning has sounded, or Decay has read no clock. */
    static final long NEVER = Long.MIN_VALUE;

    /** The ticks of an in-game day, on the world's clock. */
    static final long TICKS_PER_DAY = 24_000;

    /** Longer than any world runs, and short enough that its Decay cannot overflow. */
    private static final long LONGEST = Integer.MAX_VALUE;

    /**
     * The config's numbers for a Tally.
     *
     * @param thresholdMin       the lowest Threshold drawn
     * @param thresholdMax       the highest Threshold drawn
     * @param ticksBetweenVisits the cooldown after a Bailiff arrives before another may come
     * @param decayPerDay        how far Decay lowers Wickedness each in-game day
     * @param bands              where each Standing starts
     * @param warnings           each band's Warnings
     * @param gift               what a gift lowers Wickedness by
     * @param giftsPerDay        how many gifts count each in-game day
     */
    record Rules(int thresholdMin, int thresholdMax, long ticksBetweenVisits, int decayPerDay,
            Standing.Bands bands, Warning.Rules warnings, int gift, int giftsPerDay) {
    }

    /**
     * The player's Standing, as a look at it finds it.
     *
     * @param standing their Standing now
     * @param changed  whether it differs from the one last seen, the one a player joins with included
     * @param warns    whether a Warning sounds for them now
     */
    record Look(Standing standing, boolean changed, boolean warns) {
    }

    private int wickedness;
    private int threshold;
    /** The Decay owed short of a whole point, in points times ticks per day. */
    private long decayCarry;
    /** How many gifts counted on {@link #giftDay}. */
    private int gifts;
    /** The latest day of giving, in whole in-game days on the world's clock. */
    private long giftDay;
    /** The game time the player's last Bailiff arrived, or {@link #NEVER}. */
    private long arrivedAt;
    /** The world's clock when Decay last read it, never saved, so no Decay accrues while the player is away. */
    private long decayedAt = NEVER;
    /** The player's Standing when last seen, never saved, so a player who joins Marked or worse hears a Warning at once. */
    private Standing seen = Standing.CLEAN;
    /** The game time of the player's last Warning, never saved, or {@link #NEVER}. */
    private long warnedAt = NEVER;

    /** A Tally as it was saved. */
    Tally(int wickedness, int threshold, long decayCarry, int gifts, long giftDay, long arrivedAt) {
        this.wickedness = wickedness;
        this.threshold = threshold;
        this.decayCarry = decayCarry;
        this.gifts = gifts;
        this.giftDay = giftDay;
        this.arrivedAt = arrivedAt;
    }

    /**
     * @param random a random int from 0 up to, not including, its argument, such as {@code RandomSource::nextInt}
     * @return a new player's Tally: no Wickedness, against a Threshold drawn from the rules' range
     */
    static Tally fresh(Rules rules, IntUnaryOperator random) {
        return new Tally(0, draw(rules, random), 0, 0, 0, NEVER);
    }

    int wickedness() {
        return wickedness;
    }

    int threshold() {
        return threshold;
    }

    long decayCarry() {
        return decayCarry;
    }

    int gifts() {
        return gifts;
    }

    long giftDay() {
        return giftDay;
    }

    long arrivedAt() {
        return arrivedAt;
    }

    /** @return the player's Standing, the rough band of their Wickedness against their Threshold */
    Standing standing(Rules rules) {
        var bands = rules.bands();
        if (reaches(bands.forfeit())) {
            return Standing.FORFEIT;
        }
        if (reaches(bands.overdue())) {
            return Standing.OVERDUE;
        }
        if (reaches(bands.marked())) {
            return Standing.MARKED;
        }
        return Standing.CLEAN;
    }

    /** Sets Wickedness to {@code value}, never below 0. */
    void set(int value) {
        wickedness = Math.max(0, value);
    }

    /** Adds {@code amount}, which may be negative, to Wickedness, never below 0. */
    void add(int amount) {
        wickedness = (int) Math.clamp((long) wickedness + amount, 0, Integer.MAX_VALUE);
    }

    /** Sets the Threshold, at least 1, until the next arrival draws another. */
    void setThreshold(int value) {
        threshold = Math.max(1, value);
    }

    /** @return whether a Bailiff is due at game time {@code now}: Wickedness at the Threshold, and the cooldown over */
    boolean arrivalDue(long now, Rules rules) {
        return wickedness >= threshold
                && (arrivedAt == NEVER || now < arrivedAt || now - arrivedAt >= rules.ticksBetweenVisits());
    }

    /** A Bailiff arrived at game time {@code now}: Wickedness resets to 0, a new Threshold is drawn and the cooldown starts. */
    void arrived(long now, Rules rules, IntUnaryOperator random) {
        wickedness = 0;
        threshold = draw(rules, random);
        arrivedAt = now;
    }

    /** A Bailiff left with nothing: Wickedness goes back up to the Threshold, at the least. */
    void owe() {
        wickedness = Math.max(wickedness, threshold);
    }

    /** Lowers Wickedness by an Atonement of {@code amount}, never below 0. @return whether it fell */
    boolean atone(int amount) {
        int before = wickedness;
        wickedness = Math.max(0, wickedness - Math.max(0, amount));
        return wickedness < before;
    }

    /**
     * Lowers Wickedness by a gift's Atonement, while the gifts on the day the world's {@code clock}
     * falls on are under the cap. @return whether it fell
     */
    boolean gift(long clock, Rules rules) {
        long today = Math.floorDiv(clock, TICKS_PER_DAY);
        int todays = today == giftDay ? gifts : 0;
        if (todays >= rules.giftsPerDay()) {
            return false;
        }
        gifts = todays + 1;
        giftDay = today;
        return atone(rules.gift());
    }

    /** Runs Decay over the world's {@code clock} ticks since it last ran, which the first reading only starts. */
    void decay(long clock, Rules rules) {
        long elapsed = decayedAt == NEVER ? 0 : clock - decayedAt;
        decayedAt = clock;
        if (elapsed <= 0 || rules.decayPerDay() <= 0) {
            return;
        }
        long owed = decayCarry + Math.min(elapsed, LONGEST) * rules.decayPerDay();
        long points = owed / TICKS_PER_DAY;
        if (points >= wickedness) {
            wickedness = 0;
            decayCarry = 0;
        } else {
            wickedness -= (int) points;
            decayCarry = owed % TICKS_PER_DAY;
        }
    }

    /** Looks at the player's Standing at game time {@code now}, as the one last seen from then on, starting their Warning's interval when one sounds. */
    Look look(long now, Rules rules) {
        var standing = standing(rules);
        boolean warns = warns(standing, now, rules.warnings());
        if (warns) {
            warnedAt = now;
        }
        var look = new Look(standing, standing != seen, warns);
        seen = standing;
        return look;
    }

    private boolean warns(Standing standing, long now, Warning.Rules rules) {
        if (standing == Standing.CLEAN) {
            return false;
        }
        return standing.compareTo(seen) > 0 || warnedAt == NEVER || now < warnedAt
                || now - warnedAt >= rules.of(standing).interval();
    }

    private boolean reaches(double fraction) {
        // A hair under, so 90% of 80, which doubles hold as 72.00000000000001, starts at 72.
        return wickedness >= fraction * threshold - 1e-9;
    }

    private static int draw(Rules rules, IntUnaryOperator random) {
        if (rules.thresholdMax() < rules.thresholdMin()) {
            throw new IllegalArgumentException("the Threshold's range runs from " + rules.thresholdMin() + " down to " + rules.thresholdMax());
        }
        return rules.thresholdMin() + random.applyAsInt(rules.thresholdMax() - rules.thresholdMin() + 1);
    }
}

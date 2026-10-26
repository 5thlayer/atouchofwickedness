// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * Atonement: a kind act takes its amount off Wickedness at once, never below 0. Feeding a hurt pet
 * takes off an amount per whole heart it regains, and only so many gifts to villagers count each
 * in-game day. Plain Java, so JUnit tests it; {@link KindActs} credits the acts.
 */
public final class Atonement {

    private Atonement() {
    }

    /** @return {@code wickedness} once an Atonement of {@code amount} is taken off, never below 0 */
    public static int lowered(int wickedness, int amount) {
        return Math.max(0, wickedness - Math.max(0, amount));
    }

    /** @return what feeding a pet that regains {@code regained} health takes off, at {@code perHeart} a whole heart */
    public static int feeding(float regained, int perHeart) {
        int hearts = (int) Math.floor(regained / 2);
        return Math.max(0, hearts) * perHeart;
    }

    /**
     * The gifts of a player's that counted on their latest day of giving.
     *
     * @param count how many counted that day
     * @param day   the day, in whole in-game days on the world's clock
     */
    public record Gifts(int count, long day) {

        public static final Gifts NONE = new Gifts(0, 0);

        /** @return the in-game day the clock's {@code ticks} fall on */
        public static long dayOf(long ticks) {
            return Math.floorDiv(ticks, Decay.TICKS_PER_DAY);
        }

        /** @return whether a gift on {@code today} counts, when {@code cap} count a day */
        public boolean counts(long today, int cap) {
            return (today == day ? count : 0) < cap;
        }

        /** @return these gifts once one more has counted on {@code today} */
        public Gifts plusOne(long today) {
            return today == day ? new Gifts(count + 1, day) : new Gifts(1, today);
        }
    }
}

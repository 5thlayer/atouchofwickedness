// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * Atonement: a kind act lowers Wickedness at once by its amount, never below 0. Healing a hurt pet
 * lowers it by an amount per whole heart the pet regains, and only so many gifts of food to villagers
 * count each in-game day. Plain Java, so JUnit tests it; {@link KindActs} credits the acts.
 */
public final class Atonement {

    private Atonement() {
    }

    /**
     * What each Atonement lowers Wickedness by.
     *
     * @param breeding        breeding two animals
     * @param taming          taming an animal
     * @param healingPerHeart healing a pet the player owns, for each whole heart it regains
     * @param curing          curing a zombie villager
     * @param gift            a villager picking up food the player threw
     * @param giftsPerDay     how many gifts count each in-game day
     */
    public record Rules(int breeding, int taming, int healingPerHeart, int curing, int gift, int giftsPerDay) {
    }

    /** @return {@code wickedness} lowered by an Atonement of {@code amount}, never below 0 */
    public static int lowered(int wickedness, int amount) {
        return Math.max(0, wickedness - amount);
    }

    /** @return what healing a pet by {@code regained} health lowers Wickedness by, at {@code perHeart} a whole heart */
    public static int healing(float regained, int perHeart) {
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

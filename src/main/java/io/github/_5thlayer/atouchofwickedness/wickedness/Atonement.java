// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * Atonement: a kind act lowers Wickedness at once by its amount, never below 0. Healing a hurt pet
 * lowers it by an amount per whole heart the pet regains, and only so many gifts of food to villagers
 * count each in-game day. Plain Java, so JUnit tests it; {@link KindActs} credits the acts, and the
 * {@link Tally} lowers Wickedness by them.
 */
public final class Atonement {

    private Atonement() {
    }

    /**
     * What each Atonement lowers Wickedness by, and how many gifts count a day.
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

    /** @return what healing a pet by {@code regained} health lowers Wickedness by, at {@code perHeart} a whole heart */
    public static int healing(float regained, int perHeart) {
        int hearts = (int) Math.floor(regained / 2);
        return Math.max(0, hearts) * perHeart;
    }
}

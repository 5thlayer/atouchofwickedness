// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import org.jspecify.annotations.Nullable;

/**
 * What a Wicked kill of a creature adds, and what a hit on one costs. A creature's base weight is its
 * entry in the {@code atouchofwickedness:wickedness_weight} data map ({@link WickedKills#WEIGHT}), and
 * nothing when it has none; the rules on top of it weigh what belongs to the creature rather than its
 * type. Plain Java, so JUnit tests it.
 */
public final class WickednessWeight {

    private WickednessWeight() {
    }

    /**
     * The numbers on top of the data map.
     *
     * @param tamedWeight     what any tamed creature weighs, whatever its type
     * @param babyMultiplier  how many times its weight a baby weighs
     * @param tormentWeight   the effective weight from which each damaging hit costs {@code hitCost}
     * @param hitCost         what each damaging hit on such a creature adds
     */
    public record Rules(int tamedWeight, double babyMultiplier, int tormentWeight, int hitCost) {
    }

    /**
     * What about a creature changes its weight.
     *
     * @param tamed           whether it is tamed, by anyone
     * @param baby            whether it is a baby
     * @param goingForKiller  whether it is a neutral mob whose target was the player it is weighed against
     */
    public record Victim(boolean tamed, boolean baby, boolean goingForKiller) {
    }

    /** @return the weight of a creature whose data-map entry is {@code entry}, null when it has none */
    public static int of(@Nullable Integer entry) {
        return entry == null ? 0 : entry;
    }

    /** @return what a creature of base weight {@code base} weighs, once the rules are applied to {@code victim} */
    public static int effective(int base, Victim victim, Rules rules) {
        if (victim.goingForKiller()) {
            return 0;
        }
        int weight = victim.tamed() ? rules.tamedWeight() : base;
        if (victim.baby()) {
            return (int) Math.min(Integer.MAX_VALUE, Math.round(weight * rules.babyMultiplier()));
        }
        return weight;
    }

    /** @return what a damaging hit on a creature of effective weight {@code effective} adds */
    public static int hitCost(int effective, Rules rules) {
        return effective > 0 && effective >= rules.tormentWeight() ? rules.hitCost() : 0;
    }
}

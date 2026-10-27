// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import org.jspecify.annotations.Nullable;

/**
 * What an item is worth to the Bailiff, and how it picks the one stack it takes by it. An item's
 * Value is its entry in the {@code atouchofwickedness:bailiff_value} data map ({@link ItemValues#VALUE}),
 * or the unlisted Value when it has none; each level of its enchantments adds to it, unless it is
 * worth nothing, which is never taken. A stack is worth its items' Value times its count. Plain Java,
 * so JUnit tests it.
 */
public final class Value {

    private Value() {
    }

    /**
     * The numbers on top of the data map.
     *
     * @param unlisted            what an item the data map doesn't list is worth
     * @param perEnchantmentLevel what each level of an item's enchantments adds to it
     * @param looseFloor          the least an item lying on the ground must be worth, each, for the
     *                            Bailiff to go for it
     */
    public record Rules(int unlisted, int perEnchantmentLevel, int looseFloor) {
    }

    /** @return the Value of an item whose data-map entry is {@code entry}, null when it has none */
    public static int of(@Nullable Integer entry, Rules rules) {
        return entry == null ? rules.unlisted() : entry;
    }

    /**
     * @return what each item of a stack is worth: its Value, and the enchantment bonus for its
     *         {@code enchantmentLevels}, but nothing when its Value is nothing
     */
    public static long perItem(int value, int enchantmentLevels, Rules rules) {
        return value == 0 ? 0 : value + (long) enchantmentLevels * rules.perEnchantmentLevel();
    }

    /** @return what a stack of {@code count} items, each worth {@code perItem}, is worth */
    public static long ofStack(long perItem, int count) {
        return perItem * count;
    }

    /** @return whether an item lying on the ground, each worth {@code perItem}, is worth the Bailiff going for */
    public static boolean worthPickingUp(long perItem, Rules rules) {
        return perItem > 0 && perItem >= rules.looseFloor();
    }

    /**
     * Draws one of the stacks worth {@code values}, each as likely as its share of what they are all
     * worth together, so one worth nothing is never drawn.
     *
     * @param roll a number drawn evenly from 0 inclusive to 1 exclusive
     * @return the index of the stack drawn, or -1 when none is worth anything
     */
    public static int draw(long[] values, double roll) {
        long total = 0;
        for (long value : values) {
            // Held at the most a long holds, however a pack values its items.
            total = total > Long.MAX_VALUE - value ? Long.MAX_VALUE : total + value;
        }
        if (total <= 0) {
            return -1;
        }
        // Below the total however the roll rounds, so it always lands on a stack.
        long landing = Math.min(total - 1, (long) (roll * total));
        for (int index = 0; index < values.length; index++) {
            landing -= values[index];
            if (landing < 0) {
                return index;
            }
        }
        return -1;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * What an item is worth to the Bailiff, and which stack it seizes by it. An item's Value is its
 * entry in the {@code atouchofwickedness:bailiff_value} data map ({@link ItemValues#VALUE}), or the
 * unlisted Value when it has none; each level of its enchantments adds to it, unless it is worth
 * nothing, which is never taken. A stack is worth its items' Value times its count, and the one the
 * Bailiff takes is drawn by it: from a container, any stack; from its player, any in the main
 * inventory or hotbar but the held one. Plain Java, so JUnit tests it.
 */
public final class Value {

    private Value() {
    }

    /**
     * The numbers on top of the data map.
     *
     * @param unlisted            what an item the data map doesn't list is worth
     * @param perEnchantmentLevel what each level of an item's enchantments adds to it
     * @param pickupFloor         the least an item lying on the ground must be worth, each, for the
     *                            Bailiff to go for it
     */
    public record Rules(int unlisted, int perEnchantmentLevel, int pickupFloor) {
    }

    /**
     * What a stack shows the Bailiff, read by {@link ItemValues#stack}.
     *
     * @param listed            its item's entry in the data map, null when it has none
     * @param enchantmentLevels the levels of its enchantments, stored ones included
     * @param count             how many items it holds, none when it is empty
     */
    public record Stack(@Nullable Integer listed, int enchantmentLevels, int count) {
    }

    /**
     * Draws the stack the Bailiff takes from its player's main inventory and hotbar, never the held one.
     *
     * @param main the stacks of the main inventory and hotbar, by slot
     * @param held the selected slot
     * @param roll a number drawn evenly from 0 inclusive to 1 exclusive
     * @return the slot drawn, or -1 when none but the held one is worth anything
     */
    public static int fromPlayer(List<Stack> main, int held, Rules rules, double roll) {
        return draw(worth(main, held, rules), roll);
    }

    /**
     * Draws the stack the Bailiff takes from a container, any of its stacks.
     *
     * @param roll a number drawn evenly from 0 inclusive to 1 exclusive
     * @return the slot drawn, or -1 when none is worth anything
     */
    public static int fromContainer(List<Stack> stacks, Rules rules, double roll) {
        return draw(worth(stacks, -1, rules), roll);
    }

    /** @return whether {@link #fromPlayer} would find a stack to take in {@code main}, holding {@code held} */
    public static boolean playerHasTakeable(List<Stack> main, int held, Rules rules) {
        for (long value : worth(main, held, rules)) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    /** @return whether {@code stack}, lying on the ground, is worth the Bailiff going for: each of its items at the floor */
    public static boolean worthPickingUp(Stack stack, Rules rules) {
        long perItem = perItem(stack, rules);
        return stack.count() > 0 && perItem > 0 && perItem >= rules.pickupFloor();
    }

    /** @return what each item of {@code stack} is worth, its enchantments included, but nothing when its Value is nothing */
    private static long perItem(Stack stack, Rules rules) {
        int value = stack.listed() == null ? rules.unlisted() : stack.listed();
        return value == 0 ? 0 : value + (long) stack.enchantmentLevels() * rules.perEnchantmentLevel();
    }

    /** @return what each of {@code stacks} is worth, by slot, nothing for the {@code kept} one */
    private static long[] worth(List<Stack> stacks, int kept, Rules rules) {
        var worth = new long[stacks.size()];
        for (int slot = 0; slot < worth.length; slot++) {
            if (slot != kept) {
                var stack = stacks.get(slot);
                worth[slot] = perItem(stack, rules) * stack.count();
            }
        }
        return worth;
    }

    /**
     * Draws one of the stacks worth {@code values}, each as likely as its share of what they are all
     * worth together, so one worth nothing is never drawn.
     *
     * @return the index of the stack drawn, or -1 when none is worth anything
     */
    private static int draw(long[] values, double roll) {
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

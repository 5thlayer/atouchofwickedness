// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.function.IntUnaryOperator;

/** The hidden Wickedness at which the Bailiff comes, drawn anew after each visit. Plain Java, so JUnit tests it. */
public final class Threshold {

    private Threshold() {
    }

    /**
     * Draws a Threshold uniformly from {@code min} to {@code max}, both included.
     *
     * @param nextInt a random int from 0 up to, not including, its argument, such as {@code RandomSource::nextInt}
     */
    public static int draw(int min, int max, IntUnaryOperator nextInt) {
        if (max < min) {
            throw new IllegalArgumentException("the Threshold's range runs from " + min + " down to " + max);
        }
        return min + nextInt.applyAsInt(max - min + 1);
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

/**
 * Decay: Wickedness falls by a set amount each in-game day, a whole point at a time as the day
 * passes, and stops at 0 with nothing banked, so time spent clean never pays for later kills.
 * Plain Java, so JUnit tests it.
 */
public final class Decay {

    /** The ticks of an in-game day, on the world's clock. */
    public static final long TICKS_PER_DAY = 24_000;

    /** Longer than any world runs, and short enough that its Decay cannot overflow. */
    private static final long LONGEST = Integer.MAX_VALUE;

    private Decay() {
    }

    /**
     * Wickedness and the part of a point of Decay carried towards the next.
     *
     * @param wickedness the player's Wickedness
     * @param carry      the Decay owed short of a whole point, in points times ticks per day
     */
    public record State(int wickedness, long carry) {
    }

    /** @return {@code state} once {@code elapsed} ticks of the clock have passed, at {@code perDay} a day */
    public static State over(State state, long elapsed, int perDay) {
        if (elapsed <= 0 || perDay <= 0) {
            return state;
        }
        long owed = state.carry() + Math.min(elapsed, LONGEST) * perDay;
        long points = owed / TICKS_PER_DAY;
        if (points >= state.wickedness()) {
            return new State(0, 0);
        }
        return new State(state.wickedness() - (int) points, owed % TICKS_PER_DAY);
    }
}

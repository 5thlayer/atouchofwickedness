// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.ArrayList;
import java.util.List;

/**
 * The Seizable containers a player has opened, most recent first, at most a set length of them:
 * opening one again brings it to the front, and the oldest falls off the end. Plain Java, so JUnit
 * tests it.
 */
public record Opened<T>(List<T> recent) {

    public Opened {
        recent = List.copyOf(recent);
    }

    public static <T> Opened<T> none() {
        return new Opened<>(List.of());
    }

    /** @return these with {@code container} opened last, keeping at most {@code length}, at least 1 */
    public Opened<T> plus(T container, int length) {
        var next = new ArrayList<T>(recent.size() + 1);
        next.add(container);
        for (var other : recent) {
            if (next.size() >= length) {
                break;
            }
            if (!other.equals(container)) {
                next.add(other);
            }
        }
        return new Opened<>(next);
    }
}

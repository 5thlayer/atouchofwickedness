// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import org.jspecify.annotations.Nullable;

/**
 * What a Wicked kill of a creature adds: its entry in the {@code atouchofwickedness:wickedness_weight}
 * data map ({@link WickedKills#WEIGHT}), and nothing when it has none. Plain Java, so JUnit tests it.
 */
public final class WickednessWeight {

    private WickednessWeight() {
    }

    /** @return the weight of a creature whose data-map entry is {@code entry}, null when it has none */
    public static int of(@Nullable Integer entry) {
        return entry == null ? 0 : entry;
    }
}

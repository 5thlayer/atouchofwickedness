// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

/** How many stacks a Bailiff takes on a visit before it flees. Plain Java, so JUnit tests it. */
public final class Quota {

    private final int limit;
    private int taken;

    public Quota(int limit) {
        this.limit = limit;
    }

    /** Counts one more stack taken, if the quota has room for it. @return whether it had */
    public boolean take() {
        if (isMet()) {
            return false;
        }
        taken++;
        return true;
    }

    public int taken() {
        return taken;
    }

    public boolean isMet() {
        return taken >= limit;
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;

/**
 * The nights of the game tests: it is day for every Bailiff but those a test lets night fall on, so
 * no test reads or moves the world's clock, which every test running alongside it shares.
 */
final class Nights {

    /** The Bailiffs it is night for, read and written on the server thread alone. */
    private static final Set<Bailiff> AT_NIGHT = Collections.newSetFromMap(new WeakHashMap<>());

    private Nights() {
    }

    /** Has the Bailiffs read their nights here, in place of the day's timeline. */
    static void install() {
        Bailiff.tellNightBy(AT_NIGHT::contains);
    }

    /** Night falls where {@code bailiff} stands. */
    static void fall(Bailiff bailiff) {
        AT_NIGHT.add(bailiff);
    }

    /** Dawn breaks where {@code bailiff} stands. */
    static void dawn(Bailiff bailiff) {
        AT_NIGHT.remove(bailiff);
    }
}

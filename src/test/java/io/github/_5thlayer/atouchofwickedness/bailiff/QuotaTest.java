// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A Bailiff takes stacks up to its quota, and is done once it has. */
class QuotaTest {

    @Test
    void aNewQuotaIsNotMet() {
        assertFalse(new Quota(5).isMet());
    }

    @Test
    void fourStacksOfFiveIsNotMet() {
        var quota = new Quota(5);
        for (int i = 0; i < 4; i++) {
            quota.take();
        }
        assertFalse(quota.isMet());
        assertEquals(4, quota.taken());
    }

    @Test
    void theFifthStackMeetsIt() {
        var quota = new Quota(5);
        for (int i = 0; i < 5; i++) {
            quota.take();
        }
        assertTrue(quota.isMet());
    }

    @Test
    void aMetQuotaTakesNoMore() {
        var quota = new Quota(1);
        assertTrue(quota.take());
        assertFalse(quota.take());
        assertEquals(1, quota.taken());
    }
}

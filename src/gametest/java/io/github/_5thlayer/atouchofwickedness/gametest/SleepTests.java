// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Bailiff reads the night as it arrives and as it ticks, falling asleep where it arrived and
 * waking at dawn, and a hit on it asleep deals double damage and wakes it. What it does asleep and
 * awake is its Visit's, pinned in {@code VisitTest}. Night falls and dawn breaks for each test's
 * Bailiff alone ({@link Nights}), never on the world's clock.
 */
final class SleepTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);

    private SleepTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("sleep_asleep_at_night_until_dawn", 20, SleepTests::untilDawn);
        tests.test("sleep_the_first_hit_deals_double_and_wakes_it", 20, SleepTests::firstHit);
    }

    private static void untilDawn(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = comeForAtNight(helper, player);
        assertAsleep(helper, bailiff);
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    assertAsleep(helper, bailiff);
                    Nights.dawn(bailiff);
                })
                // Its next tick, it wakes.
                .thenIdle(2)
                .thenExecute(() -> {
                    if (bailiff.isAsleep()) {
                        throw helper.assertionException("the Bailiff slept on past dawn");
                    }
                })
                .thenSucceed();
    }

    private static void firstHit(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = comeForAtNight(helper, player);
        float full = bailiff.getHealth();
        hit(helper, bailiff, player, 3);
        assertTaken(helper, bailiff, full - 6, "the first hit, of 3, on the sleeping Bailiff");
        if (bailiff.isAsleep()) {
            throw helper.assertionException("the first hit did not wake the Bailiff");
        }
        // Awake, it takes the next hit as it comes.
        bailiff.invulnerableTime = 0;
        hit(helper, bailiff, player, 3);
        assertTaken(helper, bailiff, full - 9, "a second hit, of 3, on the woken Bailiff");
        helper.succeed();
    }

    /** A Bailiff at {@link #BAILIFF}, arrived at night for {@code player}. */
    private static Bailiff comeForAtNight(GameTestHelper helper, ServerPlayer player) {
        var bailiff = helper.spawn(BailiffContent.BAILIFF.get(), BAILIFF);
        Nights.fall(bailiff);
        bailiff.comeFor(player);
        return bailiff;
    }

    /** The player hits the Bailiff for {@code damage}, as a melee blow would. */
    private static void hit(GameTestHelper helper, Bailiff bailiff, ServerPlayer player, float damage) {
        if (!bailiff.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), damage)) {
            throw helper.assertionException("the player's hit on the Bailiff did not land");
        }
    }

    private static void assertAsleep(GameTestHelper helper, Bailiff bailiff) {
        if (!bailiff.isAsleep()) {
            throw helper.assertionException("the Bailiff is not asleep");
        }
    }

    private static void assertTaken(GameTestHelper helper, Bailiff bailiff, float health, String hit) {
        if (bailiff.getHealth() != health) {
            throw helper.assertionException(hit + " left it at " + bailiff.getHealth() + " health, not " + health);
        }
    }
}

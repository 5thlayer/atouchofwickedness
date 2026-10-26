// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * A Bailiff that arrives at night falls asleep where it arrived and robs nothing until it wakes: by
 * itself at dawn, or at the first hit, which deals double damage. Its time on site starts as it
 * wakes. Night falls and dawn breaks for each test's Bailiff alone ({@link Nights}), never on the
 * world's clock.
 */
final class SleepTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);
    private static final BlockPos CHEST = new BlockPos(2, 1, 6);

    /** Ticks a test watches a sleeping Bailiff for. */
    private static final int ASLEEP_FOR = 100;

    private SleepTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("sleep_arriving_at_night_it_sleeps_where_it_arrived_and_robs_nothing", 200, SleepTests::arrivingAtNight);
        tests.test("sleep_the_first_hit_deals_double_and_wakes_it", 100, SleepTests::firstHit);
        tests.test("sleep_its_time_on_site_starts_as_it_wakes", 1_500, SleepTests::timeOnSite);
        tests.test("sleep_it_wakes_at_dawn_and_gets_to_work", 300, SleepTests::wakesAtDawn);
    }

    private static void arrivingAtNight(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        chest(helper, player, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        var bailiff = comeForAtNight(helper, player);
        assertAsleep(helper, bailiff);
        var arrived = bailiff.position();
        var chest = helper.absolutePos(CHEST);
        helper.startSequence()
                .thenExecuteFor(ASLEEP_FOR, () -> {
                    assertAsleep(helper, bailiff);
                    if (bailiff.position().subtract(arrived).horizontalDistance() > 0.01) {
                        throw helper.assertionException("the sleeping Bailiff moved from where it arrived");
                    }
                    if (ChestBlockEntity.getOpenCount(helper.getLevel(), chest) != 0 || stacksIn(chest(helper)) != 2) {
                        throw helper.assertionException("the sleeping Bailiff robbed the chest");
                    }
                    if (Bailiffs.stacksIn(player.getInventory()) != 1) {
                        throw helper.assertionException("the sleeping Bailiff mugged its player");
                    }
                    if (player.heard.stream().anyMatch(sound -> sound.getSound().is(BailiffContent.TAUNTS.getKey()))) {
                        throw helper.assertionException("the sleeping Bailiff taunted its player");
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

    private static void timeOnSite(GameTestHelper helper) {
        // Out of its reach in the air, so once it wakes and finds the chest empty its time runs out.
        var player = Players.join(helper, PLAYER.above(12));
        chest(helper, player);
        var bailiff = comeForAtNight(helper, player);
        long[] woke = new long[1];
        helper.startSequence()
                // Asleep long enough that a clock counting from its arrival would flee 100 ticks early.
                .thenIdle(ASLEEP_FOR)
                .thenExecute(() -> {
                    hit(helper, bailiff, player, 1);
                    woke[0] = helper.getTick();
                })
                .thenWaitUntil(() -> {
                    if (!bailiff.isFleeing()) {
                        throw helper.assertionException("the Bailiff had not fled");
                    }
                })
                .thenExecute(() -> {
                    long onSite = helper.getTick() - woke[0];
                    if (Math.abs(onSite - WickednessConfig.ticksOnSite()) > 5) {
                        throw helper.assertionException("the Bailiff fled " + onSite + " ticks after it woke, not its "
                                + WickednessConfig.ticksOnSite() + " ticks on site");
                    }
                })
                .thenSucceed();
    }

    private static void wakesAtDawn(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = comeForAtNight(helper, player);
        helper.startSequence()
                .thenIdle(20)
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
                .thenWaitUntil(() -> {
                    if (Bailiffs.stacksIn(player.getInventory()) != 0) {
                        throw helper.assertionException("the woken Bailiff has not mugged its player");
                    }
                })
                .thenSucceed();
    }

    /** A Bailiff at {@link #BAILIFF}, arrived at night for {@code player}. */
    private static Bailiff comeForAtNight(GameTestHelper helper, ServerPlayer player) {
        var bailiff = helper.spawn(BailiffContent.BAILIFF.get(), BAILIFF);
        Nights.fall(bailiff);
        bailiff.comeFor(player);
        return bailiff;
    }

    /** Places the chest, holding {@code stacks}, and has the player open it, as with a right click, and close it. */
    private static void chest(GameTestHelper helper, ServerPlayer player, ItemStack... stacks) {
        helper.setBlock(CHEST, Blocks.CHEST);
        var chest = chest(helper);
        for (int slot = 0; slot < stacks.length; slot++) {
            chest.setItem(slot, stacks[slot]);
        }
        helper.useBlock(CHEST, player);
        player.closeContainer();
    }

    private static Container chest(GameTestHelper helper) {
        return (Container) helper.getBlockEntity(CHEST, BlockEntity.class);
    }

    private static int stacksIn(Container container) {
        int stacks = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                stacks++;
            }
        }
        return stacks;
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

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

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
 * A Bailiff that reaches its first container, or its player, at night falls asleep there and robs
 * nothing until it wakes: by itself at dawn, or at the first hit, which deals double damage. Its
 * time on site starts as it wakes. Night falls and dawn breaks for each test's Bailiff alone
 * ({@link Nights}), never on the world's clock.
 */
final class SleepTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos BAILIFF = new BlockPos(6, 1, 6);
    private static final BlockPos CHEST = new BlockPos(2, 1, 6);
    /** The platform's far corner from {@link #CHEST_FAR_AWAY}, so the Bailiff walks a while to reach it. */
    private static final BlockPos FAR_CORNER = new BlockPos(8, 1, 8);
    private static final BlockPos CHEST_FAR_AWAY = new BlockPos(0, 1, 0);

    /** Ticks a test watches a sleeping Bailiff for. */
    private static final int ASLEEP_FOR = 100;

    private SleepTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("sleep_by_its_chest_it_robs_nothing", 300, SleepTests::byItsChest);
        tests.test("sleep_by_its_player_it_mugs_nothing", 300, SleepTests::byItsPlayer);
        tests.test("sleep_by_its_empty_handed_player_it_taunts_nothing", 300, SleepTests::byItsEmptyHandedPlayer);
        tests.test("sleep_the_first_hit_deals_double_and_wakes_it", 200, SleepTests::firstHit);
        tests.test("sleep_its_time_on_site_starts_as_it_wakes", 1_700, SleepTests::timeOnSite);
        tests.test("sleep_it_wakes_at_dawn_and_gets_to_work", 300, SleepTests::wakesAtDawn);
    }

    private static void byItsChest(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        chest(helper, player, CHEST, new ItemStack(Items.LAPIS_LAZULI, 3), new ItemStack(Items.GOLD_INGOT, 9));
        var bailiff = atNight(Bailiffs.comeFor(helper, BAILIFF, player));
        var chest = helper.absolutePos(CHEST);
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                .thenExecuteFor(ASLEEP_FOR, () -> {
                    assertAsleep(helper, bailiff);
                    if (ChestBlockEntity.getOpenCount(helper.getLevel(), chest) != 0) {
                        throw helper.assertionException("the sleeping Bailiff opened the chest");
                    }
                    if (stacksIn(chest(helper, CHEST)) != 2) {
                        throw helper.assertionException("the sleeping Bailiff robbed the chest");
                    }
                })
                .thenSucceed();
    }

    private static void byItsPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = atNight(Bailiffs.comeFor(helper, BAILIFF, player));
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                .thenExecuteFor(ASLEEP_FOR, () -> {
                    assertAsleep(helper, bailiff);
                    if (Bailiffs.stacksIn(player.getInventory()) != 1 || player.getHealth() < player.getMaxHealth()) {
                        throw helper.assertionException("the sleeping Bailiff mugged its player");
                    }
                })
                .thenSucceed();
    }

    private static void byItsEmptyHandedPlayer(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        var bailiff = atNight(Bailiffs.comeFor(helper, BAILIFF, player));
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                .thenExecuteFor(ASLEEP_FOR, () -> {
                    assertAsleep(helper, bailiff);
                    if (bailiff.isTaunting() || player.heard.stream().anyMatch(sound -> sound.getSound().is(BailiffContent.TAUNTS.getKey()))) {
                        throw helper.assertionException("the sleeping Bailiff taunted its player");
                    }
                })
                .thenSucceed();
    }

    private static void firstHit(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = atNight(Bailiffs.comeFor(helper, BAILIFF, player));
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                .thenExecute(() -> {
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
                })
                .thenSucceed();
    }

    private static void timeOnSite(GameTestHelper helper) {
        // Out of its reach in the air, so once it wakes and finds the chest empty its time runs out.
        var player = Players.join(helper, PLAYER.above(12));
        chest(helper, player, CHEST_FAR_AWAY);
        // Its walk to the chest is on no clock: one that only paused for its sleep would run out early.
        var bailiff = atNight(Bailiffs.comeFor(helper, FAR_CORNER, player));
        long[] woke = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                // Long enough asleep that a clock counting from its arrival would run out first.
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
                    if (onSite < 1_195 || onSite > 1_205) {
                        throw helper.assertionException("the Bailiff fled " + onSite + " ticks after it woke, not 60 seconds");
                    }
                })
                .thenSucceed();
    }

    private static void wakesAtDawn(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Players.stock(player, new ItemStack(Items.EMERALD, 7));
        var bailiff = atNight(Bailiffs.comeFor(helper, BAILIFF, player));
        helper.startSequence()
                .thenWaitUntil(() -> assertAsleep(helper, bailiff))
                .thenExecute(() -> Nights.dawn(bailiff))
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

    /** @return {@code bailiff}, where night has fallen */
    private static Bailiff atNight(Bailiff bailiff) {
        Nights.fall(bailiff);
        return bailiff;
    }

    /** Places a chest at {@code relative}, holding {@code stacks}, and has the player open it, as with a right click, and close it. */
    private static void chest(GameTestHelper helper, ServerPlayer player, BlockPos relative, ItemStack... stacks) {
        helper.setBlock(relative, Blocks.CHEST);
        var chest = chest(helper, relative);
        for (int slot = 0; slot < stacks.length; slot++) {
            chest.setItem(slot, stacks[slot]);
        }
        helper.useBlock(relative, player);
        player.closeContainer();
    }

    private static Container chest(GameTestHelper helper, BlockPos relative) {
        return (Container) helper.getBlockEntity(relative, BlockEntity.class);
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

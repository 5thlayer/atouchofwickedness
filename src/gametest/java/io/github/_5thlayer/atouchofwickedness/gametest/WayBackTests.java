// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The way back: Wickedness falls by 10 over an in-game day, and kind acts take it down at once,
 * each credited to the player who did it: breeding two animals 3, taming one 10, feeding a hurt pet
 * the player owns 1 per heart it regains, curing a zombie villager 40, and a villager picking up food
 * the player threw 2, five times a day. A player standing by is credited nothing.
 */
final class WayBackTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);

    private static final int WICKEDNESS = 50;

    /** The ticks of an in-game day. */
    private static final long DAY = 24_000;

    private WayBackTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("way_back_decay_a_day_takes_off_10", 20, WayBackTests::decay);
        tests.test("way_back_breeding_takes_off_3_from_the_breeder_alone", 20, WayBackTests::breeding);
        tests.test("way_back_taming_takes_off_10", 20, WayBackTests::taming);
        tests.test("way_back_healing_a_hurt_pet_takes_off_1_per_heart", 20, WayBackTests::healingAHurtPet);
        tests.test("way_back_feeding_a_healthy_pet_takes_off_nothing", 20, WayBackTests::feedingAHealthyPet);
        tests.test("way_back_curing_a_zombie_villager_takes_off_40", 100, WayBackTests::curing);
        tests.test("way_back_gifts_take_off_2_until_the_sixth_of_the_day", 200, WayBackTests::gifts);
    }

    private static void decay(GameTestHelper helper) {
        var player = wicked(helper);
        // A tick for Decay to read the player's clock, then a whole day passes on it at once.
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> Clocks.pass(player, DAY))
                .thenIdle(1)
                .thenExecute(() -> assertWickedness(helper, player, WICKEDNESS - 10, "an in-game day"))
                .thenSucceed();
    }

    private static void breeding(GameTestHelper helper) {
        var player = wicked(helper);
        var bystander = Players.join(helper, PLAYER.north());
        Wickedness.setThreshold(bystander, 1_000);
        Wickedness.set(bystander, WICKEDNESS);
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, CREATURE);
        var partner = helper.spawnWithNoFreeWill(EntityType.COW, CREATURE.east());
        feed(player, cow, Items.WHEAT);
        feed(player, partner, Items.WHEAT);
        // What vanilla's breeding goal does once the two meet.
        cow.spawnChildFromBreeding(helper.getLevel(), partner);
        assertWickedness(helper, player, WICKEDNESS - 3, "breeding two cows");
        assertWickedness(helper, bystander, WICKEDNESS, "standing by while another player bred two cows");
        helper.succeed();
    }

    private static void taming(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        // Each bone tames a wolf one time in three, so a hundred fail about once in 10^17.
        for (int bone = 0; bone < 100 && !wolf.isTame(); bone++) {
            feed(player, wolf, Items.BONE);
        }
        helper.assertTrue(wolf.isTame(), "a hundred bones did not tame the wolf");
        assertWickedness(helper, player, WICKEDNESS - 10, "taming a wolf");
        helper.succeed();
    }

    private static void healingAHurtPet(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = pet(helper, player);
        wolf.setHealth(10);
        // Cooked beef heals a wolf twice its 8 nutrition: 16 health, 8 hearts.
        feed(player, wolf, Items.COOKED_BEEF);
        helper.assertTrue(wolf.getHealth() == 26, "the wolf regained " + (wolf.getHealth() - 10) + " health, not 16");
        assertWickedness(helper, player, WICKEDNESS - 8, "feeding a hurt wolf 8 hearts");
        helper.succeed();
    }

    private static void feedingAHealthyPet(GameTestHelper helper) {
        var player = wicked(helper);
        feed(player, pet(helper, player), Items.COOKED_BEEF);
        assertWickedness(helper, player, WICKEDNESS, "feeding a healthy wolf");
        helper.succeed();
    }

    private static void curing(GameTestHelper helper) {
        var player = wicked(helper);
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE_VILLAGER, CREATURE);
        zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
        feed(player, zombie, Items.GOLDEN_APPLE);
        helper.assertTrue(zombie.isConverting(), "the golden apple did not start curing the zombie villager");
        // A cure takes three minutes or more: the rest of it passes at once.
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "data merge entity " + zombie.getStringUUID() + " {ConversionTime:1}");
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.isRemoved(), "the zombie villager is not cured yet");
            assertWickedness(helper, player, WICKEDNESS - 40, "curing a zombie villager");
        });
    }

    private static void gifts(GameTestHelper helper) {
        var player = wicked(helper);
        var villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, CREATURE);
        var sequence = helper.startSequence();
        for (int gift = 1; gift <= 6; gift++) {
            int given = gift;
            var thrown = new ItemEntity[1];
            sequence.thenExecute(() -> thrown[0] = throwTo(player, villager, Items.BREAD))
                    .thenWaitUntil(() -> helper.assertTrue(thrown[0].isRemoved(), "the villager has not picked up bread " + given + " yet"))
                    .thenExecute(() -> assertWickedness(helper, player, WICKEDNESS - 2 * Math.min(given, 5), given + " gifts of bread on one day"));
        }
        sequence.thenSucceed();
    }

    private static ServerPlayer wicked(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        // Far above anything here, so no Bailiff comes and resets it.
        Wickedness.setThreshold(player, 1_000);
        Wickedness.set(player, WICKEDNESS);
        return player;
    }

    private static Wolf pet(GameTestHelper helper, ServerPlayer owner) {
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        wolf.setTame(true, true);
        wolf.setOwner(owner);
        return wolf;
    }

    /** {@code player} uses one {@code item} on {@code creature}, as a right click does. */
    private static void feed(ServerPlayer player, LivingEntity creature, Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        player.interactOn(creature, InteractionHand.MAIN_HAND, creature.position());
    }

    /** {@code player} throws one {@code item}, which lands at {@code villager}'s feet. */
    private static ItemEntity throwTo(ServerPlayer player, Villager villager, Item item) {
        var thrown = player.drop(new ItemStack(item), true);
        if (thrown == null) {
            throw new IllegalStateException("the player threw nothing");
        }
        thrown.setPos(villager.position());
        thrown.setDeltaMovement(0, 0, 0);
        thrown.setNoPickUpDelay();
        return thrown;
    }

    private static void assertWickedness(GameTestHelper helper, ServerPlayer player, int expected, String what) {
        int wickedness = Wickedness.of(player);
        if (wickedness != expected) {
            throw helper.assertionException(what + " left Wickedness at " + wickedness + ", not " + expected);
        }
    }
}

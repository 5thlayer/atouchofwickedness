// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Fair blame: a kill is credited to the player vanilla credits it to, the one who last hurt the
 * creature within its memory of them, or the owner of the tamed pet that did, and to no one when no
 * player had a hand in it. On top of the data map, a tamed creature weighs 50 whoever owns it, a baby
 * double, and a neutral mob going for its killer nothing, unless the killer struck it before it went
 * for them; each damaging hit short of the killing blow on a creature weighing 30 or more adds 3. A
 * Creative or Spectator player gains nothing and is never visited, and dying changes nothing.
 */
final class BlameTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);

    private BlameTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("blame_cow_shot_with_an_arrow_adds_2", 40, BlameTests::shot);
        tests.test("blame_cow_killed_by_a_tamed_wolf_adds_2_to_its_owner", 20, BlameTests::byPet);
        tests.test("blame_cow_hurt_then_killed_by_a_trap_adds_2", 20, BlameTests::trapped);
        tests.test("blame_cow_killed_with_no_player_adds_nothing", 20, BlameTests::noPlayer);
        tests.test("blame_baby_cow_adds_4", 20, BlameTests::baby);
        tests.test("blame_tamed_wolf_of_another_player_adds_50", 20, BlameTests::tamed);
        tests.test("blame_wolf_going_for_the_player_adds_nothing", 20, BlameTests::selfDefence);
        tests.test("blame_tamed_wolf_going_for_the_player_adds_nothing", 20, BlameTests::tamedSelfDefence);
        tests.test("blame_wolf_the_player_struck_first_still_adds_15", 20, BlameTests::provokedWolf);
        tests.test("blame_tamed_wolf_the_player_struck_first_adds_50_and_3_a_hit", 20, BlameTests::provokedTamedWolf);
        tests.test("blame_wolf_the_player_struck_first_with_a_snowball_still_adds_15", 20, BlameTests::snowballedWolf);
        tests.test("blame_iron_golem_going_for_the_player_still_adds_15", 20, BlameTests::provokedGolem);
        tests.test("blame_each_hit_on_a_villager_adds_3", 20, BlameTests::hitsOnAVillager);
        tests.test("blame_a_hit_on_a_cow_adds_nothing", 20, BlameTests::hitOnACow);
        tests.test("blame_creative_player_gains_nothing_and_is_never_visited", 20, BlameTests::creative);
        tests.test("blame_spectator_gains_nothing", 20, BlameTests::spectator);
        tests.test("blame_death_leaves_wickedness_unchanged", 20, BlameTests::death);
    }

    private static void shot(GameTestHelper helper) {
        var player = wicked(helper);
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, CREATURE);
        var arrow = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(100);
        var above = helper.absoluteVec(CREATURE.getBottomCenter()).add(0, 3, 0);
        arrow.snapTo(above.x, above.y, above.z, 0, 90);
        arrow.setDeltaMovement(0, -2, 0);
        helper.getLevel().addFreshEntity(arrow);
        helper.succeedWhen(() -> {
            helper.assertTrue(!cow.isAlive(), "the arrow has not killed the cow yet");
            assertWickedness(helper, player, 2, "a cow shot with an arrow");
        });
    }

    private static void byPet(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, PLAYER.east());
        wolf.setTame(true, false);
        wolf.setOwner(player);
        kill(helper, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE), helper.getLevel().damageSources().mobAttack(wolf));
        assertWickedness(helper, player, 2, "a cow killed by the player's tamed wolf");
        helper.succeed();
    }

    private static void trapped(GameTestHelper helper) {
        var player = wicked(helper);
        var cow = helper.spawnWithNoFreeWill(EntityType.COW, CREATURE);
        hit(helper, player, cow);
        kill(helper, cow, helper.getLevel().damageSources().lava());
        assertWickedness(helper, player, 2, "a cow the player hurt that then died in lava");
        helper.succeed();
    }

    private static void noPlayer(GameTestHelper helper) {
        var player = wicked(helper);
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, PLAYER.east());
        kill(helper, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE), helper.getLevel().damageSources().mobAttack(zombie));
        kill(helper, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE), helper.getLevel().damageSources().lava());
        assertWickedness(helper, player, 0, "cows killed by a zombie and by lava");
        helper.succeed();
    }

    private static void baby(GameTestHelper helper) {
        var player = wicked(helper);
        var calf = helper.spawnWithNoFreeWill(EntityType.COW, CREATURE);
        calf.setBaby(true);
        WickedKillTests.killInMelee(helper, player, calf);
        assertWickedness(helper, player, 4, "a baby cow killed in melee");
        helper.succeed();
    }

    private static void tamed(GameTestHelper helper) {
        var player = wicked(helper);
        var owner = Players.join(helper, PLAYER.north());
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        wolf.setTame(true, false);
        wolf.setOwner(owner);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 50, "another player's tamed wolf killed in melee");
        helper.succeed();
    }

    private static void selfDefence(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        wolf.setTarget(player);
        // Hits while it goes for the player are self-defence too, not striking first.
        hit(helper, player, wolf);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 0, "a wolf going for the player killed in melee");
        helper.succeed();
    }

    private static void tamedSelfDefence(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        wolf.setTame(true, false);
        wolf.setOwner(Players.join(helper, PLAYER.north()));
        wolf.setTarget(player);
        hit(helper, player, wolf);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 0, "another player's tamed wolf going for the player, hit and killed in melee");
        helper.succeed();
    }

    private static void provokedWolf(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        hit(helper, player, wolf);
        // As its hurt-by goal would on its next tick.
        wolf.setTarget(player);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 15, "a wolf the player struck before it went for them, killed in melee");
        helper.succeed();
    }

    private static void snowballedWolf(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        // A snowball hurts a wolf for nothing, but angers it all the same.
        var snowball = new Snowball(helper.getLevel(), player, new ItemStack(Items.SNOWBALL));
        wolf.hurtServer(helper.getLevel(), helper.getLevel().damageSources().thrown(snowball, player), 0);
        wolf.setTarget(player);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 15, "a wolf the player struck first with a snowball, killed in melee");
        helper.succeed();
    }

    private static void provokedTamedWolf(GameTestHelper helper) {
        var player = wicked(helper);
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, CREATURE);
        wolf.setTame(true, false);
        wolf.setOwner(Players.join(helper, PLAYER.north()));
        hit(helper, player, wolf);
        wolf.setTarget(player);
        hit(helper, player, wolf);
        WickedKillTests.killInMelee(helper, player, wolf);
        assertWickedness(helper, player, 56, "another player's tamed wolf struck first, hit again as it went for the player and killed");
        helper.succeed();
    }

    private static void provokedGolem(GameTestHelper helper) {
        var player = wicked(helper);
        var golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, CREATURE);
        golem.setTarget(player);
        WickedKillTests.killInMelee(helper, player, golem);
        assertWickedness(helper, player, 15, "an iron golem going for the player killed in melee");
        helper.succeed();
    }

    private static void hitsOnAVillager(GameTestHelper helper) {
        var player = wicked(helper);
        var villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, CREATURE);
        for (int i = 0; i < 3; i++) {
            hit(helper, player, villager);
        }
        assertWickedness(helper, player, 9, "three hits on a villager");
        // A hit the villager shrugs off, still hurt from the last, does no harm, so it costs nothing.
        villager.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1);
        assertWickedness(helper, player, 9, "a hit while the villager was still invulnerable");
        helper.succeed();
    }

    private static void hitOnACow(GameTestHelper helper) {
        var player = wicked(helper);
        hit(helper, player, helper.spawnWithNoFreeWill(EntityType.COW, CREATURE));
        assertWickedness(helper, player, 0, "a hit on a cow");
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        player.setGameMode(GameType.CREATIVE);
        Wickedness.setThreshold(player, 50);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.CAT, CREATURE));
        hit(helper, player, helper.spawnWithNoFreeWill(EntityType.VILLAGER, CREATURE));
        assertWickedness(helper, player, 0, "a Creative player killing a cat and hitting a villager");
        Wickedness.set(player, 60);
        if (!Bailiffs.of(player).isEmpty()) {
            helper.fail("a Bailiff came for a Creative player past their Threshold");
            return;
        }
        helper.succeed();
    }

    private static void spectator(GameTestHelper helper) {
        var player = wicked(helper);
        player.setGameMode(GameType.SPECTATOR);
        WickedKillTests.killInMelee(helper, player, helper.spawnWithNoFreeWill(EntityType.CAT, CREATURE));
        hit(helper, player, helper.spawnWithNoFreeWill(EntityType.VILLAGER, CREATURE));
        assertWickedness(helper, player, 0, "a Spectator killing a cat and hitting a villager");
        helper.succeed();
    }

    private static void death(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 87);
        Wickedness.set(player, 23);
        kill(helper, player, helper.getLevel().damageSources().lava());
        var respawned = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
        if (Wickedness.of(respawned) != 23 || Wickedness.threshold(respawned) != 87) {
            helper.fail("after dying and respawning the player has " + Wickedness.of(respawned) + " Wickedness against a Threshold of "
                    + Wickedness.threshold(respawned) + ", not 23 against 87");
            return;
        }
        helper.succeed();
    }

    /** A survival player whose Threshold is far above anything a test adds, so no Bailiff comes and resets it. */
    private static ServerPlayer wicked(GameTestHelper helper) {
        var player = Players.join(helper, PLAYER);
        Wickedness.setThreshold(player, 1_000);
        return player;
    }

    /** {@code player} lands a light blow on {@code creature}, which could be hurt again at once. */
    private static void hit(GameTestHelper helper, ServerPlayer player, LivingEntity creature) {
        creature.invulnerableTime = 0;
        creature.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1);
    }

    private static void kill(GameTestHelper helper, LivingEntity creature, DamageSource source) {
        creature.hurtServer(helper.getLevel(), source, 10_000);
        if (creature.isAlive()) {
            throw helper.assertionException("the " + creature.getType().getDescription().getString() + " survived the blow");
        }
    }

    private static void assertWickedness(GameTestHelper helper, ServerPlayer player, int expected, String what) {
        int wickedness = Wickedness.of(player);
        if (wickedness != expected) {
            throw helper.assertionException(what + " left Wickedness at " + wickedness + ", not " + expected);
        }
    }
}

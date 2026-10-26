// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A creature a player kills in melee adds its weight to the player's Wickedness: its entry in the
 * {@code atouchofwickedness:wickedness_weight} data map, which a datapack can change, and nothing
 * when it has none. The game-test pack weighs a mooshroom 11, not its default 5.
 */
final class WickedKillTests {

    private static final BlockPos PLAYER = new BlockPos(2, 1, 2);
    private static final BlockPos CREATURE = new BlockPos(4, 1, 4);

    private WickedKillTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("wicked_kill_cow_in_melee_adds_2", 20, helper -> killing(helper, EntityType.COW, 2));
        tests.test("wicked_kill_villager_in_melee_adds_30", 20, helper -> killing(helper, EntityType.VILLAGER, 30));
        tests.test("wicked_kill_unlisted_creature_adds_nothing", 20, helper -> killing(helper, EntityType.ZOMBIE, 0));
        tests.test("wicked_kill_datapack_entry_changes_the_weight", 20, helper -> killing(helper, EntityType.MOOSHROOM, 11));
    }

    private static void killing(GameTestHelper helper, EntityType<? extends Mob> type, int added) {
        var player = Players.join(helper, PLAYER);
        // Far above anything one kill adds, so no Bailiff comes and resets it.
        Wickedness.setThreshold(player, 1_000);
        killInMelee(helper, player, helper.spawnWithNoFreeWill(type, CREATURE));
        int wickedness = Wickedness.of(player);
        if (wickedness != added) {
            helper.fail("killing a " + type.getDescription().getString() + " in melee left Wickedness at " + wickedness + ", not " + added);
            return;
        }
        helper.succeed();
    }

    /** {@code player} strikes {@code creature} dead. */
    static void killInMelee(GameTestHelper helper, ServerPlayer player, LivingEntity creature) {
        creature.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 10_000);
        if (creature.isAlive()) {
            throw helper.assertionException("the " + creature.getType().getDescription().getString() + " survived the blow");
        }
    }
}

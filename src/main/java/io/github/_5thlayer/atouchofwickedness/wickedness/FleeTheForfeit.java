// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * A passive animal's goal to flee from a Forfeit player it sees within the config's radius, on
 * any difficulty, Peaceful too; a tamed one never flees. Each passive animal gets it as it joins a
 * level. One that steers by a brain as well as goals, such as a goat, may have its brain pull it
 * back.
 */
public final class FleeTheForfeit extends AvoidEntityGoal<Player> {

    /** Not vanilla's combat check, which no animal passes on Peaceful. */
    private final TargetingConditions forfeitInSight;

    private FleeTheForfeit(Animal animal, float radius) {
        super(animal, Player.class, radius, WickednessConfig.animalFleeSpeed(), WickednessConfig.animalFleeSprintSpeed());
        forfeitInSight = TargetingConditions.forNonCombat().range(radius).selector((target, level) -> isForfeit(target));
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal && animal.getType().getCategory().isFriendly()) {
                animal.goalSelector.addGoal(1, new FleeTheForfeit(animal, WickednessConfig.fleeRadius()));
            }
        });
    }

    /** As vanilla's, but never for a tamed animal, and finding the player by {@link #forfeitInSight}. */
    @Override
    public boolean canUse() {
        if (mob instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
            return false;
        }
        var level = getServerLevel(mob);
        toAvoid = level.getNearestEntity(level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(maxDist, 3, maxDist), player -> true),
                forfeitInSight, mob, mob.getX(), mob.getY(), mob.getZ());
        if (toAvoid == null) {
            return false;
        }
        var away = DefaultRandomPos.getPosAway(mob, 16, 7, toAvoid.position());
        if (away == null || toAvoid.distanceToSqr(away) < toAvoid.distanceToSqr(mob)) {
            return false;
        }
        path = pathNav.createPath(away.x, away.y, away.z, 0);
        return path != null;
    }

    private static boolean isForfeit(LivingEntity target) {
        return target instanceof ServerPlayer player && Wickedness.counts(player) && Wickedness.standing(player) == Standing.FORFEIT;
    }
}

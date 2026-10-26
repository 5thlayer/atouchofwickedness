// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.mixin.ZombieVillagerAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * The kind acts that take an {@link Atonement} off Wickedness, each credited to the player who did it:
 * breeding two animals, taming one, feeding a hurt tamed pet they own, curing a zombie villager, and
 * a villager picking up food they threw. Each listens last, so an act another mod cancels counts for
 * nothing; a player in Creative or Spectator atones for nothing, as their kills add nothing.
 */
public final class KindActs {

    /** The pet a player is feeding this tick, so the healing that follows is credited to them. */
    private static @Nullable Feeding feeding;

    private record Feeding(LivingEntity pet, ServerPlayer owner, long tick) {
    }

    private KindActs() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KindActs::onBreeding);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KindActs::onTaming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KindActs::onInteract);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KindActs::onHeal);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, KindActs::onConversion);
    }

    /** A {@code creature} picking up {@code item}: a gift, when it is a villager and the item food the player threw. */
    public static void pickedUp(LivingEntity creature, ItemEntity item) {
        if (creature instanceof Villager && item.getOwner() instanceof ServerPlayer player && item.getItem().has(DataComponents.FOOD)) {
            Wickedness.gift(player);
        }
    }

    private static void onBreeding(BabyEntitySpawnEvent event) {
        if (event.getChild() != null && event.getCausedByPlayer() instanceof ServerPlayer player) {
            Wickedness.atone(player, WickednessConfig.breeding());
        }
    }

    private static void onTaming(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            Wickedness.atone(player, WickednessConfig.taming());
        }
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Animal pet
                && pet instanceof OwnableEntity ownable && ownable.getOwner() == player && pet.isFood(event.getItemStack())) {
            feeding = new Feeding(pet, player, pet.level().getGameTime());
        }
    }

    private static void onHeal(LivingHealEvent event) {
        var fed = feeding;
        var pet = event.getEntity();
        if (fed == null || fed.pet() != pet) {
            return;
        }
        feeding = null;
        // Healing the tick after is not the food's: the pet ate nothing, being healthy or full.
        if (fed.tick() != pet.level().getGameTime()) {
            return;
        }
        float regained = Math.min(event.getAmount(), pet.getMaxHealth() - pet.getHealth());
        Wickedness.atone(fed.owner(), Atonement.feeding(regained, WickednessConfig.feedingPerHeart()));
    }

    private static void onConversion(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof ZombieVillager zombie) || !(event.getOutcome() instanceof Villager)
                || !(zombie.level() instanceof ServerLevel level)) {
            return;
        }
        // Who fed it the golden apple, as vanilla credits the cure's gossip and advancement.
        var curer = ((ZombieVillagerAccessor) zombie).atouchofwickedness$conversionStarter();
        if (curer != null && level.getPlayerByUUID(curer) instanceof ServerPlayer player) {
            Wickedness.atone(player, WickednessConfig.curing());
        }
    }
}

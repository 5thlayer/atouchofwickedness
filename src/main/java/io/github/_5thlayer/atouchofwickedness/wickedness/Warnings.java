// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Warnings a player hears as their Wickedness nears the Threshold, each with a subtitle and
 * sent to that player alone: a far bell while Marked, chains nearer while Overdue, and footsteps
 * close by while Forfeit. {@link Warning} says when one sounds. While a player is Forfeit, passive
 * animals near them flee from them; a tamed one never flees, nor does one that thinks with a brain
 * rather than goals, such as a goat.
 */
public final class Warnings {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfWickedness.MOD_ID);

    /** Subtitled "A far bell tolls". */
    public static final DeferredHolder<SoundEvent, SoundEvent> MARKED = sound("warning.marked");
    /** Subtitled "Chains rattle". */
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERDUE = sound("warning.overdue");
    /** Subtitled "Footsteps draw near". */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORFEIT = sound("warning.forfeit");

    /** Loud enough to be heard 32 blocks off, from the farthest a Warning sounds, so each nearer one is louder. */
    private static final float VOLUME = 2.0F;

    /** A band's Warning and how far from its player it sounds. */
    private record Sign(DeferredHolder<SoundEvent, SoundEvent> sound, double distance) {
    }

    private Warnings() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Warnings::fleeTheForfeit);
    }

    /** Plays the Warning of {@code standing}, which is not Clean, to the player alone, from a random direction. */
    static void sound(ServerPlayer player, Standing standing) {
        // Each band's nearer than the last.
        var sign = switch (standing) {
            case CLEAN -> throw new IllegalArgumentException("a Clean player hears no Warning");
            case MARKED -> new Sign(MARKED, 24);
            case OVERDUE -> new Sign(OVERDUE, 12);
            case FORFEIT -> new Sign(FORFEIT, 4);
        };
        var random = player.getRandom();
        double angle = random.nextDouble() * Math.TAU;
        player.connection.send(new ClientboundSoundPacket(sign.sound(), SoundSource.AMBIENT,
                player.getX() + Math.cos(angle) * sign.distance(), player.getEyeY(), player.getZ() + Math.sin(angle) * sign.distance(),
                VOLUME, 0.9F + random.nextFloat() * 0.2F, random.nextLong()));
    }

    /** Gives each passive animal joining a level a goal to flee from a Forfeit player within the config's radius. */
    private static void fleeTheForfeit(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal && animal.getType().getCategory().isFriendly()) {
            animal.goalSelector.addGoal(1, new AvoidEntityGoal<>(animal, Player.class, WickednessConfig.fleeRadius(), 1.0, 1.4,
                    target -> !isTamed(animal) && isForfeit(target)));
        }
    }

    private static boolean isForfeit(LivingEntity target) {
        return target instanceof ServerPlayer player && Wickedness.counts(player) && Wickedness.standing(player) == Standing.FORFEIT;
    }

    private static boolean isTamed(Animal animal) {
        return animal instanceof OwnableEntity ownable && ownable.getOwnerReference() != null;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, path)));
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.Map;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Warnings a player hears as their Wickedness nears the Threshold, each with a subtitle and
 * sent to that player alone: a far bell while Marked, chains nearer while Overdue, and footsteps
 * close by while Forfeit. {@link Warning} says when one sounds, and how far off.
 */
public final class WarningSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfWickedness.MOD_ID);

    /** Subtitled "A far bell tolls". */
    public static final DeferredHolder<SoundEvent, SoundEvent> MARKED = sound("warning.marked");
    /** Subtitled "Chains rattle". */
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERDUE = sound("warning.overdue");
    /** Subtitled "Footsteps draw near". */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORFEIT = sound("warning.forfeit");

    private static final Map<Standing, DeferredHolder<SoundEvent, SoundEvent>> OF_BAND =
            Map.of(Standing.MARKED, MARKED, Standing.OVERDUE, OVERDUE, Standing.FORFEIT, FORFEIT);

    /**
     * Eight times each sound's own volume, so the quietest, vanilla's chains, carry 64 blocks: a
     * Warning as far off as the config lets it sound, 32 blocks, still reaches its player at half
     * its loudness or more, and the bell, defined far louder, at its full.
     */
    private static final float VOLUME = 8.0F;

    private WarningSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    /**
     * Plays the Warning of {@code standing}, which is not Clean, to the player alone, {@code distance}
     * blocks off in a random direction.
     */
    static void play(ServerPlayer player, Standing standing, double distance) {
        var random = player.getRandom();
        double angle = random.nextDouble() * Math.TAU;
        player.connection.send(new ClientboundSoundPacket(OF_BAND.get(standing), SoundSource.AMBIENT,
                player.getX() + Math.cos(angle) * distance, player.getEyeY(), player.getZ() + Math.sin(angle) * distance,
                VOLUME, 0.9F + random.nextFloat() * 0.2F, random.nextLong()));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, path)));
    }
}

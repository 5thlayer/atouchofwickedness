// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.function.Supplier;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.bailiff.Arrival;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A player's Wickedness, which the player never sees as a number (ADR 0002), and their Threshold,
 * drawn at random when they first join and again after each visit. When Wickedness reaches the
 * Threshold, a Bailiff comes for the player ({@link Arrival}): Wickedness drops to 0 and a new
 * Threshold is drawn as it arrives.
 */
public final class Wickedness {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfWickedness.MOD_ID);

    private static final Supplier<AttachmentType<Tally>> TALLY = ATTACHMENTS.register("wickedness",
            () -> AttachmentType.builder(holder -> new Tally(0, draw(holder instanceof Entity entity ? entity.getRandom() : RandomSource.create())))
                    .serialize(Tally.CODEC)
                    .copyOnDeath()
                    .build());

    private Wickedness() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        // The Threshold is drawn on first join, so it is saved from then on.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> event.getEntity().getData(TALLY));
    }

    /** @return the player's Wickedness */
    public static int of(ServerPlayer player) {
        return player.getData(TALLY).wickedness;
    }

    /** @return the player's Threshold */
    public static int threshold(ServerPlayer player) {
        return player.getData(TALLY).threshold;
    }

    /** @return the player's Standing, the rough band of their Wickedness against their Threshold */
    public static Standing standing(ServerPlayer player) {
        var tally = player.getData(TALLY);
        return Standing.of(tally.wickedness, tally.threshold, WickednessConfig.bands());
    }

    /** Adds {@code amount}, which may be negative, to the player's Wickedness, never below 0. */
    public static void add(ServerPlayer player, int amount) {
        set(player, (int) Math.clamp((long) of(player) + amount, 0, Integer.MAX_VALUE));
    }

    /** Sets the player's Wickedness to {@code value}, never below 0, and sends the Bailiff once it reaches the Threshold. */
    public static void set(ServerPlayer player, int value) {
        var tally = player.getData(TALLY);
        tally.wickedness = Math.max(0, value);
        player.setData(TALLY, tally);
        if (tally.wickedness >= tally.threshold && Arrival.arrive(player)) {
            tally.wickedness = 0;
            tally.threshold = draw(player.getRandom());
        }
    }

    /** Sets the player's Threshold. For the game tests, which need to know when a Bailiff comes. */
    public static void setThreshold(ServerPlayer player, int threshold) {
        var tally = player.getData(TALLY);
        tally.threshold = Math.max(1, threshold);
        player.setData(TALLY, tally);
    }

    private static int draw(RandomSource random) {
        return Threshold.draw(WickednessConfig.thresholdMin(), WickednessConfig.thresholdMax(), random::nextInt);
    }
}

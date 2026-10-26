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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A player's Wickedness, which the player never sees as a number (ADR 0002), and their Threshold,
 * drawn at random when they first join and again after each visit. When Wickedness reaches the
 * Threshold, a Bailiff comes for the player ({@link Arrival}): Wickedness drops to 0 and a new
 * Threshold is drawn as it arrives. No other Bailiff comes for the player until the cooldown
 * after that arrival is over. While the player stays at or above their Threshold, as when a
 * Bailiff found no spot to arrive on or the cooldown still ran, a Bailiff tries again every second.
 * As Wickedness nears the Threshold, the player hears {@link Warnings}. Both are copied through death, so
 * dying changes neither.
 */
public final class Wickedness {

    /** Ticks between a Bailiff's tries to arrive while the player is at or above their Threshold. */
    private static final int RETRY_TICKS = 20;

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
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            boolean retry = event.getServer().getTickCount() % RETRY_TICKS == 0;
            event.getServer().getPlayerList().getPlayers().forEach(player -> tick(player, retry));
        });
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

    /** @return whether the player's deeds count: not in Creative or Spectator, who gain nothing and are never visited */
    public static boolean counts(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * Sets the player's Wickedness to {@code value}, never below 0, and sends the Bailiff once it
     * reaches the Threshold, unless the player is in Creative or Spectator.
     */
    public static void set(ServerPlayer player, int value) {
        var tally = player.getData(TALLY);
        tally.wickedness = Math.max(0, value);
        player.setData(TALLY, tally);
        visit(player, tally);
    }

    /**
     * Sends the Bailiff once the player's Wickedness is at their Threshold and the cooldown after
     * the last one is over, and as it arrives resets Wickedness to 0, draws a new Threshold and
     * starts the cooldown, in the tally the player holds.
     */
    private static void visit(ServerPlayer player, Tally tally) {
        long now = player.level().getGameTime();
        if (tally.wickedness >= tally.threshold && counts(player)
                && VisitCooldown.over(tally.arrivedAt, now, WickednessConfig.ticksBetweenVisits())
                && Arrival.arrive(player)) {
            tally.wickedness = 0;
            tally.threshold = draw(player.getRandom());
            tally.arrivedAt = now;
        }
    }

    /** Lowers the player's Wickedness by an Atonement of {@code amount}, unless the player is in Creative or Spectator. */
    public static void atone(ServerPlayer player, int amount) {
        if (counts(player) && amount > 0) {
            set(player, Atonement.lowered(of(player), amount));
        }
    }

    /** Lowers the player's Wickedness by a gift's Atonement, while their gifts today are under the cap. */
    public static void gift(ServerPlayer player) {
        if (!counts(player)) {
            return;
        }
        var tally = player.getData(TALLY);
        var rules = WickednessConfig.atonementRules();
        long today = Decay.dayOf(player.level().getOverworldClockTime());
        if (tally.gifts.counts(today, rules.giftsPerDay())) {
            tally.gifts = tally.gifts.plusOne(today);
            atone(player, rules.gift());
        }
    }

    /** Runs the player's Decay, on a retry tick tries the Bailiff's arrival again, and sounds any Warning due. */
    private static void tick(ServerPlayer player, boolean retry) {
        decay(player);
        if (retry) {
            visit(player, player.getData(TALLY));
        }
        warn(player);
    }

    /** Sounds the player's Warning if one is due, unless the player is in Creative or Spectator. */
    private static void warn(ServerPlayer player) {
        var tally = player.getData(TALLY);
        var standing = standing(player);
        long now = player.level().getGameTime();
        if (counts(player) && Warning.due(tally.seen, standing, tally.warnedAt, now, WickednessConfig.warningIntervals())) {
            Warnings.sound(player, standing);
            tally.warnedAt = now;
        }
        tally.seen = standing;
    }

    /** Runs the player's Decay over the clock's ticks since it last ran, which the first tick seen only reads. */
    private static void decay(ServerPlayer player) {
        var tally = player.getData(TALLY);
        long now = player.level().getOverworldClockTime();
        long elapsed = tally.decayedAt == Tally.UNSEEN ? 0 : now - tally.decayedAt;
        tally.decayedAt = now;
        var decayed = Decay.over(new Decay.State(tally.wickedness, tally.decayCarry), elapsed, WickednessConfig.decayPerDay());
        tally.decayCarry = decayed.carry();
        if (decayed.wickedness() != tally.wickedness) {
            set(player, decayed.wickedness());
        }
    }

    /** Sets the player's Threshold, at least 1, until the next visit draws another. */
    public static void setThreshold(ServerPlayer player, int threshold) {
        var tally = player.getData(TALLY);
        tally.threshold = Math.max(1, threshold);
        player.setData(TALLY, tally);
    }

    private static int draw(RandomSource random) {
        return Threshold.draw(WickednessConfig.thresholdMin(), WickednessConfig.thresholdMax(), random::nextInt);
    }
}

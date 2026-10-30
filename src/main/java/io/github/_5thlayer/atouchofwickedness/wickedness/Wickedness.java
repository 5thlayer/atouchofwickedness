// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.bailiff.Arrival;
import io.github._5thlayer.atouchofwickedness.discovery.Discoveries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
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
 * Threshold is drawn as it arrives, though one that leaves with nothing leaves the player owing
 * ({@link #owe}). No other Bailiff comes for the player until the cooldown
 * after that arrival is over. While the player stays at or above their Threshold, as when a
 * Bailiff found no spot to arrive on or the cooldown still ran, the visit stays owed and a Bailiff
 * tries again every few seconds, the config's retry interval; a dead player's waits until they
 * respawn.
 * Wickedness and the Threshold are copied through death, so dying changes neither. As Wickedness
 * nears the Threshold, the player hears {@link WarningSounds}.
 * <p>
 * The rules are the {@link Tally}'s; this is its adapter to the player's attachment, the server's
 * tick, Discovery and the Bailiff's {@link Arrival}. A player in Creative or Spectator gains nothing,
 * atones for nothing, is never visited and is not watched for Warnings.
 */
public final class Wickedness {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfWickedness.MOD_ID);

    /** The Tally as it is saved with the player, and never sent to the client (ADR 0002). */
    private static final MapCodec<Tally> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("wickedness").forGetter(Tally::wickedness),
            ExtraCodecs.POSITIVE_INT.fieldOf("threshold").forGetter(Tally::threshold),
            Codec.LONG.optionalFieldOf("decay_carry", 0L).forGetter(Tally::decayCarry),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("gifts", 0).forGetter(Tally::gifts),
            Codec.LONG.optionalFieldOf("gift_day", 0L).forGetter(Tally::giftDay),
            Codec.LONG.optionalFieldOf("arrived_at", Tally.NEVER).forGetter(Tally::arrivedAt)
    ).apply(instance, Tally::new));

    private static final Supplier<AttachmentType<Tally>> TALLY = ATTACHMENTS.register("wickedness",
            () -> AttachmentType.builder(holder -> Tally.fresh(rules(), (holder instanceof Entity entity ? entity.getRandom() : RandomSource.create())::nextInt))
                    .serialize(CODEC)
                    .copyOnDeath()
                    .build());

    /** The world's clock as a player's Decay and gifts read it. */
    private static ToLongFunction<ServerPlayer> clock = player -> player.level().getOverworldClockTime();

    private Wickedness() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        // The Threshold is drawn on first join, so it is saved from then on.
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> event.getEntity().getData(TALLY));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            boolean retry = event.getServer().getTickCount() % WickednessConfig.ticksBetweenRetries() == 0;
            var rules = rules();
            event.getServer().getPlayerList().getPlayers().forEach(player -> tick(player, retry, rules));
        });
    }

    /** @return the player's Wickedness */
    public static int of(ServerPlayer player) {
        return player.getData(TALLY).wickedness();
    }

    /** @return the player's Threshold */
    public static int threshold(ServerPlayer player) {
        return player.getData(TALLY).threshold();
    }

    /** @return the player's Standing, the rough band of their Wickedness against their Threshold */
    public static Standing standing(ServerPlayer player) {
        return player.getData(TALLY).standing(rules());
    }

    /** Adds {@code amount}, which may be negative, to the player's Wickedness, never below 0. */
    public static void add(ServerPlayer player, int amount) {
        player.getData(TALLY).add(amount);
        visit(player, rules());
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
        player.getData(TALLY).set(value);
        visit(player, rules());
    }

    /** Sends the Bailiff once one is due and the player counts and is alive, and tells the Tally it arrived. */
    private static void visit(ServerPlayer player, Tally.Rules rules) {
        var tally = player.getData(TALLY);
        long now = player.level().getGameTime();
        if (counts(player) && player.isAlive() && tally.arrivalDue(now, rules) && Arrival.arrive(player)) {
            tally.arrived(now, rules, player.getRandom()::nextInt);
        }
    }

    /**
     * Leaves the player owing after a Bailiff that left with nothing: their Wickedness goes back up
     * to their Threshold, at the least, so another comes on a retry once the cooldown after this one
     * is over, never at once, as for a player logging out.
     */
    public static void owe(ServerPlayer player) {
        player.getData(TALLY).owe();
        Discoveries.owing(player);
    }

    /** Lowers the player's Wickedness by an Atonement of {@code amount}, unless the player is in Creative or Spectator. */
    public static void atone(ServerPlayer player, int amount) {
        if (counts(player) && player.getData(TALLY).atone(amount)) {
            Discoveries.atoned(player);
        }
    }

    /** Lowers the player's Wickedness by a gift's Atonement, while their gifts today are under the cap. */
    public static void gift(ServerPlayer player) {
        if (counts(player) && player.getData(TALLY).gift(clock.applyAsLong(player), rules())) {
            Discoveries.atoned(player);
        }
    }

    /**
     * Runs the player's Decay, and unless the player is in Creative or Spectator, on a retry tick
     * tries the Bailiff's arrival again and sounds any Warning due.
     */
    private static void tick(ServerPlayer player, boolean retry, Tally.Rules rules) {
        player.getData(TALLY).decay(clock.applyAsLong(player), rules);
        if (!counts(player)) {
            return;
        }
        if (retry) {
            visit(player, rules);
        }
        warn(player, rules);
    }

    /**
     * Sounds the player's Warning if one is due, and tells Discovery of each new Standing, the one a
     * player joins with included. A player in Creative or Spectator is not watched, so one whose
     * Standing rose meanwhile hears its Warning and discovers it on coming back.
     */
    private static void warn(ServerPlayer player, Tally.Rules rules) {
        var look = player.getData(TALLY).look(player.level().getGameTime(), rules);
        if (look.warns()) {
            WarningSounds.play(player, look.standing(), rules.warnings().of(look.standing()).distance());
        }
        if (look.changed()) {
            Discoveries.standing(player, look.standing());
        }
    }

    /** Sets the player's Threshold, at least 1, until the next visit draws another. */
    public static void setThreshold(ServerPlayer player, int threshold) {
        player.getData(TALLY).setThreshold(threshold);
    }

    /**
     * Has {@code clock} tell the world's clock as a player's Decay and gifts read it, in place of the
     * overworld's, for a run that sets its own clocks rather than read the world's one clock. Set
     * once, as the mod is built, before any player joins.
     */
    public static void tellClockBy(ToLongFunction<ServerPlayer> clock) {
        Wickedness.clock = clock;
    }

    /** @return the config's numbers for a Tally, read afresh so a reload applies at once */
    private static Tally.Rules rules() {
        var atonement = WickednessConfig.atonementRules();
        return new Tally.Rules(WickednessConfig.thresholdMin(), WickednessConfig.thresholdMax(), WickednessConfig.ticksBetweenVisits(),
                WickednessConfig.decayPerDay(), WickednessConfig.bands(), WickednessConfig.warningRules(), atonement.gift(), atonement.giftsPerDay());
    }
}

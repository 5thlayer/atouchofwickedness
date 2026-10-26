// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/**
 * A player's Wickedness and Threshold, as the server keeps them, with the Decay carried towards the
 * next point, the gifts that counted on their latest day of giving and when their last Bailiff
 * arrived: saved with the player, copied through death, and never sent to the client (ADR 0002).
 * The Standing last seen and the last Warning are kept only while the player is online.
 * {@link Wickedness} reads and changes it.
 */
final class Tally {

    /** No clock reading yet: Decay counts from the first tick the player is seen. */
    static final long UNSEEN = Long.MIN_VALUE;

    static final MapCodec<Tally> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("wickedness").forGetter(tally -> tally.wickedness),
            ExtraCodecs.POSITIVE_INT.fieldOf("threshold").forGetter(tally -> tally.threshold),
            Codec.LONG.optionalFieldOf("decay_carry", 0L).forGetter(tally -> tally.decayCarry),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("gifts", 0).forGetter(tally -> tally.gifts.count()),
            Codec.LONG.optionalFieldOf("gift_day", 0L).forGetter(tally -> tally.gifts.day()),
            Codec.LONG.optionalFieldOf("arrived_at", VisitCooldown.NEVER).forGetter(tally -> tally.arrivedAt)
    ).apply(instance, (wickedness, threshold, decayCarry, gifts, giftDay, arrivedAt) ->
            new Tally(wickedness, threshold, decayCarry, new Atonement.Gifts(gifts, giftDay), arrivedAt)));

    int wickedness;
    int threshold;
    long decayCarry;
    Atonement.Gifts gifts;
    /** The game time the player's last Bailiff arrived, or {@link VisitCooldown#NEVER}. */
    long arrivedAt;
    /** The world's clock when Decay last ran, never saved, so no Decay accrues while the player is away. */
    long decayedAt = UNSEEN;
    /** The player's Standing when last seen, never saved, so a player who joins Marked or worse hears a Warning at once. */
    Standing seen = Standing.CLEAN;
    /** The game time of the player's last Warning, never saved, or {@link Warning#NEVER}. */
    long warnedAt = Warning.NEVER;

    Tally(int wickedness, int threshold) {
        this(wickedness, threshold, 0, Atonement.Gifts.NONE, VisitCooldown.NEVER);
    }

    private Tally(int wickedness, int threshold, long decayCarry, Atonement.Gifts gifts, long arrivedAt) {
        this.wickedness = wickedness;
        this.threshold = threshold;
        this.decayCarry = decayCarry;
        this.gifts = gifts;
        this.arrivedAt = arrivedAt;
    }
}

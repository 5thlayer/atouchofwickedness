// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/**
 * A player's Wickedness and Threshold, as the server keeps them, with the Decay carried towards the
 * next point and the gifts that counted on their latest day of giving: saved with the player, copied
 * through death, and never sent to the client (ADR 0002). {@link Wickedness} reads and changes it.
 */
final class Tally {

    /** No clock reading yet: Decay counts from the first tick the player is seen. */
    static final long UNSEEN = Long.MIN_VALUE;

    static final MapCodec<Tally> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("wickedness").forGetter(tally -> tally.wickedness),
            ExtraCodecs.POSITIVE_INT.fieldOf("threshold").forGetter(tally -> tally.threshold),
            Codec.LONG.optionalFieldOf("decay_carry", 0L).forGetter(tally -> tally.decayCarry),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("gifts", 0).forGetter(tally -> tally.gifts.count()),
            Codec.LONG.optionalFieldOf("gift_day", 0L).forGetter(tally -> tally.gifts.day())
    ).apply(instance, (wickedness, threshold, decayCarry, gifts, giftDay) ->
            new Tally(wickedness, threshold, decayCarry, new Atonement.Gifts(gifts, giftDay))));

    int wickedness;
    int threshold;
    long decayCarry;
    Atonement.Gifts gifts;
    /** The world's clock when Decay last ran, never saved, so no Decay accrues while the player is away. */
    long decayedAt = UNSEEN;

    Tally(int wickedness, int threshold) {
        this(wickedness, threshold, 0, Atonement.Gifts.NONE);
    }

    private Tally(int wickedness, int threshold, long decayCarry, Atonement.Gifts gifts) {
        this.wickedness = wickedness;
        this.threshold = threshold;
        this.decayCarry = decayCarry;
        this.gifts = gifts;
    }
}

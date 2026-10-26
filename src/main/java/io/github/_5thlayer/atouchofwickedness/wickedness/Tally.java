// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/**
 * A player's Wickedness and Threshold, as the server keeps them: saved with the player, copied
 * through death, and never sent to the client (ADR 0002). {@link Wickedness} reads and changes it.
 */
final class Tally {

    static final MapCodec<Tally> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("wickedness").forGetter(tally -> tally.wickedness),
            ExtraCodecs.POSITIVE_INT.fieldOf("threshold").forGetter(tally -> tally.threshold)
    ).apply(instance, Tally::new));

    int wickedness;
    int threshold;

    Tally(int wickedness, int threshold) {
        this.wickedness = wickedness;
        this.threshold = threshold;
    }
}

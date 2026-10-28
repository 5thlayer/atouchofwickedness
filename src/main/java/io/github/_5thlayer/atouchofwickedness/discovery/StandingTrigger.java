// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.discovery;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofwickedness.wickedness.Standing;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * A trigger for the player's Standing, whose criterion names the Standing it waits for and is met by
 * that one or any above it: a player whose Wickedness leaps from Clean to Forfeit has passed Marked and
 * Overdue on the way.
 */
final class StandingTrigger extends SimpleCriterionTrigger<StandingTrigger.Instance> {

    /** A Standing by its name in lower case, such as {@code marked}. */
    private static final Codec<Standing> STANDING = Codec.STRING.comapFlatMap(
            name -> Arrays.stream(Standing.values()).filter(standing -> name(standing).equals(name)).findFirst()
                    .map(DataResult::success).orElseGet(() -> DataResult.error(() -> "No Standing " + name)),
            StandingTrigger::name);

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    private static String name(Standing standing) {
        return standing.name().toLowerCase(Locale.ROOT);
    }

    void trigger(ServerPlayer player, Standing standing) {
        trigger(player, instance -> standing.compareTo(instance.standing()) >= 0);
    }

    record Instance(Optional<ContextAwarePredicate> player, Standing standing) implements SimpleCriterionTrigger.SimpleInstance {

        static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                STANDING.fieldOf("standing").forGetter(Instance::standing)
        ).apply(instance, Instance::new));
    }
}

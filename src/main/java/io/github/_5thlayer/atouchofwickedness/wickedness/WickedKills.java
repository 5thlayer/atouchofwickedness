// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.wickedness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * A Wicked kill: a creature's death credited to a player adds its weight to the player's Wickedness,
 * and each damaging hit a player lands on a creature weighing enough adds the hit cost. A kill is
 * credited as vanilla credits its experience, to the player who last hurt the creature within its
 * memory of them, through a projectile or a trap they knocked it into too, or to the owner of the
 * tamed pet that did; a kill no player had a hand in counts for no one. Creative and Spectator
 * players gain nothing. A neutral mob killed in self-defence weighs nothing, but only when it went
 * for the player before the player struck it: a mob remembers each player who struck it first.
 *
 * <p>The weight is the creature type's entry in the data map {@code atouchofwickedness:wickedness_weight},
 * and nothing when it has none, so a pack can weigh any creature, a modded one included; the rules of
 * {@link WickednessWeight} weigh what belongs to the creature rather than its type.
 */
public final class WickedKills {

    public static final Identifier WEIGHT_ID = Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "wickedness_weight");

    /** What a Wicked kill of each creature type adds; unlisted types add nothing. */
    public static final DataMapType<EntityType<?>, Integer> WEIGHT =
            DataMapType.builder(WEIGHT_ID, Registries.ENTITY_TYPE, ExtraCodecs.NON_NEGATIVE_INT).build();

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ATouchOfWickedness.MOD_ID);

    /** The players who struck a neutral mob before it went for them, so killing it is no self-defence. */
    private static final Supplier<AttachmentType<List<UUID>>> STRUCK_FIRST_BY = ATTACHMENTS.register("struck_first_by",
            () -> AttachmentType.<List<UUID>>builder(() -> List.of())
                    .serialize(UUIDUtil.CODEC.listOf().fieldOf("players"), players -> !players.isEmpty())
                    .build());

    private WickedKills() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(WEIGHT));
        NeoForge.EVENT_BUS.addListener(WickedKills::onDeath);
        NeoForge.EVENT_BUS.addListener(WickedKills::onHit);
    }

    /** @return what a Wicked kill of a creature of {@code type} adds, before the rules on top of the data map */
    public static int weightOf(EntityType<?> type) {
        return WickednessWeight.of(type.builtInRegistryHolder().getData(WEIGHT));
    }

    /** @return what killing {@code creature} adds to {@code player}'s Wickedness, once the rules are applied */
    private static int effectiveWeightOf(LivingEntity creature, ServerPlayer player) {
        var victim = new WickednessWeight.Victim(tamed(creature), creature.isBaby(), selfDefence(creature, player));
        return WickednessWeight.effective(weightOf(creature.getType()), victim, WickednessConfig.weightRules());
    }

    private static void onDeath(LivingDeathEvent event) {
        var creature = event.getEntity();
        // Within vanilla's memory of the player, the window in which it credits them the experience.
        if (creature.level().isClientSide() || creature.getLastHurtByPlayerMemoryTime() <= 0
                || !(creature.getLastHurtByPlayer() instanceof ServerPlayer player) || !Wickedness.counts(player)) {
            return;
        }
        int weight = effectiveWeightOf(creature, player);
        if (weight > 0) {
            Wickedness.add(player, weight);
        }
    }

    private static void onHit(LivingDamageEvent.Post event) {
        var creature = event.getEntity();
        // The killing blow is the kill's, not a hit's: a quick kill adds the weight alone.
        if (event.getHealthDamage() <= 0 || creature.isDeadOrDying()
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || player == creature) {
            return;
        }
        if (creature instanceof NeutralMob && creature instanceof Mob mob && mob.getTarget() != player) {
            struckFirst(creature, player);
        }
        if (!Wickedness.counts(player)) {
            return;
        }
        int cost = WickednessWeight.hitCost(effectiveWeightOf(creature, player), WickednessConfig.weightRules());
        if (cost > 0) {
            Wickedness.add(player, cost);
        }
    }

    private static boolean tamed(LivingEntity creature) {
        if (creature instanceof TamableAnimal animal) {
            return animal.isTame();
        }
        // A horse, a donkey, a llama: tamed once someone owns it.
        return creature instanceof OwnableEntity ownable && ownable.getOwnerReference() != null;
    }

    /**
     * Whether killing {@code creature} is {@code player}'s self-defence: it is a neutral mob going for
     * them, which they had not struck before it did. Never an iron golem, which turns on a player only
     * for what the player did.
     */
    private static boolean selfDefence(LivingEntity creature, ServerPlayer player) {
        return creature instanceof NeutralMob && !(creature instanceof IronGolem)
                && creature instanceof Mob mob && mob.getTarget() == player
                && !creature.getData(STRUCK_FIRST_BY).contains(player.getUUID());
    }

    private static void struckFirst(LivingEntity creature, ServerPlayer player) {
        var players = creature.getData(STRUCK_FIRST_BY);
        if (!players.contains(player.getUUID())) {
            var more = new ArrayList<>(players);
            more.add(player.getUUID());
            creature.setData(STRUCK_FIRST_BY, List.copyOf(more));
        }
    }
}

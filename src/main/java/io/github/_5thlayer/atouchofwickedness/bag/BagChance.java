// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bag;

import java.util.Set;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The loot condition {@code atouchofwickedness:bag_chance}, on which the Bailiff's loot table drops a
 * Bailiff's Bag: vanilla's {@code random_chance_with_enchanted_bonus} with Looting, its chance and its
 * bonus for each level of Looting read from the common config, which a loot table can't name.
 */
public enum BagChance implements LootItemCondition {
    INSTANCE;

    private static final MapCodec<BagChance> CODEC = MapCodec.unit(INSTANCE);

    private static final DeferredRegister<MapCodec<? extends LootItemCondition>> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, ATouchOfWickedness.MOD_ID);

    static {
        CONDITIONS.register("bag_chance", () -> CODEC);
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    @Override
    public MapCodec<BagChance> codec() {
        return CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ATTACKING_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        float chance = WickednessConfig.bagChance();
        float perLevel = WickednessConfig.bagChancePerLootingLevel();
        var looting = context.getResolver().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        return new LootItemRandomChanceWithEnchantedBonusCondition(chance, new LevelBasedValue.Linear(chance + perLevel, perLevel), looting)
                .test(context);
    }
}

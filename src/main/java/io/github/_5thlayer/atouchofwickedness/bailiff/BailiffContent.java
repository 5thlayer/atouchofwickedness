// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Bailiff, as a creature, and the sound of its arrival. */
public final class BailiffContent {

    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ATouchOfWickedness.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfWickedness.MOD_ID);

    /** A wandering trader's size. Never saved: a visit ends with the server at the latest. */
    public static final DeferredHolder<EntityType<?>, EntityType<Bailiff>> BAILIFF = ENTITIES.registerEntityType("bailiff",
            Bailiff::new, MobCategory.MISC, builder -> builder.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).noSave());

    /** Heard by everyone near the player a Bailiff arrives for; its subtitle reads "Bailiff arrives". */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARRIVES = SOUNDS.register("entity.bailiff.arrives",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "entity.bailiff.arrives")));

    private BailiffContent() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BAILIFF.get(), Bailiff.createAttributes().build()));
    }
}

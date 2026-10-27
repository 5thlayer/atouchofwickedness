// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import io.github._5thlayer.atouchofwickedness.bailiff.BailiffContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * The Bailiff is in A Touch of Madness's {@code unsettling} tag, added by a tag file of optional
 * entries that loads on a server without Madness (suite ADR-0002). That Madness then drains Sanity
 * near it is tried by hand, with Madness in the dev client.
 */
final class UnsettlingTests {

    private static final TagKey<EntityType<?>> UNSETTLING =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("atouchofmadness", "unsettling"));

    private UnsettlingTests() {
    }

    static void register(ATouchOfWickednessGameTests.Registrar tests) {
        tests.test("unsettling_holds_the_bailiff_without_madness", 1, UnsettlingTests::holdsTheBailiff);
    }

    private static void holdsTheBailiff(GameTestHelper helper) {
        helper.assertTrue(BailiffContent.BAILIFF.get().builtInRegistryHolder().is(UNSETTLING),
                "the bailiff is not in #atouchofmadness:unsettling");
        helper.succeed();
    }
}

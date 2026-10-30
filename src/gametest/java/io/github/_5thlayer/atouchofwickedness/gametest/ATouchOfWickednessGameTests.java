// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Module's game tests, run by the {@code gameTestServer} Gradle run: the Minecraft-integration
 * seam, with a real player on a real server. Each test stands on the {@code gametest/platform}
 * structure, a stone floor that {@code scripts/build-gametest-structures.py} writes, and sets up
 * what it needs itself, so the setup is in the diff. It lives in the {@code gametest} source set, which
 * only the Module's own runs load, and hooks itself in, so the {@code @Mod} class names no game-test
 * code and nothing gates it (libworks ADR 0011).
 */
@EventBusSubscriber(modid = ATouchOfWickedness.MOD_ID)
public final class ATouchOfWickednessGameTests {

    private static final Identifier PLATFORM = id("gametest/platform");

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, ATouchOfWickedness.MOD_ID);

    static {
        TEST_TYPES.register("code", () -> CodeGameTest.CODEC);
    }

    private ATouchOfWickednessGameTests() {
    }

    @SubscribeEvent
    static void construct(FMLConstructModEvent event) {
        register(event.getContainer().getEventBus());
        // The dev client loads this source set too, and keeps the day's own nights and clock.
        if ("gameTestServer".equals(System.getProperty("libworks.run"))) {
            Nights.install();
            Clocks.install();
        }
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        // Posted only when game tests are enabled, which a production game never is.
        modBus.addListener(ATouchOfWickednessGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        // Registered rather than borrowed, since the event hands out no lookup for vanilla's.
        var environment = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        var alone = event.registerEnvironment(id("alone"), new TestEnvironmentDefinition.AllOf(List.of()));
        var peaceful = event.registerEnvironment(id("peaceful"),
                new TestEnvironmentDefinition.Functions(Optional.of(id("peaceful")), Optional.of(id("normal"))));
        var tests = new Registrar(event, environment, alone, peaceful);
        LoadTests.register(tests);
        CreativeTabTests.register(tests);
        WickedKillTests.register(tests);
        BlameTests.register(tests);
        ThresholdTests.register(tests);
        ArrivalTests.register(tests);
        TheftTests.register(tests);
        RobberyTests.register(tests);
        FleeTests.register(tests);
        DropTests.register(tests);
        BagTests.register(tests);
        TallyStickTests.register(tests);
        StrongboxTests.register(tests);
        CommandTests.register(tests);
        WayBackTests.register(tests);
        ReportTests.register(tests);
        WarningTests.register(tests);
        SleepTests.register(tests);
        HarderWorldTests.register(tests);
        ValueTests.register(tests);
        GossipTests.register(tests);
        UnsettlingTests.register(tests);
        DiscoveryTests.register(tests);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, path);
    }

    /**
     * What a test class is handed: a name, a tick budget and a body per test. A batch's tests run
     * together, and the batches one after another, each of its own environment.
     */
    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            Holder<TestEnvironmentDefinition<?>> alone, Holder<TestEnvironmentDefinition<?>> peaceful) {

        void test(String name, int maxTicks, Consumer<GameTestHelper> body) {
            register(name, maxTicks, environment, body);
        }

        /** A test that runs alone, after the others, such as one that moves the world's clock, which every test's players read. */
        void testAlone(String name, int maxTicks, Consumer<GameTestHelper> body) {
            register(name, maxTicks, alone, body);
        }

        /** A test that runs on Peaceful, in a batch of its own, back on Normal once it ends. */
        void testOnPeaceful(String name, int maxTicks, Consumer<GameTestHelper> body) {
            register(name, maxTicks, peaceful, body);
        }

        private void register(String name, int maxTicks, Holder<TestEnvironmentDefinition<?>> batch, Consumer<GameTestHelper> body) {
            var id = id(name);
            CodeGameTest.define(id, body);
            event.registerTest(id, new CodeGameTest(id, new TestData<>(batch, PLATFORM, maxTicks, 0, true, Rotation.NONE)));
        }
    }
}

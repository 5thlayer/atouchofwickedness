// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

/**
 * A real survival player on the test's server, joined as a client would join, so a Bailiff can find
 * them and the Pillar draws their Threshold. Each test's player leaves when the test ends.
 */
final class Players {

    private Players() {
    }

    /** A player who keeps the action-bar messages and the sounds they are sent. */
    static final class Joined extends ServerPlayer {

        final List<Component> actionBar = new ArrayList<>();
        final List<Holder<SoundEvent>> heard = new ArrayList<>();

        Joined(GameTestHelper helper, GameProfile profile) {
            super(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
        }

        @Override
        public void sendOverlayMessage(Component message) {
            actionBar.add(message);
            super.sendOverlayMessage(message);
        }
    }

    /** Joins a new survival player, standing at {@code relative} in the test's structure. */
    static Joined join(GameTestHelper helper, BlockPos relative) {
        var server = helper.getLevel().getServer();
        // Names are at most 16 characters, and each player's own, so a selector finds only them.
        var profile = new GameProfile(UUID.randomUUID(), "w" + UUID.randomUUID().toString().substring(0, 12));
        var player = new Joined(helper, profile);
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
                if (packet instanceof ClientboundSoundPacket sound) {
                    player.heard.add(sound.getSound());
                }
                super.send(packet, listener, flush);
            }
        };
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        // As a client does once it has loaded the world; until then the player cannot be hurt.
        player.connection.markClientLoaded();
        player.setGameMode(GameType.SURVIVAL);
        var feet = helper.absoluteVec(relative.getBottomCenter());
        player.teleportTo(helper.getLevel(), feet.x, feet.y, feet.z, Set.of(), 0, 0, true);
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
                leave();
            }

            @Override
            public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
                leave();
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }

            private void leave() {
                // Whoever the player is now: respawning replaces them with a new one.
                var current = server.getPlayerList().getPlayer(profile.id());
                if (current != null) {
                    server.getPlayerList().remove(current);
                }
            }
        });
        return player;
    }

    /**
     * Puts {@code stacks} in the first empty slots of the player's main inventory and hotbar, never
     * the held one, so each is a stack a Bailiff's hit can take.
     */
    static void stock(ServerPlayer player, ItemStack... stacks) {
        var inventory = player.getInventory();
        int slot = 0;
        for (var stack : stacks) {
            while (slot == inventory.getSelectedSlot() || !inventory.getItem(slot).isEmpty()) {
                slot++;
            }
            inventory.setItem(slot, stack);
        }
    }

    /** Lets the player be hurt again at once, as the hurt cooldown a ticking player sheds would, and heals them. */
    static void recover(ServerPlayer player) {
        player.invulnerableTime = 0;
        player.setHealth(player.getMaxHealth());
    }
}

// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * A Bailiff's arrival: one comes for a player, unless one already has, on a block it can stand on
 * 12 to 24 blocks away across the ground, in the player's dimension, with a sound everyone near the
 * spot hears.
 */
public final class Arrival {

    /** Spots tried before the visit is given up for now. */
    private static final int TRIES = 32;

    /** How far above and below the player a spot is looked for. */
    private static final int REACH = 8;

    private Arrival() {
    }

    /** Sends a Bailiff for {@code player}. @return whether one arrived */
    public static boolean arrive(ServerPlayer player) {
        var level = player.level();
        if (hasOne(level, player)) {
            return false;
        }
        var spot = spot(level, player);
        if (spot == null) {
            return false;
        }
        var bailiff = BailiffContent.BAILIFF.get().create(level, EntitySpawnReason.EVENT);
        if (bailiff == null) {
            return false;
        }
        bailiff.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getRandom().nextFloat() * 360, 0);
        bailiff.comeFor(player);
        level.addFreshEntity(bailiff);
        // Loud enough to carry the whole way to its player.
        level.playSound(null, bailiff.getX(), bailiff.getY(), bailiff.getZ(), BailiffContent.ARRIVES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        return true;
    }

    private static boolean hasOne(ServerLevel level, ServerPlayer player) {
        return !level.getEntities(BailiffContent.BAILIFF.get(), bailiff -> bailiff.player().filter(player.getUUID()::equals).isPresent()).isEmpty();
    }

    private static @Nullable BlockPos spot(ServerLevel level, ServerPlayer player) {
        var random = player.getRandom();
        int min = WickednessConfig.arrivalMinDistance();
        int max = WickednessConfig.arrivalMaxDistance();
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.TAU;
            double distance = min + random.nextDouble() * (max - min);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            double across = Math.hypot(x + 0.5 - player.getX(), z + 0.5 - player.getZ());
            if (across < min || across > max) {
                continue;
            }
            var spot = standable(level, x, player.getBlockY(), z);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    /** @return the standable spot in the column at {@code x}, {@code z} nearest {@code y}, or null */
    private static @Nullable BlockPos standable(ServerLevel level, int x, int y, int z) {
        var type = BailiffContent.BAILIFF.get();
        for (int offset = 0; offset <= REACH * 2; offset++) {
            // 0, +1, -1, +2, -2, ...
            int dy = (offset + 1) / 2 * (offset % 2 == 0 ? -1 : 1);
            var pos = new BlockPos(x, y + dy, z);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                return null;
            }
            var below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getFluidState(pos).isEmpty()
                    && level.noCollision(type.getSpawnAABB(x + 0.5, pos.getY(), z + 0.5))) {
                return pos;
            }
        }
        return null;
    }
}

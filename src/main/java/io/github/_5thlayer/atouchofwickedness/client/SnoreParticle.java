// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** A "Z" rising from a sleeping Bailiff, drifting a little as it goes, then fading out. */
final class SnoreParticle extends SingleQuadParticle {

    private SnoreParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, 0, 0, 0, sprite);
        this.xd = (random.nextDouble() - 0.5) * 0.01;
        this.yd = 0.02;
        this.zd = (random.nextDouble() - 0.5) * 0.01;
        this.friction = 1.0F;
        this.quadSize *= 1.5F;
        this.lifetime = 40;
        this.hasPhysics = false;
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Gone in the last quarter of its life.
        setAlpha(Mth.clamp((lifetime - age) / (lifetime / 4.0F), 0, 1));
    }

    @Override
    public float getQuadSize(float partialTicks) {
        // Grows as it rises, from half its size.
        return quadSize * (0.5F + 0.5F * Mth.clamp((age + partialTicks) / lifetime, 0, 1));
    }

    static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xAux, double yAux, double zAux, RandomSource random) {
            return new SnoreParticle(level, x, y, z, sprites.get(random));
        }
    }
}

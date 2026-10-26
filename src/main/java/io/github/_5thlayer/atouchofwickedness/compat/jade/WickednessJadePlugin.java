// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.compat.jade;

import io.github._5thlayer.atouchofwickedness.ATouchOfWickedness;
import io.github._5thlayer.atouchofwickedness.bailiff.Bailiff;
import io.github._5thlayer.atouchofwickedness.bailiff.BailiffReport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

/**
 * A Bailiff in Jade: beside the name and hearts Jade shows of any creature, the player it came for
 * and the stacks it has taken, read on the server as its {@link BailiffReport}. Nothing of any
 * player's Wickedness, Threshold or Standing, which no tooltip shows (ADR 0002).
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency.
 */
@WailaPlugin
public final class WickednessJadePlugin implements IWailaPlugin {

    private static final Identifier BAILIFF = Identifier.fromNamespaceAndPath(ATouchOfWickedness.MOD_ID, "bailiff");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(Server.INSTANCE, Bailiff.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(Client.INSTANCE, Bailiff.class);
    }

    /** The server's half: a Bailiff's report, sent with its codec. Names no client type. */
    private enum Server implements StreamServerDataProvider<EntityAccessor, BailiffReport> {
        INSTANCE;

        @Override
        public @Nullable BailiffReport streamData(EntityAccessor accessor) {
            return accessor.getEntity() instanceof Bailiff bailiff && bailiff.level() instanceof ServerLevel level ? bailiff.report(level) : null;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BailiffReport> streamCodec() {
            return BailiffReport.STREAM_CODEC;
        }

        @Override
        public Identifier getUid() {
            return BAILIFF;
        }
    }

    /** The client's half: the player it came for on one line, and on the next what it carries as icons. Client-only. */
    private enum Client implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Server.INSTANCE.decodeFromData(accessor).ifPresent(report -> {
                tooltip.add(Component.translatable("atouchofwickedness.jade.came_for", report.player()));
                if (!report.carried().isEmpty()) {
                    tooltip.add(JadeUI.text(Component.translatable("atouchofwickedness.jade.carries")).alignSelfCenter());
                    report.carried().forEach(stack -> tooltip.append(JadeUI.item(stack)));
                }
            });
        }

        @Override
        public Identifier getUid() {
            return BAILIFF;
        }
    }
}

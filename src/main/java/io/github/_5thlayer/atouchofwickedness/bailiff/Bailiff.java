// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.WickednessItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The creature that comes for a player whose Wickedness reached their Threshold, to rob them rather
 * than kill them (ADR 0004). It goes for its player, and each of its hits that lands takes one stack
 * at random from their main inventory or hotbar, never the held item, their worn armour or off
 * hand. Once it has taken its quota, or its time on site is up, it flees faster than a sprinting
 * player, and a few seconds out of its player's sight it vanishes with what it carries. Killed, it
 * drops everything it stole and a few Seals. It is never saved, so a visit never outlasts the server.
 */
public final class Bailiff extends PathfinderMob {

    private @Nullable UUID player;
    private final List<ItemStack> stolen = new ArrayList<>();
    private final Quota quota = new Quota(WickednessConfig.quota());
    private int ticksOnSite;
    private int ticksUnseen;
    private boolean fleeing;

    public Bailiff(EntityType<? extends Bailiff> type, Level level) {
        super(type, level);
    }

    static AttributeSupplier.Builder createAttributes() {
        // The config's numbers replace these as the Bailiff arrives (takeStats).
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Flee());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16));
    }

    /** Sends the Bailiff for {@code target}, with its stats from the config, as it arrives. */
    public void comeFor(ServerPlayer target) {
        player = target.getUUID();
        takeStats();
    }

    private void takeStats() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(WickednessConfig.bailiffHealth());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(WickednessConfig.bailiffDamage());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(WickednessConfig.bailiffKnockbackResistance());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(WickednessConfig.bailiffSpeed());
        setHealth(getMaxHealth());
    }

    /** @return whether the Bailiff came for {@code target} */
    public boolean cameFor(ServerPlayer target) {
        return target.getUUID().equals(player);
    }

    /** @return what the Bailiff shows a viewer, or null once its player has gone */
    public @Nullable BailiffReport report(ServerLevel level) {
        var target = target(level);
        return target == null ? null : new BailiffReport(target.getDisplayName(), stolen.stream().map(ItemStack::copy).toList());
    }

    /** @return whether the Bailiff is done and running off with what it took */
    public boolean isFleeing() {
        return fleeing;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        var target = target(level);
        if (target == null) {
            // Its player left, or left the dimension: the visit is over.
            discard();
            return;
        }
        if (!fleeing) {
            setTarget(target);
            if (++ticksOnSite >= WickednessConfig.ticksOnSite()) {
                flee();
            }
            return;
        }
        ticksUnseen = target.hasLineOfSight(this) ? 0 : ticksUnseen + 1;
        if (ticksUnseen >= WickednessConfig.ticksToVanish()) {
            // It vanishes with what it carries.
            discard();
        }
    }

    private @Nullable ServerPlayer target(ServerLevel level) {
        if (player == null) {
            return null;
        }
        var target = level.getPlayerByUUID(player);
        return target instanceof ServerPlayer serverPlayer && serverPlayer.isAlive() ? serverPlayer : null;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean landed = super.doHurtTarget(level, target);
        if (landed && !fleeing && target instanceof ServerPlayer hit && cameFor(hit)) {
            steal(hit);
        }
        return landed;
    }

    /**
     * Takes one stack at random from the main inventory or hotbar, never the held item: the one in
     * the selected slot as the hit lands, so the player keeps what they fight with.
     */
    private void steal(ServerPlayer target) {
        var inventory = target.getInventory();
        var items = inventory.getNonEquipmentItems();
        var filled = new ArrayList<Integer>();
        for (int slot = 0; slot < items.size(); slot++) {
            if (slot != inventory.getSelectedSlot() && !items.get(slot).isEmpty()) {
                filled.add(slot);
            }
        }
        if (filled.isEmpty()) {
            return;
        }
        int slot = filled.get(getRandom().nextInt(filled.size()));
        stolen.add(inventory.removeItemNoUpdate(slot));
        inventory.setChanged();
        quota.take();
        if (quota.isMet()) {
            flee();
        }
    }

    private void flee() {
        fleeing = true;
        setTarget(null);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        // The players' own goods, so they drop whatever the mob_drops rule says, as a player's do on death.
        stolen.forEach(stack -> spawnAtLocation(level, stack));
        stolen.clear();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int seals = WickednessConfig.sealsMin() + getRandom().nextInt(WickednessConfig.sealsMax() - WickednessConfig.sealsMin() + 1);
        if (seals > 0) {
            spawnAtLocation(level, new ItemStack(WickednessItems.SEAL.get(), seals));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    /** Runs from its player, faster than they can sprint. */
    private final class Flee extends Goal {

        Flee() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeing && level() instanceof ServerLevel level && target(level) != null;
        }

        @Override
        public void tick() {
            if (!getNavigation().isDone() || !(level() instanceof ServerLevel level)) {
                return;
            }
            var from = target(level);
            var away = from == null ? null : DefaultRandomPos.getPosAway(Bailiff.this, 24, 7, from.position());
            if (away != null) {
                getNavigation().moveTo(away.x, away.y, away.z, WickednessConfig.bailiffFleeSpeed());
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}

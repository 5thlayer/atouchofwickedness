// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.IntPredicate;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.WickednessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The creature that comes for a player whose Wickedness reached their Threshold, to rob them rather
 * than kill them (ADR 0004). First it robs the Seizable containers its player opened that are near
 * where it arrived, nearest first: it opens each as a player would, lid and sound, and takes one
 * stack at random from it every half second until it is empty (ADR 0003). Then it goes for its
 * player, and each of its hits that lands takes one stack at random from their main inventory or
 * hotbar, never the held item, their worn armour or off hand. A player who hits it while it robs
 * turns it on them at once. Once it has taken its quota, or its time on site is up, it flees faster
 * than a sprinting player, and a few seconds out of its player's sight it vanishes with what it
 * carries. Killed, it drops everything it stole and a few Seals. It is never saved, so a visit never
 * outlasts the server.
 */
public final class Bailiff extends PathfinderMob implements ContainerUser {

    /** How near, in blocks, the Bailiff robs a container from: the centre of the block to its feet. */
    private static final double REACH = 2.5;

    /** Ticks between the stacks the Bailiff takes from a container it has open. */
    private static final int TICKS_PER_STACK = 10;

    /** Ticks the Bailiff tries to reach a container before it gives up on it, as one walled off. */
    private static final int TICKS_TO_REACH = 200;

    private @Nullable UUID player;
    /** The containers still to rob, its player's, near where it arrived. */
    private final List<BlockPos> toRob = new ArrayList<>();
    /** The container it has open, or null. */
    private @Nullable BlockPos robbing;
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
        goalSelector.addGoal(2, new Rob());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16));
    }

    /**
     * Sends the Bailiff for {@code target}, with its stats from the config, as it arrives where it
     * stands, to rob first the containers they opened within the robbing radius of here.
     */
    public void comeFor(ServerPlayer target) {
        player = target.getUUID();
        takeStats();
        var arrival = blockPosition();
        long radius = WickednessConfig.robbingRadius();
        toRob.clear();
        Seizable.opened(target, target.level()).stream()
                .filter(pos -> pos.distSqr(arrival) <= radius * radius)
                .forEach(toRob::add);
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

    /** @return whether the Bailiff is still robbing its player's containers, before it goes for them */
    public boolean isRobbing() {
        return !fleeing && !toRob.isEmpty();
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
            if (!isRobbing()) {
                setTarget(target);
            }
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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && isRobbing() && source.getEntity() instanceof ServerPlayer hitter && cameFor(hitter)) {
            // Caught at it, it leaves the rest of the containers and turns on its player.
            toRob.clear();
        }
        return hurt;
    }

    /**
     * Takes one stack at random from the main inventory or hotbar, never the held item: the one in
     * the selected slot as the hit lands, so the player keeps what they fight with.
     */
    private void steal(ServerPlayer target) {
        var inventory = target.getInventory();
        take(inventory, inventory.getNonEquipmentItems().size(), slot -> slot == inventory.getSelectedSlot());
    }

    /**
     * Takes one stack at random from the first {@code slots} of {@code container}, but none that
     * {@code kept} keeps, towards the quota, and flees once the quota is met.
     *
     * @return whether there was a stack to take
     */
    private boolean take(Container container, int slots, IntPredicate kept) {
        var filled = new ArrayList<Integer>();
        for (int slot = 0; slot < slots; slot++) {
            if (!kept.test(slot) && !container.getItem(slot).isEmpty()) {
                filled.add(slot);
            }
        }
        if (filled.isEmpty()) {
            return false;
        }
        int slot = filled.get(getRandom().nextInt(filled.size()));
        stolen.add(container.removeItemNoUpdate(slot));
        container.setChanged();
        quota.take();
        if (quota.isMet()) {
            flee();
        }
        return true;
    }

    @Override
    public boolean hasContainerOpen(ContainerOpenersCounter container, BlockPos pos) {
        if (robbing == null) {
            return false;
        }
        // Either half of a double chest, as a copper golem sees it.
        var state = level().getBlockState(robbing);
        return robbing.equals(pos)
                || state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                        && ChestBlock.getConnectedBlockPos(robbing, state).equals(pos);
    }

    @Override
    public double getContainerInteractionRange() {
        return REACH;
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

    /**
     * Robs its player's containers, the nearest first: walks to it, opens it and takes a stack from
     * it every half second until it is empty, then moves on to the next. One it can't reach in ten
     * seconds, or that is no longer there, it leaves.
     */
    private final class Rob extends Goal {

        private @Nullable BlockPos next;
        private int ticks;

        Rob() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isRobbing();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            // Its ticks count game ticks: half a second a stack, ten to reach a container.
            return true;
        }

        @Override
        public void tick() {
            // Ticked on the off ticks too, before the selector checks it should go on: done, or
            // turned on its player, it may have nothing left to rob.
            if (!isRobbing() || !(level() instanceof ServerLevel level)) {
                return;
            }
            if (next == null) {
                next = toRob.stream().min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(position()))).orElseThrow();
                ticks = 0;
            }
            var container = Seizable.at(level, next);
            if (container == null) {
                leave();
                return;
            }
            getLookControl().setLookAt(Vec3.atCenterOf(next));
            ticks++;
            if (robbing == null) {
                walkTo(container);
            } else if (ticks % TICKS_PER_STACK == 0 && !take(container, container.getContainerSize(), slot -> false)) {
                leave();
            }
        }

        private void walkTo(Container container) {
            if (next.closerToCenterThan(position(), REACH)) {
                getNavigation().stop();
                robbing = next;
                container.startOpen(Bailiff.this);
                ticks = 0;
                return;
            }
            if (ticks > TICKS_TO_REACH) {
                leave();
                return;
            }
            if (getNavigation().isDone()) {
                // No path yet, as before it has landed, it tries again until it gives up.
                getNavigation().moveTo(getNavigation().createPath(next, 1), 1.0);
            }
        }

        /** Done with the container it was robbing or making for, it closes it and drops it from its list. */
        private void leave() {
            close();
            toRob.remove(next);
            next = null;
        }

        private void close() {
            if (robbing != null && level() instanceof ServerLevel level) {
                var container = Seizable.at(level, robbing);
                if (container != null) {
                    container.stopOpen(Bailiff.this);
                }
            }
            robbing = null;
        }

        @Override
        public void stop() {
            close();
            next = null;
            getNavigation().stop();
        }
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

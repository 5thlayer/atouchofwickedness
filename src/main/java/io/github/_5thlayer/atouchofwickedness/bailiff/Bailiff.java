// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness.bailiff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.github._5thlayer.atouchofwickedness.WickednessConfig;
import io.github._5thlayer.atouchofwickedness.WickednessItems;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
import net.minecraft.world.entity.player.Inventory;
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
 * than kill them (ADR 0004). With Seizable containers its player opened near where it arrived, it
 * robs the base: it walks to the nearest, opens it as a player would, lid and sound, and takes one
 * stack at random from it, or moves on to the next when it is empty (ADR 0003). That one stack is
 * its haul: it walks up to its player, shakes its head at them mocking, and flees. With nothing to
 * rob it mugs its player instead: it goes for them, and its first hit that lands takes one stack at
 * random from their main inventory or hotbar, never the held item, their worn armour or off hand,
 * and it goes to taunt them. A player with nothing it may take it taunts at once, empty-handed.
 * Arriving at night, it lies down where it arrived and sleeps until dawn under rising Zs, harmless:
 * the first hit on it deals double damage and wakes it, and its time on site starts as it wakes. A
 * player who hits it as it robs or taunts sends it fleeing at once. Once its time on site is up it
 * flees whatever it has. Fleeing, it runs faster than a sprinting player, and once far enough from
 * them, or after a while however far it got, it stands still charging a teleport, when its player
 * can still kill it from afar, then vanishes with what it carries; one that vanishes with nothing
 * leaves its player owing ({@link Wickedness#owe}). Killed, it drops everything it stole and a few
 * Seals. It is never saved, so a visit never outlasts the server.
 */
public final class Bailiff extends PathfinderMob implements ContainerUser {

    /** How near, in blocks, the Bailiff robs a container from: the centre of the block to its feet. */
    private static final double REACH = 2.5;

    /** Ticks the Bailiff holds a container open before it takes its stack. */
    private static final int TICKS_TO_TAKE = 10;

    /** Ticks the Bailiff tries to reach a container or its player before it gives up, as on one walled off. */
    private static final int TICKS_TO_REACH = 200;

    /** How near, in blocks, the Bailiff comes to its player to taunt them. */
    private static final double TAUNT_DISTANCE = 3;

    /** Ticks the Bailiff taunts its player for before it flees. */
    private static final int TICKS_TAUNTING = 40;

    /** Ticks a fleeing Bailiff runs for before it charges its teleport wherever it got to, as when cornered. */
    private static final int TICKS_TO_GET_AWAY = 400;

    /** Ticks between the Zs a sleeping Bailiff sleeps under. */
    private static final int TICKS_BETWEEN_SNORES = 20;

    /** Whether the Bailiff is shaking its head at its player, which its renderer draws. */
    private static final EntityDataAccessor<Boolean> DATA_TAUNTING = SynchedEntityData.defineId(Bailiff.class, EntityDataSerializers.BOOLEAN);

    /** Whether the Bailiff is asleep, which its renderer draws lying down. */
    private static final EntityDataAccessor<Boolean> DATA_ASLEEP = SynchedEntityData.defineId(Bailiff.class, EntityDataSerializers.BOOLEAN);

    /** What the Bailiff is about: robbing the base then taunting, or mugging its player, then fleeing. */
    private enum Phase {
        ROBBING, TAUNTING, MUGGING, FLEEING
    }

    /** Tells whether it is night where a Bailiff stands: the day's timeline, unless a run sets its own. */
    private static Predicate<Bailiff> night = Bailiff::isNightByTheTimeline;

    private @Nullable UUID player;
    private Phase phase = Phase.MUGGING;
    /** The containers still to try, its player's, near where it arrived. */
    private final List<BlockPos> toRob = new ArrayList<>();
    /** The container it has open, or null. */
    private @Nullable BlockPos robbing;
    private final List<ItemStack> stolen = new ArrayList<>();
    private int ticksOnSite;
    private int ticksFleeing;
    /** Ticks it has stood charging its teleport, or -1 before it starts. */
    private int ticksCharging = -1;

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
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TAUNTING, false);
        entityData.define(DATA_ASLEEP, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Flee());
        goalSelector.addGoal(1, new Sleep());
        goalSelector.addGoal(2, new Rob());
        goalSelector.addGoal(2, new Taunt());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16));
    }

    /**
     * Sends the Bailiff for {@code target}, with its stats from the config, as it arrives where it
     * stands: to rob the containers they opened within the robbing radius of here, or with none to
     * mug them, and at night to sleep here first.
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
        if (!toRob.isEmpty()) {
            phase = Phase.ROBBING;
        } else if (!hasTakeable(target.getInventory())) {
            phase = Phase.TAUNTING;
        } else {
            phase = Phase.MUGGING;
        }
        // Its work waits for the morning.
        entityData.set(DATA_ASLEEP, isNight());
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
        return phase == Phase.FLEEING;
    }

    /** @return whether the Bailiff is still robbing its player's containers */
    public boolean isRobbing() {
        return phase == Phase.ROBBING;
    }

    /** @return whether the Bailiff is shaking its head at its player, on the client too */
    public boolean isTaunting() {
        return entityData.get(DATA_TAUNTING);
    }

    /** @return whether the Bailiff is asleep where it arrived, on the client too */
    public boolean isAsleep() {
        return entityData.get(DATA_ASLEEP);
    }

    /** @return whether it is night where the Bailiff stands */
    private boolean isNight() {
        return night.test(this);
    }

    /**
     * @return whether it is night where the Bailiff stands, as the day's timeline has it, whatever the
     *         weather: outside the hours undead burn in by day, so a storm by day is no night
     */
    private boolean isNightByTheTimeline() {
        return !level().dimensionType().hasFixedTime()
                && !level().environmentAttributes().getValue(EnvironmentAttributes.MONSTERS_BURN, position());
    }

    /**
     * Has {@code night} tell whether it is night where a Bailiff stands, in place of the day's
     * timeline, for a run that sets its own nights rather than read the world's one clock. Set once,
     * as the mod is built, before any Bailiff arrives.
     */
    public static void tellNightBy(Predicate<Bailiff> night) {
        Bailiff.night = night;
    }

    /** Wakes the Bailiff, whose time on site starts now. */
    private void wake() {
        entityData.set(DATA_ASLEEP, false);
        ticksOnSite = 0;
    }

    /** @return whether the Bailiff stands charging its teleport away */
    public boolean isCharging() {
        return ticksCharging >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && isAsleep() && tickCount % TICKS_BETWEEN_SNORES == 0) {
            // Drawn by each client alone, over its head as it lies.
            level().addParticle(BailiffContent.SNORE.get(), getX(), getY() + 0.6, getZ(), 0, 0, 0);
        }
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
        if (isAsleep()) {
            if (!isNight()) {
                wake();
            }
            return;
        }
        if (phase != Phase.FLEEING) {
            if (phase == Phase.MUGGING) {
                setTarget(target);
            }
            if (++ticksOnSite >= WickednessConfig.ticksOnSite()) {
                flee();
            }
            return;
        }
        if (!isCharging() && (distanceTo(target) >= WickednessConfig.vanishDistance() || ++ticksFleeing >= TICKS_TO_GET_AWAY)) {
            ticksCharging = 0;
            level.playSound(null, getX(), getY(), getZ(), BailiffContent.CHARGES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        }
        if (isCharging()) {
            charge(level, target);
        }
    }

    /**
     * Stands charging its teleport, and once charged vanishes with what it carries, leaving the
     * {@code target} owing when that is nothing.
     */
    private void charge(ServerLevel level, ServerPlayer target) {
        // Stock still, so its player can take aim.
        getNavigation().stop();
        getMoveControl().setWait();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 6, 0.3, 0.8, 0.3, 0.6);
        if (++ticksCharging < WickednessConfig.ticksToTeleport()) {
            return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 48, 0.3, 0.8, 0.3, 0.2);
        level.playSound(null, getX(), getY(), getZ(), BailiffContent.VANISHES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        if (stolen.isEmpty()) {
            Wickedness.owe(target);
        }
        discard();
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
        if (landed && phase == Phase.MUGGING && target instanceof ServerPlayer hit && cameFor(hit)) {
            // Whether or not it found a stack, it has had its go.
            steal(hit);
            phase = Phase.TAUNTING;
            setTarget(null);
        }
        return landed;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isAsleep() && source.getEntity() != null) {
            // Struck in its sleep, by anyone, it takes double and wakes to get to work, not to flee.
            boolean hurt = super.hurtServer(level, source, damage * 2);
            if (hurt) {
                wake();
            }
            return hurt;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && (phase == Phase.ROBBING || phase == Phase.TAUNTING)
                && source.getEntity() instanceof ServerPlayer hitter && cameFor(hitter)) {
            // Caught at it, it runs with whatever it has.
            flee();
        }
        return hurt;
    }

    /**
     * Takes one stack at random from the main inventory or hotbar, never the held item: the one in
     * the selected slot as the hit lands, so the player keeps what they fight with.
     */
    private void steal(ServerPlayer target) {
        var inventory = target.getInventory();
        take(inventory, inventory.getNonEquipmentItems().size(), held(inventory));
    }

    /** @return whether the inventory holds a stack the Bailiff may take */
    private static boolean hasTakeable(Inventory inventory) {
        return !filled(inventory, inventory.getNonEquipmentItems().size(), held(inventory)).isEmpty();
    }

    /** @return the inventory's selected slot, which the Bailiff never takes from */
    private static IntPredicate held(Inventory inventory) {
        return slot -> slot == inventory.getSelectedSlot();
    }

    /**
     * Takes one stack at random from the first {@code slots} of {@code container}, but none that
     * {@code kept} keeps.
     *
     * @return whether there was a stack to take
     */
    private boolean take(Container container, int slots, IntPredicate kept) {
        var filled = filled(container, slots, kept);
        if (filled.isEmpty()) {
            return false;
        }
        int slot = filled.get(getRandom().nextInt(filled.size()));
        stolen.add(container.removeItemNoUpdate(slot));
        container.setChanged();
        return true;
    }

    /** @return the slots among the first {@code slots} of {@code container} that hold a stack and {@code kept} doesn't keep */
    private static List<Integer> filled(Container container, int slots, IntPredicate kept) {
        var filled = new ArrayList<Integer>();
        for (int slot = 0; slot < slots; slot++) {
            if (!kept.test(slot) && !container.getItem(slot).isEmpty()) {
                filled.add(slot);
            }
        }
        return filled;
    }

    private void flee() {
        phase = Phase.FLEEING;
        setTarget(null);
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
     * Robs the base, the nearest of its player's containers first: walks to it, opens it, and after
     * half a second takes one stack from it, its haul, and goes to taunt. An empty one it closes and
     * moves on from, and one it can't reach in ten seconds, or that is no longer there, it leaves.
     * With none left and nothing taken, it mugs its player instead.
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
            // Its ticks count game ticks: half a second to take, ten to reach a container.
            return true;
        }

        @Override
        public void tick() {
            // Ticked on the off ticks too, before the selector checks it should go on: done, or
            // sent fleeing, it may have nothing left to rob.
            if (!isRobbing() || isAsleep() || !(level() instanceof ServerLevel level)) {
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
            } else if (ticks >= TICKS_TO_TAKE) {
                boolean took = take(container, container.getContainerSize(), slot -> false);
                leave();
                if (took) {
                    phase = Phase.TAUNTING;
                }
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
            if (toRob.isEmpty() && phase == Phase.ROBBING) {
                phase = Phase.MUGGING;
            }
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

    /**
     * With its haul, or with nothing to take, walks up to its player, or as near as it gets in ten
     * seconds, and taunts them: shakes its head with a mocking sound, and tells them in the action
     * bar that it took its due, or that it will return. Then it flees.
     */
    private final class Taunt extends Goal {

        private int ticks;

        Taunt() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return phase == Phase.TAUNTING;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
        }

        @Override
        public void tick() {
            if (phase != Phase.TAUNTING || isAsleep() || !(level() instanceof ServerLevel level)) {
                return;
            }
            var target = target(level);
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target);
            ticks++;
            if (isTaunting()) {
                if (ticks >= TICKS_TAUNTING) {
                    flee();
                }
                return;
            }
            if (distanceTo(target) > TAUNT_DISTANCE && ticks <= TICKS_TO_REACH) {
                if (getNavigation().isDone()) {
                    getNavigation().moveTo(target, 1.0);
                }
                return;
            }
            getNavigation().stop();
            entityData.set(DATA_TAUNTING, true);
            ticks = 0;
            level.playSound(null, getX(), getY(), getZ(), BailiffContent.TAUNTS.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
            target.sendOverlayMessage(Component.translatable(stolen.isEmpty()
                    ? "entity.atouchofwickedness.bailiff.taunt_empty_handed" : "entity.atouchofwickedness.bailiff.taunt"));
        }

        @Override
        public void stop() {
            entityData.set(DATA_TAUNTING, false);
            getNavigation().stop();
        }
    }

    /** Lies still where it fell asleep, doing nothing, until it wakes. */
    private final class Sleep extends Goal {

        Sleep() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isAsleep();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getMoveControl().setWait();
        }
    }

    /** Runs from its player, faster than they can sprint, until it stands charging its teleport. */
    private final class Flee extends Goal {

        Flee() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isFleeing() && !isCharging() && level() instanceof ServerLevel level && target(level) != null;
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

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
import io.github._5thlayer.atouchofwickedness.discovery.Discoveries;
import io.github._5thlayer.atouchofwickedness.wickedness.Standing;
import io.github._5thlayer.atouchofwickedness.wickedness.Wickedness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Container;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
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
 * stack from it, or moves on to the next when it holds nothing worth taking (ADR 0003). That one
 * stack is its haul: it walks up to its player, shakes its head at them mocking, and flees. With
 * nothing to rob it mugs its player instead: it goes for them, and its first hit that lands takes
 * one stack from their main inventory or hotbar, never the held item, their worn armour or off
 * hand, and it goes to taunt them. A player with nothing worth taking it taunts at once,
 * empty-handed. Whichever stack it takes it draws by Value ({@link ItemValues}): each as likely as
 * its share of what all those it may take are worth, so never one worth nothing. Until it has its
 * haul, an item lying near it worth going for, its dead player's drops included, is its haul too:
 * it walks to the nearest and picks it up; junk on the ground it leaves. On Peaceful its hits deal
 * nothing and still steal, though not through a shield its player blocks with.
 * Arriving at night, it lies down where it arrived and sleeps until dawn under rising Zs, harmless:
 * the first hit on it deals double damage and wakes it, and its time on site starts as it wakes. A
 * player who hits it as it robs or taunts sends it fleeing at once. Once its time on site is up it
 * flees whatever it has. Fleeing, it runs faster than a sprinting player, and once far enough from
 * them, or after a while however far it got, it stands still charging a teleport, when its player
 * can still kill it from afar, then vanishes with what it carries; one that vanishes with nothing
 * leaves its player owing ({@link Wickedness#owe}). Killed, it drops everything it stole and a few
 * Seals. Its player dying doesn't end the visit, but their logging out, the server stopping
 * included, or changing dimension, respawning included, does: it is gone, never saved, and leaves
 * them owing as one that vanishes with nothing does.
 */
public final class Bailiff extends PathfinderMob implements ContainerUser {

    /** How near, in blocks, the Bailiff robs a container from: the centre of the block to its feet. */
    private static final double REACH = 2.5;

    /** Ticks the Bailiff holds a container open before it takes its stack. */
    private static final int TICKS_TO_TAKE = 10;

    /** Ticks the Bailiff tries to reach a container or an item on the ground before it gives up, as on one walled off. */
    private static final int TICKS_TO_REACH = 200;

    /** Ticks between the Zs a sleeping Bailiff sleeps under. */
    private static final int TICKS_BETWEEN_SNORES = 20;

    /** Whether the Bailiff is shaking its head at its player, which its renderer draws. */
    private static final EntityDataAccessor<Boolean> DATA_TAUNTING = SynchedEntityData.defineId(Bailiff.class, EntityDataSerializers.BOOLEAN);

    /** Whether the Bailiff is asleep, which its renderer draws lying down. */
    private static final EntityDataAccessor<Boolean> DATA_ASLEEP = SynchedEntityData.defineId(Bailiff.class, EntityDataSerializers.BOOLEAN);

    /** Tells whether it is night where a Bailiff stands: the day's timeline, unless a run sets its own. */
    private static Predicate<Bailiff> night = Bailiff::isNightByTheTimeline;

    private @Nullable UUID player;
    /** What it is about, as the rules have it; the rest of the Bailiff is its body in the world. */
    private final Visit visit = new Visit();
    /** The containers still to try, its player's, near where it arrived. */
    private final List<BlockPos> toRob = new ArrayList<>();
    /** The container it has open, or null. */
    private @Nullable BlockPos robbing;
    private final List<ItemStack> stolen = new ArrayList<>();
    /** Whether it is going for an item on the ground, which its Visit's taunt waits on. */
    private boolean gathering;

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
        goalSelector.addGoal(1, new Gather());
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
        // Its work waits for the morning.
        visit.arrived(new Visit.Rules(WickednessConfig.ticksOnSite(), WickednessConfig.vanishDistance(), WickednessConfig.ticksToTeleport()),
                !toRob.isEmpty(), hasTakeable(target.getInventory()), isNight());
        sync();
        // Its player was at their Threshold to bring it, so past Forfeit too.
        Discoveries.standing(target, Standing.FORFEIT);
        Discoveries.bailiffCame(target);
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
        return visit.phase() == Visit.Phase.FLEEING;
    }

    /** @return whether the Bailiff is still robbing its player's containers */
    public boolean isRobbing() {
        return visit.phase() == Visit.Phase.ROBBING;
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

    /** Shows its clients whether it sleeps and whether it taunts, as its Visit has it. */
    private void sync() {
        entityData.set(DATA_ASLEEP, visit.phase() == Visit.Phase.ASLEEP);
        entityData.set(DATA_TAUNTING, visit.isShaking());
    }

    /** @return whether the Bailiff stands charging its teleport away */
    public boolean isCharging() {
        return visit.isCharging();
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
            // Its player left, or left the dimension, as by respawning in another: the visit is
            // over. One who died is still here.
            var elsewhere = level.getServer().getPlayerList().getPlayer(player);
            if (elsewhere != null) {
                end(elsewhere);
            } else {
                discard();
            }
            return;
        }
        boolean wasCharging = visit.isCharging();
        var outcome = visit.tick(isNight(), distanceTo(target), target.isAlive(), gathering);
        sync();
        if (visit.phase() != Visit.Phase.MUGGING) {
            if (getTarget() != null) {
                setTarget(null);
            }
        } else if (target.isAlive()) {
            setTarget(target);
        }
        if (outcome == Visit.Outcome.TAUNTS) {
            taunt(level, target);
        }
        if (visit.isCharging()) {
            if (!wasCharging) {
                level.playSound(null, getX(), getY(), getZ(), BailiffContent.CHARGES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            }
            charge(level);
        }
        if (outcome == Visit.Outcome.VANISHES) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 48, 0.3, 0.8, 0.3, 0.2);
            level.playSound(null, getX(), getY(), getZ(), BailiffContent.VANISHES.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
            end(target);
        }
    }

    /** Shakes its head at {@code target}, with a mocking sound, and tells them what it took, or that it will return. */
    private void taunt(ServerLevel level, ServerPlayer target) {
        getNavigation().stop();
        level.playSound(null, getX(), getY(), getZ(), BailiffContent.TAUNTS.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        target.sendOverlayMessage(Component.translatable(visit.leavesOwing()
                ? "entity.atouchofwickedness.bailiff.taunt_empty_handed" : "entity.atouchofwickedness.bailiff.taunt"));
    }

    /** Stands stock still charging its teleport, so its player can take aim. */
    private void charge(ServerLevel level) {
        getNavigation().stop();
        getMoveControl().setWait();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 6, 0.3, 0.8, 0.3, 0.6);
    }

    /**
     * Ends the visits of the Bailiffs in {@code level} come for {@code target}, who is leaving it or
     * the server.
     */
    public static void endVisits(ServerLevel level, ServerPlayer target) {
        for (Bailiff bailiff : level.getEntities(BailiffContent.BAILIFF.get(), bailiff -> bailiff.cameFor(target))) {
            bailiff.end(target);
        }
    }

    /** Ends the Visit, however it ends, leaving {@code target} owing when it took nothing. */
    private void end(ServerPlayer target) {
        if (visit.leavesOwing()) {
            Wickedness.owe(target);
        }
        discard();
    }

    /** @return its player, dead or alive, while they are in its level, or null once they left it */
    private @Nullable ServerPlayer target(ServerLevel level) {
        return player != null && level.getPlayerByUUID(player) instanceof ServerPlayer target ? target : null;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        // Its own player on Peaceful too, where no mob may go for a player: it comes to rob them, not hurt them.
        return target instanceof ServerPlayer hit && cameFor(hit) ? target.canBeSeenAsEnemy() : super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean landed = super.doHurtTarget(level, target);
        if (!landed && level.getDifficulty() == Difficulty.PEACEFUL && target instanceof ServerPlayer hit && cameFor(hit)
                && hit.isAlive() && !hit.isInvulnerableTo(level, damageSources().mobAttack(this))
                && !blocks(hit)) {
            // Peaceful scales its hit to nothing, which no player takes, but theft is no damage.
            landed = true;
        }
        if (landed && visit.phase() == Visit.Phase.MUGGING && target instanceof ServerPlayer hit && cameFor(hit)) {
            steal(hit);
            visit.hitLanded();
            sync();
            setTarget(null);
        }
        return landed;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() == null) {
            return super.hurtServer(level, source, damage);
        }
        boolean hurt = super.hurtServer(level, source, visit.damage(damage));
        if (hurt) {
            visit.struck(source.getEntity() instanceof ServerPlayer hitter && cameFor(hitter));
            sync();
        }
        return hurt;
    }

    /**
     * @return whether {@code target} blocks the Bailiff's hit, as with a shield raised its way, worked
     *         out as vanilla blocks one, for a nominal hit since Peaceful's is nothing
     */
    private boolean blocks(ServerPlayer target) {
        var with = target.getItemBlockingWith();
        var blocking = with == null ? null : with.get(DataComponents.BLOCKS_ATTACKS);
        if (blocking == null) {
            return false;
        }
        var toward = position().subtract(target.position());
        toward = new Vec3(toward.x, 0, toward.z).normalize();
        double angle = Math.acos(toward.dot(Vec3.directionFromRotation(0, target.getYHeadRot())));
        return blocking.resolveBlockedDamage(damageSources().mobAttack(this), 1, angle) > 0;
    }

    /**
     * Takes one stack by Value from the main inventory or hotbar, never the held item: the one in
     * the selected slot as the hit lands, so the player keeps what they fight with.
     */
    private void steal(ServerPlayer target) {
        var inventory = target.getInventory();
        take(inventory, inventory.getNonEquipmentItems().size(), held(inventory));
    }

    /** @return whether the inventory holds a stack the Bailiff may take and that is worth taking */
    private static boolean hasTakeable(Inventory inventory) {
        return Value.anyWorth(worth(inventory, inventory.getNonEquipmentItems().size(), held(inventory)));
    }

    /** @return the inventory's selected slot, which the Bailiff never takes from */
    private static IntPredicate held(Inventory inventory) {
        return slot -> slot == inventory.getSelectedSlot();
    }

    /**
     * Takes one stack by Value from the first {@code slots} of {@code container}, but none that
     * {@code kept} keeps, a seizure its player discovers while they are in its level.
     *
     * @return whether there was a stack worth taking
     */
    private boolean take(Container container, int slots, IntPredicate kept) {
        int slot = Value.draw(worth(container, slots, kept), getRandom().nextDouble());
        if (slot < 0) {
            return false;
        }
        seize(container.removeItemNoUpdate(slot));
        container.setChanged();
        return true;
    }

    /** Adds {@code stack} to its haul, a seizure its player discovers, whether it was theirs or lay on the ground. */
    private void seize(ItemStack stack) {
        stolen.add(stack);
        visit.took();
        sync();
        if (level() instanceof ServerLevel level && target(level) instanceof ServerPlayer target) {
            Discoveries.seized(target);
        }
    }

    /**
     * @return what each of the first {@code slots} of {@code container} is worth to the Bailiff, by
     *         slot, nothing for one empty or that {@code kept} keeps
     */
    private static long[] worth(Container container, int slots, IntPredicate kept) {
        var worth = new long[slots];
        for (int slot = 0; slot < slots; slot++) {
            if (!kept.test(slot)) {
                worth[slot] = ItemValues.of(container.getItem(slot));
            }
        }
        return worth;
    }

    /** Picks up {@code item}, its haul, as a mob picks up loot, and goes to taunt its player. */
    private void pickUp(ItemEntity item) {
        onItemPickup(item);
        take(item, item.getItem().getCount());
        seize(item.getItem().copy());
        item.discard();
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
     * With none left and nothing taken, it mugs its player instead, or with nothing of theirs worth
     * taking taunts them.
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
            } else if (ticks >= TICKS_TO_TAKE) {
                take(container, container.getContainerSize(), slot -> false);
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
            if (toRob.isEmpty() && level() instanceof ServerLevel level && target(level) instanceof ServerPlayer target) {
                visit.containersRanOut(hasTakeable(target.getInventory()));
                sync();
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

        Taunt() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return visit.phase() == Visit.Phase.TAUNTING;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(level() instanceof ServerLevel level) || !(target(level) instanceof ServerPlayer target) || !target.isAlive()) {
                return;
            }
            getLookControl().setLookAt(target);
            // Walks up to its player until its Visit, near enough or done trying, has it shake its head.
            if (!isTaunting() && getNavigation().isDone()) {
                getNavigation().moveTo(target, 1.0);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /**
     * Until it has its haul, walks to the nearest item lying near it worth going for, and picks it
     * up once it can, before robbing, mugging or taunting. One it can't reach and pick up in ten
     * seconds it leaves.
     */
    private final class Gather extends Goal {

        /** The items it gave up on, for as long as it is here. */
        private final List<ItemEntity> outOfReach = new ArrayList<>();
        private @Nullable ItemEntity item;
        private int ticks;

        Gather() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!visit.wantsLoose()) {
                return false;
            }
            item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(WickednessConfig.pickupRadius()),
                            loose -> loose.isAlive() && ItemValues.worthPickingUp(loose.getItem()) && !outOfReach.contains(loose))
                    .stream().min(Comparator.comparingDouble(Bailiff.this::distanceToSqr)).orElse(null);
            return item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return visit.wantsLoose() && item != null && item.isAlive() && !outOfReach.contains(item);
        }

        @Override
        public void start() {
            ticks = 0;
            gathering = true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (item == null || !canContinueToUse()) {
                return;
            }
            getLookControl().setLookAt(item);
            // Ten seconds however it spends them, so one that is never picked up holds it no longer.
            if (++ticks > TICKS_TO_REACH) {
                outOfReach.add(item);
                return;
            }
            // A mob's reach for loot, and a dead player's drops wait out their pickup delay.
            if (getBoundingBox().inflate(1, 0, 1).intersects(item.getBoundingBox())) {
                if (!item.hasPickUpDelay()) {
                    pickUp(item);
                }
                return;
            }
            if (distanceToSqr(item) < 4) {
                // A path ends on the item's block, which may leave it just out of reach: the last
                // steps it walks straight.
                getNavigation().stop();
                getMoveControl().setWantedPosition(item.getX(), item.getY(), item.getZ(), 1.0);
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(item, 1.0);
            }
        }

        @Override
        public void stop() {
            item = null;
            gathering = false;
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

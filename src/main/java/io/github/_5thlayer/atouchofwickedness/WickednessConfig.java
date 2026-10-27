// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness;

import io.github._5thlayer.atouchofwickedness.bailiff.Value;
import io.github._5thlayer.atouchofwickedness.wickedness.Atonement;
import io.github._5thlayer.atouchofwickedness.wickedness.Gossip;
import io.github._5thlayer.atouchofwickedness.wickedness.Standing;
import io.github._5thlayer.atouchofwickedness.wickedness.Warning;
import io.github._5thlayer.atouchofwickedness.wickedness.WickednessWeight;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Pillar's common config, {@code atouchofwickedness-common.toml}: every number of the loop, each a
 * starting value to tune in play (suite ADR-0001). The creatures' weights are not here: they are the
 * {@code atouchofwickedness:wickedness_weight} data map, so a pack weighs modded creatures too, and the
 * items' Values the {@code atouchofwickedness:bailiff_value} one.
 */
public final class WickednessConfig {

    static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue TAMED_WEIGHT;
    private static final ModConfigSpec.DoubleValue BABY_MULTIPLIER;
    private static final ModConfigSpec.IntValue TORMENT_WEIGHT;
    private static final ModConfigSpec.IntValue HIT_COST;
    private static final ModConfigSpec.IntValue GOSSIP_RADIUS;
    private static final ModConfigSpec.DoubleValue GOSSIP_SCALE;
    private static final ModConfigSpec.IntValue DECAY_PER_DAY;
    private static final ModConfigSpec.IntValue BREEDING;
    private static final ModConfigSpec.IntValue TAMING;
    private static final ModConfigSpec.IntValue HEALING_PER_HEART;
    private static final ModConfigSpec.IntValue CURING;
    private static final ModConfigSpec.IntValue GIFT;
    private static final ModConfigSpec.IntValue GIFTS_PER_DAY;
    private static final ModConfigSpec.IntValue THRESHOLD_MIN;
    private static final ModConfigSpec.IntValue THRESHOLD_MAX;
    private static final ModConfigSpec.DoubleValue MARKED;
    private static final ModConfigSpec.DoubleValue OVERDUE;
    private static final ModConfigSpec.DoubleValue FORFEIT;
    private static final ModConfigSpec.IntValue SECONDS_BETWEEN_MARKED;
    private static final ModConfigSpec.IntValue SECONDS_BETWEEN_OVERDUE;
    private static final ModConfigSpec.IntValue SECONDS_BETWEEN_FORFEIT;
    private static final ModConfigSpec.IntValue MARKED_DISTANCE;
    private static final ModConfigSpec.IntValue OVERDUE_DISTANCE;
    private static final ModConfigSpec.IntValue FORFEIT_DISTANCE;
    private static final ModConfigSpec.IntValue FLEE_RADIUS;
    private static final ModConfigSpec.DoubleValue ANIMAL_FLEE_SPEED;
    private static final ModConfigSpec.DoubleValue ANIMAL_FLEE_SPRINT_SPEED;
    private static final ModConfigSpec.IntValue ARRIVAL_MIN_DISTANCE;
    private static final ModConfigSpec.IntValue ARRIVAL_MAX_DISTANCE;
    private static final ModConfigSpec.IntValue SECONDS_BETWEEN_VISITS;
    private static final ModConfigSpec.IntValue SECONDS_BETWEEN_RETRIES;
    private static final ModConfigSpec.DoubleValue BAILIFF_HEALTH;
    private static final ModConfigSpec.DoubleValue BAILIFF_DAMAGE;
    private static final ModConfigSpec.DoubleValue BAILIFF_KNOCKBACK_RESISTANCE;
    private static final ModConfigSpec.DoubleValue BAILIFF_SPEED;
    private static final ModConfigSpec.DoubleValue BAILIFF_FLEE_SPEED;
    private static final ModConfigSpec.IntValue CONTAINERS_REMEMBERED;
    private static final ModConfigSpec.IntValue ROBBING_RADIUS;
    private static final ModConfigSpec.IntValue PICKUP_RADIUS;
    private static final ModConfigSpec.IntValue UNLISTED_VALUE;
    private static final ModConfigSpec.IntValue VALUE_PER_ENCHANTMENT_LEVEL;
    private static final ModConfigSpec.IntValue PICKUP_FLOOR;
    private static final ModConfigSpec.IntValue SECONDS_ON_SITE;
    private static final ModConfigSpec.IntValue VANISH_DISTANCE;
    private static final ModConfigSpec.IntValue SECONDS_TO_TELEPORT;
    private static final ModConfigSpec.IntValue SEALS_MIN;
    private static final ModConfigSpec.IntValue SEALS_MAX;
    private static final ModConfigSpec.DoubleValue BAG_CHANCE;
    private static final ModConfigSpec.DoubleValue BAG_CHANCE_PER_LOOTING_LEVEL;
    private static final ModConfigSpec.IntValue TALLY_STICK_USES;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("weights");
        TAMED_WEIGHT = builder
                .comment("What killing any tamed creature adds, whoever owns it and whatever its own weight.")
                .defineInRange("tamed", 50, 0, 1_000_000);
        BABY_MULTIPLIER = builder
                .comment("How many times its weight killing a baby adds, rounded to the nearest whole number.")
                .defineInRange("babyMultiplier", 2.0, 0, 100);
        TORMENT_WEIGHT = builder
                .comment("The weight from which each damaging hit a player lands on a creature adds the hit cost:",
                        "a villager, a tamed pet, a cat or an ocelot by default.")
                .defineInRange("tormentWeight", 30, 0, 1_000_000);
        HIT_COST = builder
                .comment("What each damaging hit on a creature weighing the torment weight or more adds, short of the killing blow.")
                .defineInRange("hitCost", 3, 0, 1_000_000);
        builder.pop();

        builder.push("gossip");
        GOSSIP_RADIUS = builder
                .comment("How near, in blocks, a villager must be to a Wicked kill it can see to gossip about the killer.")
                .defineInRange("radius", 16, 0, 64);
        GOSSIP_SCALE = builder
                .comment("How many times the kill's weight in vanilla's minor negative gossip each villager who sees it gains",
                        "about the killer, rounded to the nearest whole number. Vanilla holds at most 200 of it, a villager",
                        "raises its prices by it, and an iron golem turns on a player at 100. 0 is none.")
                .defineInRange("scale", 5.0, 0, 1_000);
        builder.pop();

        builder.push("decay");
        DECAY_PER_DAY = builder
                .comment("How much Wickedness falls each in-game day, a point at a time as the day passes.",
                        "It falls only while the player is online, and never below 0.")
                .defineInRange("perDay", 10, 0, 1_000_000);
        builder.pop();

        builder.push("atonement");
        BREEDING = builder
                .comment("What breeding two animals takes off the Wickedness of the player who fed them.")
                .defineInRange("breeding", 3, 0, 1_000_000);
        TAMING = builder
                .comment("What taming an animal takes off.")
                .defineInRange("taming", 10, 0, 1_000_000);
        HEALING_PER_HEART = builder
                .comment("What healing a hurt tamed pet the player owns, by feeding it, takes off for each whole heart",
                        "it regains, so feeding a healthy pet takes off nothing.")
                .defineInRange("healingPerHeart", 1, 0, 1_000_000);
        CURING = builder
                .comment("What curing a zombie villager takes off.")
                .defineInRange("curing", 40, 0, 1_000_000);
        GIFT = builder
                .comment("What a villager picking up food the player threw takes off.")
                .defineInRange("gift", 2, 0, 1_000_000);
        GIFTS_PER_DAY = builder
                .comment("How many gifts to villagers take off Wickedness each in-game day; the rest take off nothing.")
                .defineInRange("giftsPerDay", 5, 0, 1_000_000);
        builder.pop();

        builder.push("threshold");
        THRESHOLD_MIN = builder
                .comment("The lowest Threshold a player can draw: the Wickedness at which the Bailiff comes. It is drawn",
                        "at random from this range when a player first joins and again after each visit.")
                .defineInRange("min", 50, 1, 1_000_000);
        THRESHOLD_MAX = builder
                .comment("The highest Threshold a player can draw. Below the lowest, the lowest is drawn every time.")
                .defineInRange("max", 100, 1, 1_000_000);
        builder.pop();

        builder.push("standing");
        MARKED = builder
                .comment("Where the Tally Stick starts reading Marked, and the Warnings start, as a fraction of the player's Threshold.")
                .defineInRange("marked", 0.5, 0, 1);
        OVERDUE = builder
                .comment("Where the Tally Stick starts reading Overdue, and the Warnings draw nearer, as a fraction of the player's Threshold.")
                .defineInRange("overdue", 0.75, 0, 1);
        FORFEIT = builder
                .comment("Where the Tally Stick starts reading Forfeit, the Warnings are close and animals flee,",
                        "as a fraction of the player's Threshold.")
                .defineInRange("forfeit", 0.9, 0, 1);
        builder.pop();

        builder.push("warnings");
        SECONDS_BETWEEN_MARKED = builder
                .comment("How many seconds apart a Marked player hears a far bell, after the one as they enter Marked.")
                .defineInRange("secondsBetweenMarked", 120, 1, 86_400);
        SECONDS_BETWEEN_OVERDUE = builder
                .comment("How many seconds apart an Overdue player hears chains, after the ones as they enter Overdue.")
                .defineInRange("secondsBetweenOverdue", 60, 1, 86_400);
        SECONDS_BETWEEN_FORFEIT = builder
                .comment("How many seconds apart a Forfeit player hears footsteps, after the ones as they enter Forfeit.")
                .defineInRange("secondsBetweenForfeit", 30, 1, 86_400);
        MARKED_DISTANCE = builder
                .comment("How far off, in blocks, a Marked player's bell sounds. A Warning is louder the nearer it is.")
                .defineInRange("markedDistance", 24, 0, 32);
        OVERDUE_DISTANCE = builder
                .comment("How far off, in blocks, an Overdue player's chains sound.")
                .defineInRange("overdueDistance", 12, 0, 32);
        FORFEIT_DISTANCE = builder
                .comment("How far off, in blocks, a Forfeit player's footsteps sound.")
                .defineInRange("forfeitDistance", 4, 0, 32);
        FLEE_RADIUS = builder
                .comment("How near, in blocks, passive animals flee from a Forfeit player they see. It applies to",
                        "animals that join the world after it changes.")
                .defineInRange("fleeRadius", 16, 1, 64);
        ANIMAL_FLEE_SPEED = builder
                .comment("How many times its own speed an animal flees from a Forfeit player at. It applies to animals",
                        "that join the world after it changes.")
                .defineInRange("animalFleeSpeed", 1.0, 0, 10);
        ANIMAL_FLEE_SPRINT_SPEED = builder
                .comment("How many times its own speed an animal flees at while within 7 blocks of the player.",
                        "It applies to animals that join the world after it changes.")
                .defineInRange("animalFleeSprintSpeed", 1.4, 0, 10);
        builder.pop();

        builder.push("bailiff");
        ARRIVAL_MIN_DISTANCE = builder
                .comment("The nearest, in blocks across the ground, that a Bailiff arrives to its player.")
                .defineInRange("arrivalMinDistance", 12, 1, 64);
        ARRIVAL_MAX_DISTANCE = builder
                .comment("The farthest, in blocks across the ground, that a Bailiff arrives from its player.")
                .defineInRange("arrivalMaxDistance", 24, 1, 64);
        SECONDS_BETWEEN_VISITS = builder
                .comment("How many seconds after a Bailiff arrives before another may come for the same player.",
                        "A player still at or above their Threshold when it ends is visited again then.")
                .defineInRange("secondsBetweenVisits", 300, 0, 86_400);
        SECONDS_BETWEEN_RETRIES = builder
                .comment("How many seconds apart an owed visit tries again to send a Bailiff, as when the last try found",
                        "no spot for it to arrive on or the cooldown still ran. Wickedness resets only once one arrives.")
                .defineInRange("secondsBetweenRetries", 3, 1, 3600);
        BAILIFF_HEALTH = builder
                .comment("The Bailiff's health, in half hearts.")
                .defineInRange("health", 60.0, 1, 1024);
        BAILIFF_DAMAGE = builder
                .comment("The damage of the Bailiff's hit on Normal, which vanilla scales by difficulty as it does any mob's.")
                .defineInRange("damage", 4.0, 0, 1024);
        BAILIFF_KNOCKBACK_RESISTANCE = builder
                .comment("How much of a hit's knockback the Bailiff ignores: 1 is all of it.")
                .defineInRange("knockbackResistance", 1.0, 0, 1);
        BAILIFF_SPEED = builder
                .comment("The Bailiff's movement speed as it goes for its player, in vanilla's attribute units.")
                .defineInRange("speed", 0.35, 0, 2);
        BAILIFF_FLEE_SPEED = builder
                .comment("How many times its own speed the Bailiff flees at. Above about 1.1 it outruns a sprinting player.")
                .defineInRange("fleeSpeed", 1.4, 0, 10);
        CONTAINERS_REMEMBERED = builder
                .comment("How many of the Seizable containers a player opened, the most recent, are remembered on them",
                        "for their Bailiff to rob. Only these are ever robbed.")
                .defineInRange("containersRemembered", 32, 1, 1024);
        ROBBING_RADIUS = builder
                .comment("How near, in blocks, to where it arrived a remembered container must be for the Bailiff to rob it.")
                .defineInRange("robbingRadius", 32, 0, 128);
        PICKUP_RADIUS = builder
                .comment("How near, in blocks, an item lying on the ground must be for a Bailiff with nothing yet to go",
                        "for it and take it as its haul, its dead player's drops included. 0 is never.")
                .defineInRange("pickupRadius", 6, 0, 32);
        UNLISTED_VALUE = builder
                .comment("What an item the atouchofwickedness:bailiff_value data map doesn't list is worth to the Bailiff,",
                        "which draws the stack it takes weighted by what each is worth: its items' Value times its count.",
                        "0 is never taken.")
                .defineInRange("unlistedValue", 1, 0, 1_000_000);
        VALUE_PER_ENCHANTMENT_LEVEL = builder
                .comment("What each level of an item's enchantments, a book's stored ones included, adds to its Value.",
                        "An item worth nothing stays worth nothing.")
                .defineInRange("valuePerEnchantmentLevel", 20, 0, 1_000_000);
        PICKUP_FLOOR = builder
                .comment("The least an item lying on the ground must be worth, each and its enchantments included, for a",
                        "Bailiff to go for it, so junk dropped in front of it buys nothing.")
                .defineInRange("pickupFloor", 5, 0, 1_000_000);
        SECONDS_ON_SITE = builder
                .comment("How many seconds after it arrives a Bailiff flees, whatever it has taken.")
                .defineInRange("secondsOnSite", 60, 1, 3600);
        VANISH_DISTANCE = builder
                .comment("How far, in blocks, from its player a fleeing Bailiff stops to charge its teleport away.")
                .defineInRange("vanishDistance", 24, 1, 128);
        SECONDS_TO_TELEPORT = builder
                .comment("How many seconds a Bailiff stands still charging its teleport, when its player can still kill it,",
                        "before it vanishes with what it carries.")
                .defineInRange("secondsToTeleport", 2, 0, 60);
        SEALS_MIN = builder
                .comment("The fewest Seals a killed Bailiff drops.")
                .defineInRange("sealsMin", 1, 0, 64);
        SEALS_MAX = builder
                .comment("The most Seals a killed Bailiff drops. Below the fewest, the fewest drop every time.")
                .defineInRange("sealsMax", 3, 0, 64);
        BAG_CHANCE = builder
                .comment("The chance a killed Bailiff also drops a Bailiff's Bag, which holds four times what a bundle holds.")
                .defineInRange("bagChance", 0.05, 0, 1);
        BAG_CHANCE_PER_LOOTING_LEVEL = builder
                .comment("What each level of Looting on its killer's weapon adds to the chance of a Bailiff's Bag.")
                .defineInRange("bagChancePerLootingLevel", 0.01, 0, 1);
        builder.pop();

        builder.push("tallyStick");
        TALLY_STICK_USES = builder
                .comment("How many times a Tally Stick can be read before it breaks. A Seal repairs it in an anvil.")
                .defineInRange("uses", 16, 1, 1024);
        builder.pop();
        SPEC = builder.build();
    }

    private WickednessConfig() {
    }

    public static WickednessWeight.Rules weightRules() {
        return new WickednessWeight.Rules(TAMED_WEIGHT.getAsInt(), BABY_MULTIPLIER.getAsDouble(), TORMENT_WEIGHT.getAsInt(), HIT_COST.getAsInt());
    }

    public static Gossip.Rules gossipRules() {
        return new Gossip.Rules(GOSSIP_RADIUS.getAsInt(), GOSSIP_SCALE.getAsDouble());
    }

    public static int decayPerDay() {
        return DECAY_PER_DAY.getAsInt();
    }

    public static Atonement.Rules atonementRules() {
        return new Atonement.Rules(BREEDING.getAsInt(), TAMING.getAsInt(), HEALING_PER_HEART.getAsInt(),
                CURING.getAsInt(), GIFT.getAsInt(), GIFTS_PER_DAY.getAsInt());
    }

    public static int thresholdMin() {
        return THRESHOLD_MIN.getAsInt();
    }

    public static int thresholdMax() {
        return Math.max(THRESHOLD_MIN.getAsInt(), THRESHOLD_MAX.getAsInt());
    }

    public static Standing.Bands bands() {
        return new Standing.Bands(MARKED.getAsDouble(), OVERDUE.getAsDouble(), FORFEIT.getAsDouble());
    }

    public static Warning.Rules warningRules() {
        return new Warning.Rules(
                new Warning.Sounding(SECONDS_BETWEEN_MARKED.getAsInt() * 20L, MARKED_DISTANCE.getAsInt()),
                new Warning.Sounding(SECONDS_BETWEEN_OVERDUE.getAsInt() * 20L, OVERDUE_DISTANCE.getAsInt()),
                new Warning.Sounding(SECONDS_BETWEEN_FORFEIT.getAsInt() * 20L, FORFEIT_DISTANCE.getAsInt()));
    }

    public static int fleeRadius() {
        return FLEE_RADIUS.getAsInt();
    }

    public static double animalFleeSpeed() {
        return ANIMAL_FLEE_SPEED.getAsDouble();
    }

    public static double animalFleeSprintSpeed() {
        return ANIMAL_FLEE_SPRINT_SPEED.getAsDouble();
    }

    public static int arrivalMinDistance() {
        return ARRIVAL_MIN_DISTANCE.getAsInt();
    }

    public static int arrivalMaxDistance() {
        return Math.max(ARRIVAL_MIN_DISTANCE.getAsInt(), ARRIVAL_MAX_DISTANCE.getAsInt());
    }

    public static int ticksBetweenVisits() {
        return SECONDS_BETWEEN_VISITS.getAsInt() * 20;
    }

    public static int ticksBetweenRetries() {
        return SECONDS_BETWEEN_RETRIES.getAsInt() * 20;
    }

    public static double bailiffHealth() {
        return BAILIFF_HEALTH.getAsDouble();
    }

    public static double bailiffDamage() {
        return BAILIFF_DAMAGE.getAsDouble();
    }

    public static double bailiffKnockbackResistance() {
        return BAILIFF_KNOCKBACK_RESISTANCE.getAsDouble();
    }

    public static double bailiffSpeed() {
        return BAILIFF_SPEED.getAsDouble();
    }

    public static double bailiffFleeSpeed() {
        return BAILIFF_FLEE_SPEED.getAsDouble();
    }

    public static int containersRemembered() {
        return CONTAINERS_REMEMBERED.getAsInt();
    }

    public static int robbingRadius() {
        return ROBBING_RADIUS.getAsInt();
    }

    public static int pickupRadius() {
        return PICKUP_RADIUS.getAsInt();
    }

    public static Value.Rules valueRules() {
        return new Value.Rules(UNLISTED_VALUE.getAsInt(), VALUE_PER_ENCHANTMENT_LEVEL.getAsInt(), PICKUP_FLOOR.getAsInt());
    }

    public static int ticksOnSite() {
        return SECONDS_ON_SITE.getAsInt() * 20;
    }

    public static int vanishDistance() {
        return VANISH_DISTANCE.getAsInt();
    }

    public static int ticksToTeleport() {
        return SECONDS_TO_TELEPORT.getAsInt() * 20;
    }

    public static int sealsMin() {
        return SEALS_MIN.getAsInt();
    }

    public static int sealsMax() {
        return Math.max(SEALS_MIN.getAsInt(), SEALS_MAX.getAsInt());
    }

    public static float bagChance() {
        return (float) BAG_CHANCE.getAsDouble();
    }

    public static float bagChancePerLootingLevel() {
        return (float) BAG_CHANCE_PER_LOOTING_LEVEL.getAsDouble();
    }

    /** The Tally Stick's uses, or the default before the config has loaded, as when items are first built. */
    public static int tallyStickUses() {
        return SPEC.isLoaded() ? TALLY_STICK_USES.getAsInt() : TALLY_STICK_USES.getDefault();
    }
}

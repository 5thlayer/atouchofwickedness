// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofwickedness;

import io.github._5thlayer.atouchofwickedness.wickedness.Standing;
import io.github._5thlayer.atouchofwickedness.wickedness.WickednessWeight;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Pillar's common config, {@code atouchofwickedness-common.toml}: every number of the loop, each a
 * starting value to tune in play (suite ADR-0001). The creatures' weights are not here: they are the
 * {@code atouchofwickedness:wickedness_weight} data map, so a pack weighs modded creatures too.
 */
public final class WickednessConfig {

    static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue TAMED_WEIGHT;
    private static final ModConfigSpec.DoubleValue BABY_MULTIPLIER;
    private static final ModConfigSpec.IntValue TORMENT_WEIGHT;
    private static final ModConfigSpec.IntValue HIT_COST;
    private static final ModConfigSpec.IntValue DECAY_PER_DAY;
    private static final ModConfigSpec.IntValue BREEDING;
    private static final ModConfigSpec.IntValue TAMING;
    private static final ModConfigSpec.IntValue FEEDING_PER_HEART;
    private static final ModConfigSpec.IntValue CURING;
    private static final ModConfigSpec.IntValue GIFT;
    private static final ModConfigSpec.IntValue GIFTS_PER_DAY;
    private static final ModConfigSpec.IntValue THRESHOLD_MIN;
    private static final ModConfigSpec.IntValue THRESHOLD_MAX;
    private static final ModConfigSpec.DoubleValue MARKED;
    private static final ModConfigSpec.DoubleValue OVERDUE;
    private static final ModConfigSpec.DoubleValue FORFEIT;
    private static final ModConfigSpec.IntValue ARRIVAL_MIN_DISTANCE;
    private static final ModConfigSpec.IntValue ARRIVAL_MAX_DISTANCE;
    private static final ModConfigSpec.DoubleValue BAILIFF_HEALTH;
    private static final ModConfigSpec.DoubleValue BAILIFF_DAMAGE;
    private static final ModConfigSpec.DoubleValue BAILIFF_KNOCKBACK_RESISTANCE;
    private static final ModConfigSpec.DoubleValue BAILIFF_SPEED;
    private static final ModConfigSpec.DoubleValue BAILIFF_FLEE_SPEED;
    private static final ModConfigSpec.IntValue QUOTA;
    private static final ModConfigSpec.IntValue SECONDS_ON_SITE;
    private static final ModConfigSpec.IntValue SECONDS_TO_VANISH;
    private static final ModConfigSpec.IntValue SEALS_MIN;
    private static final ModConfigSpec.IntValue SEALS_MAX;
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
        FEEDING_PER_HEART = builder
                .comment("What feeding a hurt tamed pet the player owns takes off for each whole heart it regains,",
                        "so feeding a healthy pet takes off nothing.")
                .defineInRange("feedingPerHeart", 1, 0, 1_000_000);
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
                .comment("Where the Tally Stick starts reading Marked, as a fraction of the player's Threshold.")
                .defineInRange("marked", 0.5, 0, 1);
        OVERDUE = builder
                .comment("Where the Tally Stick starts reading Overdue, as a fraction of the player's Threshold.")
                .defineInRange("overdue", 0.75, 0, 1);
        FORFEIT = builder
                .comment("Where the Tally Stick starts reading Forfeit, as a fraction of the player's Threshold.")
                .defineInRange("forfeit", 0.9, 0, 1);
        builder.pop();

        builder.push("bailiff");
        ARRIVAL_MIN_DISTANCE = builder
                .comment("The nearest, in blocks across the ground, that a Bailiff arrives to its player.")
                .defineInRange("arrivalMinDistance", 12, 1, 64);
        ARRIVAL_MAX_DISTANCE = builder
                .comment("The farthest, in blocks across the ground, that a Bailiff arrives from its player.")
                .defineInRange("arrivalMaxDistance", 24, 1, 64);
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
        QUOTA = builder
                .comment("How many stacks a Bailiff takes on a visit before it flees.")
                .defineInRange("quota", 5, 1, 64);
        SECONDS_ON_SITE = builder
                .comment("How many seconds after it arrives a Bailiff flees, whatever it has taken.")
                .defineInRange("secondsOnSite", 60, 1, 3600);
        SECONDS_TO_VANISH = builder
                .comment("How many seconds out of its player's sight a fleeing Bailiff vanishes after, with what it carries.")
                .defineInRange("secondsToVanish", 3, 0, 600);
        SEALS_MIN = builder
                .comment("The fewest Seals a killed Bailiff drops.")
                .defineInRange("sealsMin", 1, 0, 64);
        SEALS_MAX = builder
                .comment("The most Seals a killed Bailiff drops. Below the fewest, the fewest drop every time.")
                .defineInRange("sealsMax", 3, 0, 64);
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

    public static int decayPerDay() {
        return DECAY_PER_DAY.getAsInt();
    }

    public static int breeding() {
        return BREEDING.getAsInt();
    }

    public static int taming() {
        return TAMING.getAsInt();
    }

    public static int feedingPerHeart() {
        return FEEDING_PER_HEART.getAsInt();
    }

    public static int curing() {
        return CURING.getAsInt();
    }

    public static int gift() {
        return GIFT.getAsInt();
    }

    public static int giftsPerDay() {
        return GIFTS_PER_DAY.getAsInt();
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

    public static int arrivalMinDistance() {
        return ARRIVAL_MIN_DISTANCE.getAsInt();
    }

    public static int arrivalMaxDistance() {
        return Math.max(ARRIVAL_MIN_DISTANCE.getAsInt(), ARRIVAL_MAX_DISTANCE.getAsInt());
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

    public static int quota() {
        return QUOTA.getAsInt();
    }

    public static int ticksOnSite() {
        return SECONDS_ON_SITE.getAsInt() * 20;
    }

    public static int ticksToVanish() {
        return SECONDS_TO_VANISH.getAsInt() * 20;
    }

    public static int sealsMin() {
        return SEALS_MIN.getAsInt();
    }

    public static int sealsMax() {
        return Math.max(SEALS_MIN.getAsInt(), SEALS_MAX.getAsInt());
    }

    /** The Tally Stick's uses, or the default before the config has loaded, as when items are first built. */
    public static int tallyStickUses() {
        return SPEC.isLoaded() ? TALLY_STICK_USES.getAsInt() : TALLY_STICK_USES.getDefault();
    }
}

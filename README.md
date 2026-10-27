# A Touch of Wickedness

![A Touch of Wickedness](https://raw.githubusercontent.com/5thlayer/atouchofwickedness/main/publish/atouchofwickedness-cover.png)

<!-- For players, written by the maintainer and never by an agent: what A Touch of Wickedness is, why it exists
and how it feels to play. It is also the Modrinth and CurseForge description
(docs/agents/publishing.md). The technical sections go below it. -->

Killing harmless creatures builds Wickedness, and too much of it summons a punisher.

## For pack authors

- **What a kill weighs** is the data map `atouchofwickedness:wickedness_weight` on entity types, each entry a whole number: by default 2 for livestock and fish, 5 for wild animals, 15 for rare and companion creatures, 30 for a villager and 50 for a cat or ocelot. A creature the map doesn't list weighs nothing, so a pack can weigh another mod's creatures by adding them. Any tamed creature weighs `weights.tamed` whatever the map says, a baby `weights.babyMultiplier` times its weight, and a neutral mob that went for its killer before they struck it nothing.
- **What an item is worth to the Bailiff** is the data map `atouchofwickedness:bailiff_value` on items, keyed by item or item tag. The Bailiff takes one stack, each as likely as its share of what all the stacks it may take are worth, a stack being worth its Value times its count, plus `bailiff.valuePerEnchantmentLevel` per enchantment level. An unlisted item is worth `bailiff.unlistedValue`, and one valued 0 is never taken. By default stone, dirt, sand, gravel, netherrack, rotten flesh and seeds are worth nothing, and the scale runs from food and lapis at 5 to netherite gear at 900.
- **Which containers the Bailiff robs** is the block entity type tag `atouchofwickedness:seizable`, by default chests, trapped chests and barrels. A Bailiff robs only a container its own player opened. Ender chests, shulker boxes and Strongboxes are never robbed, even if a pack adds them to the tag.
- **What repairs a Tally Stick** in an anvil is the item tag `atouchofwickedness:repairs_tally_stick`, by default the Seal.
- **The Bailiff's drops** are the loot table `atouchofwickedness:entities/bailiff`: the Bailiff's Bag, under the loot condition `atouchofwickedness:bag_chance`, which reads `bailiff.bagChance` and `bailiff.bagChancePerLootingLevel`. The Seals and the stolen haul drop outside the table.
- **The recipes** are `atouchofwickedness:strongbox` (four Seals around a chest) and `atouchofwickedness:tally_stick` (a Seal and a stick), so a datapack can replace them.

Wickedness, the Threshold, the Bailiff's cooldown and the containers a player opened are kept on the player and survive death and restarts. The Bailiff itself is never saved: it is gone once its player logs out or changes dimension, and a visit it left unfinished is still owed.

The terms are in [GLOSSARY.md](GLOSSARY.md), and what each release changed for a pack is in [CHANGELOG.md](CHANGELOG.md).

## Commands

All at permission level 2:

- `/wickedness get <player>` shows a player's Wickedness.
- `/wickedness set <player> <value>` and `/wickedness add <player> <value>` change it. Reaching the Threshold sends the Bailiff as a kill would.
- `/wickedness threshold <player>` shows the Threshold they drew.

## Configuration

Everything is in one common config, `config/atouchofwickedness-common.toml`. Times are in seconds.

- **`weights`**: `tamed` (50), `babyMultiplier` (2), and `hitCost` (3), what each damaging hit short of the killing blow adds on a creature weighing `tormentWeight` (30) or more.
- **`gossip`**: villagers within `radius` (16) who see a Wicked kill gain `scale` (5) times its weight in vanilla's minor negative gossip about the killer. 0 turns it off.
- **`decay`**: `perDay` (10), the Wickedness that falls each in-game day while the player is online.
- **`atonement`**: what kind acts take off: `breeding` (3), `taming` (10), `healingPerHeart` (1), `curing` (40), and `gift` (2), at most `giftsPerDay` (5) times a day.
- **`threshold`**: `min` (50) and `max` (100), the range each player's Threshold is drawn from.
- **`standing`**: where Marked (0.5), Overdue (0.75) and Forfeit (0.9) start, as fractions of the Threshold.
- **`warnings`**: how often each Warning sounds (`secondsBetweenMarked` 120, `secondsBetweenOverdue` 60, `secondsBetweenForfeit` 30) and how far off (`markedDistance` 24, `overdueDistance` 12, `forfeitDistance` 4). Also how near animals flee from a Forfeit player (`fleeRadius` 16) and how fast (`animalFleeSpeed` 1.0, `animalFleeSprintSpeed` 1.4).
- **`bailiff`**:
  - Arriving: how far from its player it arrives (`arrivalMinDistance` 12, `arrivalMaxDistance` 24), the wait before another may come (`secondsBetweenVisits` 300), and how often an owed visit retries (`secondsBetweenRetries` 3).
  - Its stats: `health` (60), `damage` on Normal (4), `knockbackResistance` (1), `speed` (0.35) and `fleeSpeed` (1.4).
  - What it robs: the containers remembered per player (`containersRemembered` 32), how near where it arrived they must be (`robbingRadius` 32), how near a ground item must be (`pickupRadius` 6) and what it must be worth (`pickupFloor` 5), plus `unlistedValue` (1) and `valuePerEnchantmentLevel` (20).
  - Leaving: how long it stays (`secondsOnSite` 60), how far it flees before it stops (`vanishDistance` 24), and how long it charges its teleport (`secondsToTeleport` 2).
  - Its drops: the Seals it drops (`sealsMin` 1, `sealsMax` 3), and the chance of a Bailiff's Bag (`bagChance` 0.05, plus `bagChancePerLootingLevel` 0.01).
- **`tallyStick`**: `uses` (16).

The numbers are provisional while A Touch of Wickedness is below 1.0.

## Compatibility

All optional, and none nested in the jar:

- **[Jade](https://www.curseforge.com/minecraft/mc-mods/jade)** shows, when you look at a Bailiff, the player it came for and the stacks it has taken, and never anyone's Wickedness.
- **[A Touch of Madness](https://github.com/5thlayer/atouchofmadness)**: a Bailiff nearby drains Sanity like any hostile mob, through an optional entry in `#atouchofmadness:unsettling`.

## Dependencies

None beyond NeoForge, on Minecraft 26.1.2.

# Changelog

Written for Consumers: what a mod building against A Touch of Wickedness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- Killing a creature in melee adds its weight to the killer's hidden Wickedness: 2 for livestock, 5 for wild animals, 15 for rare and companion creatures, 30 for a villager and 50 for a cat or ocelot. The weights are the data map `atouchofwickedness:wickedness_weight` on entity types, unlisted meaning 0, so a datapack can weigh any creature.
- Each player draws a hidden Threshold from 50 to 100. When their Wickedness reaches it, a Bailiff arrives 12–24 blocks away, Wickedness resets to 0 and a new Threshold is drawn.
- The Bailiff goes for its player and each hit that lands takes one random stack from the main inventory or hotbar, never armour or the off hand. After 5 stacks or 60 s it flees, and vanishes with what it took once out of sight. Killed, it drops what it stole and 1–3 Seals.
- A Seal and a stick craft a Tally Stick, which shows the player's Standing (Clean, Marked, Overdue or Forfeit) in the action bar, wears 1 of 16 uses each time, and repairs with a Seal in an anvil.
- `/wickedness get|set|add <player> [value]` and `/wickedness threshold <player>`, at permission level 2.
- Every number of the loop is in the common config, `atouchofwickedness-common.toml`.

- The Pillar has its own creative tab, "A Touch of Wickedness", and no longer nests `libworks-runtime` or fills its Works tab.

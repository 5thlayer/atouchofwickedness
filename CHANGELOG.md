# Changelog

Written for Consumers: what a mod building against A Touch of Wickedness can use, or will see change. A published version never changes: a fix is the next patch (`docs/agents/releases.md`).

## Unreleased

- Killing a creature adds its weight to the killer's hidden Wickedness: 2 for livestock, 5 for wild animals, 15 for rare and companion creatures, 30 for a villager and 50 for a cat or ocelot. The weights are the data map `atouchofwickedness:wickedness_weight` on entity types, unlisted meaning 0, so a datapack can weigh any creature.
- A kill is credited as vanilla credits its experience: to the player who last hurt the creature, by arrow, trident or a trap they knocked it into too, or to the owner of the tamed pet that killed it. A kill no player had a hand in counts for no one, and Creative and Spectator players gain nothing and are never visited.
- Any tamed creature weighs 50 whoever owns it, a baby double, and a neutral mob that went for its killer before they struck it nothing (an iron golem always counts). Each damaging hit short of the killing blow on a creature weighing 30 or more adds 3. The tamed weight, the baby multiplier, the hit cost and the weight it starts from are in the common config.
- Dying leaves Wickedness and the Threshold unchanged.
- Wickedness falls by 10 each in-game day while the player is online, a point at a time, a night slept through included, and never below 0.
- Kind acts take Wickedness off at once, credited to whoever did them: breeding two animals 3, taming one 10, feeding a hurt tamed pet the player owns 1 per heart it regains, curing a zombie villager 40, and a villager picking up food the player threw 2, at most 5 times a day. Creative and Spectator players atone for nothing.
- Each player draws a hidden Threshold from 50 to 100. When their Wickedness reaches it, a Bailiff arrives 12–24 blocks away, Wickedness resets to 0 and a new Threshold is drawn.
- The Bailiff goes for its player and each hit that lands takes one random stack from the main inventory or hotbar, never armour or the off hand. After 5 stacks or 60 s it flees, and vanishes with what it took once out of sight. Killed, it drops what it stole and 1–3 Seals.
- A Seal and a stick craft a Tally Stick, which shows the player's Standing (Clean, Marked, Overdue or Forfeit) in the action bar, wears 1 of 16 uses each time, and repairs with a Seal in an anvil.
- `/wickedness get|set|add <player> [value]` and `/wickedness threshold <player>`, at permission level 2.
- Every number of the loop is in the common config, `atouchofwickedness-common.toml`.

- The Pillar has its own creative tab, "A Touch of Wickedness", and no longer nests `libworks-runtime` or fills its Works tab.

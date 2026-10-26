---
status: provisional
---

# The Bailiff seizes only containers its player has opened

The Bailiff robs a container its player opened instead of robbing the player, when there is one, but vanilla containers have no owner, so on a server "the chests near the player" would include an innocent neighbour's. The Pillar remembers, per player, the positions of the last ~32 Seizable containers that player opened, and the Bailiff robs only those, within 32 blocks of where it arrives. Seizable is the tag `#atouchofwickedness:seizable` (chests, trapped chests and barrels by default); ender chests, shulker boxes and the Strongbox never are.

**Considered.** Any container within a radius: simplest, and fine in single player, but it punishes whoever builds next to a wicked player. Different rules in single player and on a server: two behaviours to test and explain. Owner tracking by placement: misses world-generated and pre-existing containers, and needs data on every block entity rather than a short list on the player.

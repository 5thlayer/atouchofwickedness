# A Touch of Wickedness

The Pillar of A Touch Of that owns Wickedness: a hidden, per-player tally of the harmless creatures a player has killed, which fades with time, and which brings a thief to rob them when it runs too high. The terms the Pillars share are in the suite glossary (`5thlayer/atouchof`'s `GLOSSARY.md`); this file holds the terms only this Pillar uses.

## Language

### Parties

**A Touch of Wickedness**:
This Pillar, `atouchofwickedness`.
_Avoid_: Wickedness mod, Module

**A Touch Of**:
The suite this Pillar belongs to, with A Touch of Darkness and A Touch of Madness.
_Avoid_: FactoryWorks (a different suite), the pack

**Pillar**:
One of the suite's mods, each owning one danger and playable alone in vanilla.
_Avoid_: Module (a FactoryWorks mod), Consumer, addon

**Runtime**:
The small mod every Pillar nests jar-in-jar, which draws the Pillars' meters and carries the suite's neutral signals, and holds no mechanic.
_Avoid_: Core, base mod, library

### Wickedness

**Wickedness**:
A per-player amount of harm done to creatures that posed no threat; the player never sees it as a number on the HUD, only through warnings and what reads it.
_Avoid_: Naughtiness (Klei's name), karma, sin, a meter (it isn't drawn)

**Wicked kill**:
The death of a non-hostile creature credited to a player, which adds that creature's weight to the player's Wickedness.
_Avoid_: crime, sin, bad deed

**Decay**:
The slow, steady fall of Wickedness over time, so a modest rate of killing, such as farming, never adds up.
_Avoid_: forgiveness, cooldown

### Dev runs

**Game-test pack**:
A datapack only this Pillar's game tests switch on, holding the data a pack would supply, such as a tag's entries or a recipe, so a test can stand on it. Never in what players download.
_Avoid_: test datapack, gametest datapack

**Dev pack**:
A datapack only this Pillar's dev client switches on, holding the data a pack would supply, so a feature a pack turns on with data can be tried by hand. Never in what players download.
_Avoid_: debug pack, test pack

### Shipped packs

**Gallery pack**:
A datapack in this Pillar's jar, off until a player turns it on, whose functions build a working scene of each of the Pillar's features, each with a sign naming it. Functions only: turning it on changes nothing about how the Pillar plays (libworks ADR 0010).
_Avoid_: demo pack, showcase pack, gallery datapack

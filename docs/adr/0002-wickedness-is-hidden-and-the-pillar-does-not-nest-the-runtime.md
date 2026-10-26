---
status: provisional
---

# Wickedness is hidden, and the Pillar does not nest the Runtime yet

Wickedness is never drawn as a meter. A player reads it only through the Warnings, which grow at ½, ¾ and 90% of the Threshold, and through the Tally Stick, which shows a coarse Standing and is crafted from the Seals a Bailiff leaves. The dread is in not knowing how far they can push, and the Threshold is redrawn at random after each visit for the same reason. Because nothing is drawn, the value lives in the Pillar's own server-side player attachment, and the Pillar does not nest the Runtime until it needs one of the Runtime's services. That is an exception to suite ADR-0002's "each Pillar nests the Runtime", filed on 5thlayer/atouchof.

**Considered.** A Runtime meter like Sanity: exact, but it turns the Threshold into a number to farm up to, and A Touch of Darkness already chose a state read from the world over a meter. Warnings alone, with no reading item: the jam draft's Karma Compass showed that players want to check, and making the Tally Stick from Seals ties reading Wickedness to having faced the Bailiff once. Nesting the Runtime for its per-player stat attachment alone: a dependency with nothing to draw.

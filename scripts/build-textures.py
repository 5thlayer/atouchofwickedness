#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: LGPL-3.0-only
"""Builds the Strongbox's chest sheet from Malcolm Riley's tiny stone chest in data/art/ (NOTICE).

chest_stone_tiny.png is a 16x16 sheet of a tiny chest's four 8x8 faces: its top at the top left, a side
at the top right, its front with the latch at the bottom left and its back with two hinges at the bottom
right. On the front, the sides and the back, row 3 is the seam between lid and base: rows 0 to 3 are the
lid, rows 3 to 7 the base.

Each face is stretched onto vanilla's chest model, whose faces are 14 texels wide: its 1-texel border is
kept and each texel inside it doubled across, and its rows spread down the lid's 5 or the base's 10. The
lid's bottom row and the base's top row overlap on the model, so both draw the seam row, or they z-fight.
The floor and the lid's underside, seen with the lid open, are the top darkened, as a chest is inside.
The latch and its shadow are painted out of the front with the side's planks beneath, since the lock is a
cube of its own: a crimson Seal, drawn here.

The sheet is box UV, and vanilla's chest shows each face turned half round: a face's top row is at the
bottom of its slot, and its left column at the right.

Run from the repository root; it needs Pillow. With --check it writes nothing and fails when the
committed sheet's pixels differ from what it builds.
"""

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "data" / "art"
SHEET = ROOT / "src" / "main" / "resources" / "assets" / "atouchofwickedness" / "textures" / "entity" / "chest" / "strongbox.png"

SOURCE = "chest_stone_tiny.png"
FACE = 8
# Vanilla's chest model: the lid and the base are as wide and as deep, and overlap by a row.
WIDTH = 14
LID = 5
BASE = 10
SEAM = 3
# The front's latch and the shadow it casts, as (x, y) on the front.
LATCH = [(3, 1), (3, 2), (4, 2), (3, 3), (4, 3), (3, 4), (4, 4), (5, 4), (6, 4), (3, 5), (4, 5)]
# How light the inside is against the outside.
INSIDE_LIGHT = 0.6

# The lock's corner of the sheet, at (0, 0), its slots laid out as box UV lays them: a Seal's crimson
# with a gold band round its top, which shows at the top of the lock once the face is turned.
SEAL_COLOURS = {
    "g": (232, 180, 90, 255),   # gold
    "s": (96, 14, 20, 255),     # shadow
    "c": (140, 22, 30, 255),    # crimson
    "h": (190, 52, 48, 255),    # highlight
}
SEAL = [
    ".ggss.",
    "ssssss",
    "cccccc",
    "chhccc",
    "gggggg",
]


def faces():
    source = Image.open(ART / SOURCE).convert("RGBA")
    top, side, front, back = (source.crop((x, y, x + FACE, y + FACE)) for x, y in ((0, 0), (8, 0), (0, 8), (8, 8)))
    for x, y in LATCH:
        front.putpixel((x, y), side.getpixel((x, y)))
    return top, side, front, back


def spread(size, length):
    """Which source texel each of length texels shows: the first and last kept, those between spread over the rest."""
    inner, span = size - 2, length - 2
    return [0] + [1 + i * inner // span for i in range(span)] + [size - 1]


def stretch(face, width, height, rows):
    out = Image.new("RGBA", (width, height))
    columns, source_rows = spread(face.width, width), spread(len(rows), height)
    for y in range(height):
        for x in range(width):
            out.putpixel((x, y), face.getpixel((columns[x], rows[source_rows[y]])))
    return out


def lid(face):
    return stretch(face, WIDTH, LID, range(SEAM + 1))


def base(face):
    return stretch(face, WIDTH, BASE, range(SEAM, FACE))


def darkened(face):
    out = face.copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            out.putpixel((x, y), (round(r * INSIDE_LIGHT), round(g * INSIDE_LIGHT), round(b * INSIDE_LIGHT), a))
    return out


def lay(sheet, u, v, width, height, depth, down, up, west, north, east, south):
    """Lays a box's faces into its box-UV slots, each turned half round as the chest shows it."""
    for face, x, y in (
        (down, u + depth, v),
        (up, u + depth + width, v),
        (west, u, v + depth),
        (north, u + depth, v + depth),
        (east, u + depth + width, v + depth),
        (south, u + depth + width + depth, v + depth),
    ):
        sheet.paste(face.rotate(180), (x, y))


def seal():
    out = Image.new("RGBA", (len(SEAL[0]), len(SEAL)))
    for y, row in enumerate(SEAL):
        for x, colour in enumerate(row):
            if colour != ".":
                out.putpixel((x, y), SEAL_COLOURS[colour])
    return out


def strongbox():
    top, side, front, back = faces()
    top = stretch(top, WIDTH, WIDTH, range(FACE))
    inside = darkened(top)
    sheet = Image.new("RGBA", (64, 64))
    # The lid, at (0, 0): seen from above, its top; from below, with the lid open, its underside.
    lay(sheet, 0, 0, WIDTH, LID, WIDTH, inside, top, lid(side), lid(back), lid(side), lid(front))
    # The base, at (0, 19): its bottom, and the floor seen with the lid open.
    lay(sheet, 0, WIDTH + LID, WIDTH, BASE, WIDTH, top, inside, base(side), base(back), base(side), base(front))
    sheet.paste(seal(), (0, 0))
    return sheet


def main():
    image = strongbox()
    if "--check" in sys.argv:
        # Pixels, not the file's bytes, which another Pillow may encode differently.
        current = SHEET.is_file() and Image.open(SHEET).convert("RGBA").tobytes() == image.tobytes()
        print(("ok   %s is current" if current else "FAIL %s is stale -- re-run scripts/build-textures.py")
              % SHEET.relative_to(ROOT))
        return 0 if current else 1
    image.save(SHEET)
    print(f"wrote {SHEET.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

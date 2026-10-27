# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, a great Seal stamped on the title's ribbon among the Bailiff's spoils, in the -works
# suite's style (A Touch of Darkness's make-cover.py), until an in-game screenshot or an artist's art
# replaces them. The Bailiff isn't drawn: it is vanilla's wandering trader retinted, which the cover
# can't hold (libworks ADR 0012). They carry Malcolm Riley's art through the Strongbox (NOTICE). Run
# from the repo root with Pillow installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageFont

T = 'src/main/resources/assets/atouchofwickedness/textures/'
BG = (10, 6, 8)
ACCENT = (196, 86, 255)  # violet, A Touch of Darkness's, the suite's
HAZE = (200, 38, 52)  # crimson, the Seal's
GOLD = (236, 176, 72)  # the Seal's heart


def item(name, px):
    return Image.open(f'{T}item/{name}.png').convert('RGBA').resize((px, px), Image.NEAREST)


def strongbox(px):
    # the Strongbox seen from the front, from its chest sheet as vanilla's chest model lays it out: the
    # lid's front over the base's, and the crimson Seal of its lock across the seam
    tex = Image.open(f'{T}entity/chest/strongbox.png').convert('RGBA')
    im = Image.new('RGBA', (14, 15))
    im.alpha_composite(tex.crop((14, 14, 28, 19)), (0, 0))
    im.alpha_composite(tex.crop((14, 33, 28, 43)), (0, 5))
    im.alpha_composite(tex.crop((1, 1, 3, 5)), (6, 2))
    return im.resize((px, px * 15 // 14), Image.NEAREST)


SPOILS = [lambda px: item('seal', px), lambda px: item('tally_stick', px),
          lambda px: item('bailiffs_bag', px), strongbox]


def glow(rgb, r, strength=150):
    # a red haze that falls off to nothing
    im = Image.new('RGBA', (2 * r, 2 * r))
    for y in range(2 * r):
        for x in range(2 * r):
            d = math.hypot(x - r, y - r) / r
            if d < 1:
                im.putpixel((x, y), rgb + (int(strength * (1 - d) ** 2),))
    return im


def field(W, H, px, seed, clear):
    # the dark, with the spoils scattered through it, kept clear of the title band and of the great
    # Seal between the fractions of the width in clear
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v + 6, v - 4, v - 2))
    slots = [(x, y) for y in (0.17, 0.83) for x in (0.12, 0.31, 0.5, 0.69, 0.88)
             if not clear[0] <= x <= clear[1]]
    rng.shuffle(slots)
    for (fx, fy), draw in zip(slots, SPOILS * 3):
        cx, cy = int(fx * W + rng.uniform(-0.04, 0.04) * W), int(fy * H)
        if draw is SPOILS[0]:
            g = glow(HAZE, int(px * 1.8), 110)
            img.alpha_composite(g, (cx - g.width // 2, cy - g.height // 2))
        s = draw(px)
        img.alpha_composite(s, (cx - s.width // 2, cy - s.height // 2))
    return img


def stamp(img, cx, cy, size):
    # the great Seal pressed onto the ribbon like wax, in its own red haze
    g = glow(HAZE, int(size * 0.85), 170)
    img.alpha_composite(g, (cx - g.width // 2, cy - g.height // 2))
    s = item('seal', size)
    shadow = Image.new('RGBA', s.size, (0, 0, 0, 0))
    shadow.putalpha(s.getchannel('A').point(lambda a: a * 3 // 5))
    o = max(4, size // 24)
    img.alpha_composite(shadow, (cx - size // 2 + o, cy - size // 2 + o))
    img.alpha_composite(s, (cx - size // 2, cy - size // 2))


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, seal, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon. seal is the great Seal's middle and
    # size, as fractions of the height; the title is centred in the room the Seal leaves it, beside it
    # on the cover and below it on the icon
    sx, sy, ss = seal
    img = field(W, H, px, 11, (sx * H / W - ss * H / W, sx * H / W + ss * H / W))
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf'),
               '/usr/share/fonts/noto/NotoSans-Black.ttf'], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Apple Chancery.ttf', '/usr/share/fonts/gsfonts/Z003-MediumItalic.otf',
               '/usr/share/fonts/urw-base35/Z003-MediumItalic.otf'], int(title_size * (0.62 if stacked else 0.55)))
    # the -works title turned around: the suite's "A touch of" in a script, then the Pillar's word
    w, a = 'A touch of', 'WICKEDNESS'
    d = ImageDraw.Draw(img)
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw, ph = w2 + 2 * gap, (h2 + 2 * pad if stacked else h1)
    th = ph + gap + h1 if stacked else h1
    cy = int(H * (sy + ss * 0.62) + (th + title_size * 0.6) / 2) if stacked else H // 2
    bh = int(th + title_size * 0.6)
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, cy - bh // 2, W, cy + bh // 2], fill=(6, 3, 4, 240))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, cy - bh // 2, W, cy - bh // 2 + lt], fill=ACCENT)
    d.rectangle([0, cy + bh // 2 - lt, W, cy + bh // 2], fill=ACCENT)
    stamp(img, int(sx * H), int(sy * H), int(ss * H))
    d = ImageDraw.Draw(img)
    if stacked:
        x, bt = (W - bw) // 2, cy - th // 2
        tx, tt = (W - w1) // 2, bt + ph + gap
    else:
        room = int((sx - ss * 0.5) * H)
        x, bt = (room - (bw + gap + w1)) // 2, cy - h1 // 2
        tx, tt = x + bw + gap, bt
    qx, qy = x + gap - b2[0], bt + (ph - h2) // 2 - b2[1]
    d.text((qx + sh, qy + sh), w, font=f2, fill=(0, 0, 0))
    d.text((qx, qy), w, font=f2, fill=(240, 226, 210))
    ty = tt - b1[1]
    d.text((tx + sh - b1[0], ty + sh), a, font=f1, fill=(0, 0, 0))
    d.text((tx - b1[0], ty), a, font=f1, fill=ACCENT)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/atouchofwickedness-cover.png', 92, 64, (1.68, 0.56, 0.46))
make(512, 512, 'publish/atouchofwickedness-icon.png', 74, 48, (0.5, 0.3, 0.42), stacked=True)

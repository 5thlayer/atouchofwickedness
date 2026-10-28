# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, a great Seal stamped on the title's ribbon among the Bailiff's spoils, in the -works
# suite's style (A Touch of Darkness's make-cover.py), until an in-game screenshot or an artist's art
# replaces them. The Bailiff is vanilla's wandering trader retinted, so the cover holds only his
# shadow, drawn from the villager model's boxes rather than from Mojang's texture (libworks ADR 0012). They carry Malcolm Riley's art through the Strongbox (NOTICE). Run
# from the repo root with Pillow installed.
import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

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


def put(img, s, cx, cy):
    # a sprite pasted with its drawn pixels, not its canvas, centred on (cx, cy), so sprites that sit
    # off-centre in their 16x16 still line up
    l, t, r, b = s.getchannel('A').getbbox()
    img.alpha_composite(s, (cx - (l + r) // 2, cy - (t + b) // 2))


def bailiff(h):
    # the Bailiff's black shadow in profile, facing the title, from the villager model's boxes in its
    # 16ths of a block: the hooded head and its long nose, the arms crossed before him, the robe and
    # the legs under it, in a soft light grey glow so he stands out of the dark
    u = h / 35
    k = int(u * 4)
    im = Image.new('RGBA', (int(12 * u) + 2 * k, h + 2 * k))
    d = ImageDraw.Draw(im)
    for x0, y0, x1, y1 in [(0.5, 0, 9.5, 11), (9, 9.5, 11, 14), (2, 10.5, 8.5, 29), (3, 26, 7.5, 35)]:
        d.rectangle([k + x0 * u, k + y0 * u, k + x1 * u, k + y1 * u], fill=(0, 0, 0, 255))
    d.polygon([(k + x * u, k + y * u) for x, y in [(6, 13), (10.5, 15), (11.5, 19.5), (7, 21)]],
              fill=(0, 0, 0, 255))
    a = im.getchannel('A')
    halo = Image.new('RGBA', im.size, (190, 190, 196, 0))
    halo.putalpha(a.filter(ImageFilter.GaussianBlur(u * 1.6)).point(lambda v: v * 7 // 10))
    halo.alpha_composite(im)
    return halo


def field(W, H, px, seed, rows, right):
    # the dark, with the spoils set out once each, half above the band and half below it, in the same
    # columns between the left edge and right
    rng = random.Random(seed)
    img = Image.new('RGBA', (W, H), BG + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(W * H // 900):
        v = rng.randrange(10, 22)
        x, y = rng.randrange(W), rng.randrange(H)
        d.point((x, y), fill=(v + 6, v - 4, v - 2))
    n = len(SPOILS) // max(1, len(rows))
    for i, cy in enumerate(rows):
        for j in range(n):
            cx = int(right * (j + 0.5) / n)
            draw = SPOILS[i * n + j]
            if draw is SPOILS[0]:
                g = glow(HAZE, int(px * 1.8), 110)
                img.alpha_composite(g, (cx - g.width // 2, cy - g.height // 2))
            put(img, draw(px), cx, cy)
    return img


def stamp(img, cx, cy, size):
    # the great Seal pressed onto the ribbon like wax, in its own red haze
    g = glow(HAZE, int(size * 0.85), 170)
    img.alpha_composite(g, (cx - g.width // 2, cy - g.height // 2))
    s = item('seal', size)
    shadow = Image.new('RGBA', s.size, (0, 0, 0, 0))
    shadow.putalpha(s.getchannel('A').point(lambda a: a * 3 // 5))
    o = max(4, size // 24)
    put(img, shadow, cx + o, cy + o)
    put(img, s, cx, cy)


def font(names, size):
    for n in names:
        if os.path.exists(n):
            return ImageFont.truetype(n, size)
    raise SystemExit('needs one of ' + ', '.join(names))


def make(W, H, out, title_size, px, seal, stacked=False):
    # stacked puts "A touch of" above the word, for the square icon, with the great Seal above the band
    # and the two together centred in the height; otherwise the Seal sits on the band's right, centred
    # on it, and the title is centred in the room it leaves. seal is the Seal's size, as a fraction of
    # the height
    ss = int(seal * H)
    f1 = font(['/System/Library/Fonts/Supplemental/Impact.ttf', '/usr/share/fonts/TTF/impact.ttf',
               os.path.expanduser('~/.local/share/fonts/msfonts/impact.ttf'),
               '/usr/share/fonts/noto/NotoSans-Black.ttf'], title_size)
    f2 = font(['/System/Library/Fonts/Supplemental/Apple Chancery.ttf', '/usr/share/fonts/gsfonts/Z003-MediumItalic.otf',
               '/usr/share/fonts/urw-base35/Z003-MediumItalic.otf'], int(title_size * (0.62 if stacked else 0.55)))
    # the -works title turned around: the suite's "A touch of" in a script, then the Pillar's word
    w, a = 'A touch of', 'WICKEDNESS'
    d = ImageDraw.Draw(Image.new('RGBA', (1, 1)))
    b1, b2 = d.textbbox((0, 0), a, font=f1), d.textbbox((0, 0), w, font=f2)
    w1, w2, h1, h2 = b1[2] - b1[0], b2[2] - b2[0], b1[3] - b1[1], b2[3] - b2[1]
    pad, gap, sh = int(title_size * 0.16), int(title_size * 0.1), max(3, title_size // 20)
    bw, ph = w2 + 2 * gap, (h2 + 2 * pad if stacked else h1)
    th = ph + gap + h1 if stacked else h1
    bh = int(th + title_size * 0.6)
    if stacked:
        # the Seal's drawn height is 14/16 of its size; the gap between it and the band matches the
        # margins above and below
        sh_ = ss * 14 // 16
        m = (H - sh_ - bh) // 3
        sy, cy, sx = m + sh_ // 2, 2 * m + sh_ + bh // 2, W // 2
        room = W
    else:
        cy = sy = H // 2
        sx = W - int(H * 0.32)
        room = sx - ss // 2
    top, bot = cy - bh // 2, cy + bh // 2
    rows = [] if stacked else [top // 2, (bot + H) // 2]
    left = 0
    if not stacked:
        left = int(H * 0.4)
        room -= left
    img = field(W, H, px, 11, rows, room)
    if left:
        # room at the left for the Bailiff, who stands behind the ribbon
        shift = Image.new('RGBA', (W, H), BG + (255,))
        shift.alpha_composite(img.crop((0, 0, W - left, H)), (left, 0))
        img = shift
    # the Bailiff stands behind the ribbon and the Seal: at the cover's left, in the icon's middle
    b = bailiff(int(H * (0.62 if left else 0.94)))
    bx = left // 2 if left else W // 2
    img.alpha_composite(b, (bx - b.width // 2, H // 2 - b.height // 2))
    band = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, top, W, bot], fill=(6, 3, 4, 240))
    img = Image.alpha_composite(img, band)
    d = ImageDraw.Draw(img)
    lt = max(3, title_size // 25)
    d.rectangle([0, top, W, top + lt], fill=ACCENT)
    d.rectangle([0, bot - lt, W, bot], fill=ACCENT)
    stamp(img, sx, sy, ss)
    d = ImageDraw.Draw(img)
    if stacked:
        x, bt = (W - bw) // 2, cy - th // 2
        tx, tt = (W - w1) // 2, bt + ph + gap
    else:
        x, bt = left + (room - (bw + gap + w1)) // 2, cy - h1 // 2
        tx, tt = x + bw + gap, bt
    qx, qy = x + gap - b2[0], bt + (ph - h2) // 2 - b2[1]
    d.text((qx + sh, qy + sh), w, font=f2, fill=(0, 0, 0))
    d.text((qx, qy), w, font=f2, fill=(240, 226, 210))
    ty = tt - b1[1]
    d.text((tx + sh - b1[0], ty + sh), a, font=f1, fill=(0, 0, 0))
    d.text((tx - b1[0], ty), a, font=f1, fill=ACCENT)
    img.convert('RGB').save(out)


make(1280, 640, 'publish/atouchofwickedness-cover.png', 72, 64, 0.46)
make(512, 512, 'publish/atouchofwickedness-icon.png', 74, 48, 0.42, stacked=True)

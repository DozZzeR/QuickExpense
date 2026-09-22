"""Builds the QuickExpense launcher icon from the widget's money-bag art.

Composition (from the owner's description of the widget): the bag peeks in from the bottom-right,
cut off by the right and bottom edges, with the "$" about a third of its own width away from both
edges. Adaptive icons are 108x108 units; launchers show roughly the centre 72x72 through a mask.
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app", "src", "main", "res", "drawable", "widget_bag.png")
OUT = sys.argv[1] if len(sys.argv) > 1 else "."

# Measured in the source image (see conversation): real cut edges and the "$" glyph box.
CUT_RIGHT, CUT_BOTTOM = 530, 588
DOLLAR = (265, 271, 460, 494)  # left, top, right, bottom

BG_TOP = (0x16, 0x1A, 0x24)
BG_BOTTOM = (0x22, 0x34, 0x4D)


def load_bag():
    im = Image.open(SRC).convert("RGBA").crop((0, 0, CUT_RIGHT, CUT_BOTTOM))
    # Extend the flat cut edges outward by replicating the last column/row, so the seam sits
    # well outside the launcher mask instead of right on its edge.
    pad = 60
    w, h = im.size
    ext = Image.new("RGBA", (w + pad, h + pad))
    ext.paste(im, (0, 0))
    col = im.crop((w - 1, 0, w, h)).resize((pad, h))
    ext.paste(col, (w, 0))
    row = ext.crop((0, h - 1, w + pad, h)).resize((w + pad, pad))
    ext.paste(row, (0, h))
    return ext


def background(size):
    """Vertical dark-blue gradient plus a soft highlight top-left (as in widget_bg.xml)."""
    bg = Image.new("RGB", (size, size))
    d = ImageDraw.Draw(bg)
    for y in range(size):
        t = y / (size - 1)
        d.line([(0, y), (size, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM)))
    glow = Image.new("L", (size, size), 0)
    gd = ImageDraw.Draw(glow)
    r = size * 0.55
    cx, cy = size * 0.30, size * 0.25
    gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=34)
    glow = glow.filter(ImageFilter.GaussianBlur(size * 0.18))
    white = Image.new("RGB", (size, size), (255, 255, 255))
    return Image.composite(white, bg, glow).convert("RGBA")


def foreground(size, visible_right, visible_bottom, glyph_units, canvas_units=108.0):
    """Bag layer on a transparent canvas of `size` px representing `canvas_units` units.
    The "$" right/bottom edges land a third of the glyph width inside the visible edges."""
    bag = load_bag()
    upx = size / canvas_units
    gw_src = DOLLAR[2] - DOLLAR[0]
    scale = glyph_units * upx / gw_src
    bag = bag.resize((round(bag.width * scale), round(bag.height * scale)), Image.LANCZOS)
    gap = glyph_units / 3.0
    dollar_right = (visible_right - gap) * upx
    dollar_bottom = (visible_bottom - gap) * upx
    x = round(dollar_right - DOLLAR[2] * scale)
    y = round(dollar_bottom - DOLLAR[3] * scale)
    layer = Image.new("RGBA", (size, size))
    layer.alpha_composite(bag, (x, y)) if x >= 0 and y >= 0 else layer.paste(bag, (x, y), bag)
    return layer


def superellipse_mask(size, box, n=5.0):
    x0, y0, x1, y1 = box
    m = Image.new("L", (size, size), 0)
    px = m.load()
    cx, cy, a, b = (x0 + x1) / 2, (y0 + y1) / 2, (x1 - x0) / 2, (y1 - y0) / 2
    for y in range(size):
        for x in range(size):
            if abs((x - cx) / a) ** n + abs((y - cy) / b) ** n <= 1:
                px[x, y] = 255
    return m


def masks(size):
    u = size / 108
    box = (18 * u, 18 * u, 90 * u, 90 * u)
    circle = Image.new("L", (size, size), 0)
    ImageDraw.Draw(circle).ellipse(box, fill=255)
    rsq = Image.new("L", (size, size), 0)
    ImageDraw.Draw(rsq).rounded_rectangle(box, radius=72 * u * 0.22, fill=255)
    return {"circle": circle, "squircle": superellipse_mask(size, box), "rounded": rsq}


VARIANTS = {
    # A: faithful to the widget — "$" a third of its width from the visible square's edges.
    "A": dict(visible_right=90, visible_bottom=90, glyph_units=22),
    # B: pulled in so the whole "$" stays inside the 66-unit safe circle (circle masks too).
    "B": dict(visible_right=84, visible_bottom=84, glyph_units=19),
}


def full_icon(size, variant):
    img = background(size)
    img.alpha_composite(foreground(size, **VARIANTS[variant]))
    return img


if __name__ == "__main__":
    S = 324
    tiles = []
    for v in VARIANTS:
        icon = full_icon(S, v)
        for name, m in masks(S).items():
            t = Image.new("RGBA", (S, S), (235, 235, 235, 255))
            t.paste(icon, (0, 0), m)
            t = t.crop((int(S * 14 / 108), int(S * 14 / 108), int(S * 94 / 108), int(S * 94 / 108)))
            tiles.append((v, name, t))
    tw = tiles[0][2].width
    sheet = Image.new("RGBA", (tw * 3 + 40, tw * 2 + 30 + 120), (250, 250, 250, 255))
    d = ImageDraw.Draw(sheet)
    for i, (v, name, t) in enumerate(tiles):
        r, c = divmod(i, 3)
        sheet.paste(t, (10 + c * (tw + 10), 10 + r * (tw + 10)))
        d.text((14 + c * (tw + 10), 12 + r * (tw + 10)), f"{v} / {name}", fill=(255, 255, 255, 255))
    # actual launcher size check (48dp ~ 144px on xxhdpi; also tiny 48px)
    y0 = 20 + 2 * (tw + 10)
    for j, v in enumerate(VARIANTS):
        icon = full_icon(S, v)
        m = masks(S)["squircle"]
        t = Image.new("RGBA", (S, S), (250, 250, 250, 255))
        t.paste(icon, (0, 0), m)
        t = t.crop((int(S * 18 / 108), int(S * 18 / 108), int(S * 90 / 108), int(S * 90 / 108)))
        for k, px in enumerate((96, 48)):
            sheet.paste(t.resize((px, px), Image.LANCZOS), (10 + j * 260 + k * 110, y0))
        d.text((10 + j * 260, y0 + 100), f"{v} small", fill=(40, 40, 40, 255))
    sheet.save(f"{OUT}/icon_preview.png")
    print("preview written")

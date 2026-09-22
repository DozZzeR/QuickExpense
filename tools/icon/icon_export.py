"""Writes the launcher-icon resources and the 512x512 Play Store icon (variant A).

Usage:  pip install pillow && python tools/icon/icon_export.py
(python tools/icon/icon_gen.py <dir> writes a preview sheet of the variants under different masks.)
"""
import os
from PIL import Image
import icon_gen as g

RES = os.path.join(g.ROOT, "app", "src", "main", "res")
STORE = os.path.join(g.ROOT, "fastlane", "metadata", "android", "en-US", "images")
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
A = g.VARIANTS["A"]

master = g.foreground(1728, **A)  # 108 units at 16 px/unit, downscaled per density


def monochrome(fg):
    """Bag silhouette for themed icons, with the gold "$" and rope knocked out so they still read.
    "Goldness" is graded (warm hue: red well above blue) rather than a hard threshold, then the
    knock-out mask is closed and softened so the cut-out edges come out clean."""
    from PIL import ImageFilter
    r, gg, b, a = fg.split()
    rp, gp, bp = r.load(), gg.load(), b.load()
    gold = Image.new("L", fg.size, 0)
    gpx = gold.load()
    for y in range(fg.height):
        for x in range(fg.width):
            warm = rp[x, y] - bp[x, y]
            bright = rp[x, y]
            v = min(1.0, max(0.0, (warm - 35) / 45)) * min(1.0, max(0.0, (bright - 90) / 50))
            gpx[x, y] = round(v * 255)
    gold = gold.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.MinFilter(5))  # close gaps
    gold = gold.filter(ImageFilter.GaussianBlur(1.2))
    keep = Image.eval(gold, lambda v: 255 - v)
    from PIL import ImageChops
    alpha = ImageChops.multiply(a, keep)
    white = Image.new("L", fg.size, 255)
    return Image.merge("RGBA", (white, white, white, alpha))


mono_master = monochrome(g.foreground(864, **A))

for name, d in DENSITIES.items():
    size = round(108 * d)
    folder = os.path.join(RES, f"mipmap-{name}")
    os.makedirs(folder, exist_ok=True)
    master.resize((size, size), Image.LANCZOS).save(os.path.join(folder, "ic_launcher_foreground.png"), optimize=True)
    mono_master.resize((size, size), Image.LANCZOS).save(os.path.join(folder, "ic_launcher_monochrome.png"), optimize=True)
    print("wrote", folder, size)

# Play Store icon: the visible 72x72 centre of the full icon, as a 512x512 opaque PNG
# (Play applies its own rounded-corner mask).
os.makedirs(STORE, exist_ok=True)
full = g.full_icon(768, "A")
u = 768 / 108
store = full.crop((round(18 * u), round(18 * u), round(90 * u), round(90 * u))).resize((512, 512), Image.LANCZOS)
store.convert("RGB").save(os.path.join(STORE, "icon.png"), optimize=True)
print("wrote store icon", os.path.join(STORE, "icon.png"))

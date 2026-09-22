"""Play Store feature graphic (1024x500) per store language.

Usage:  pip install pillow && python tools/icon/feature_graphic.py
Uses Segoe UI from Windows; pass other .ttf paths via FONT_BOLD / FONT_REGULAR env vars.
"""
import os
from PIL import Image, ImageDraw, ImageFont
import icon_gen as g

W, H = 1024, 500
FONT_BOLD = os.environ.get("FONT_BOLD", r"C:\Windows\Fonts\seguisb.ttf")
FONT_REGULAR = os.environ.get("FONT_REGULAR", r"C:\Windows\Fonts\segoeui.ttf")

TAGLINES = {
    "en-US": ("Log it in two taps.", "Sort it out later."),
    "ru-RU": ("Запишите в два касания.", "Разберите потом."),
    "sr": ("Упишите у два додира.", "Средите касније."),
}


def banner(lines):
    bg = g.background(W).resize((W, W)).crop((0, (W - H) // 2, W, (W - H) // 2 + H))
    img = bg.copy()

    # The bag peeks in from the bottom-right corner, as on the icon and the widget.
    bag = g.load_bag()
    gw_px = 150  # "$" glyph width on the banner
    scale = gw_px / (g.DOLLAR[2] - g.DOLLAR[0])
    bag = bag.resize((round(bag.width * scale), round(bag.height * scale)), Image.LANCZOS)
    gap = gw_px / 3
    x = round(W - gap - g.DOLLAR[2] * scale)
    y = round(H - gap - g.DOLLAR[3] * scale)
    img.alpha_composite(bag, (x, y))

    d = ImageDraw.Draw(img)
    title = ImageFont.truetype(FONT_BOLD, 84)
    sub = ImageFont.truetype(FONT_REGULAR, 38)
    left, top = 72, 150
    d.text((left, top), "QuickExpense", font=title, fill=(255, 255, 255, 255))
    ty = top + 118
    for line in lines:
        d.text((left, ty), line, font=sub, fill=(255, 255, 255, 205))
        ty += 52
    return img.convert("RGB")


if __name__ == "__main__":
    for locale, lines in TAGLINES.items():
        out = os.path.join(g.ROOT, "fastlane", "metadata", "android", locale, "images")
        os.makedirs(out, exist_ok=True)
        path = os.path.join(out, "featureGraphic.png")
        banner(lines).save(path, optimize=True)
        print("wrote", path)

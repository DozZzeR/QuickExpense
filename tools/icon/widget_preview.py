"""Static preview of the home-screen widget, for the launcher's widget picker.

Usage:  pip install pillow && python tools/icon/widget_preview.py

Android generates a preview from previewLayout on its own, but some launchers (MIUI among them)
only list widgets that ship a previewImage — without it the widget cannot be added at all there.
"""
import os
from PIL import Image, ImageDraw, ImageFont
import icon_gen as g

W, H = 720, 360  # ~2x1 cells at xhdpi
RADIUS = 36
FONT_BOLD = os.environ.get("FONT_BOLD", r"C:\Windows\Fonts\seguisb.ttf")
FONT_REGULAR = os.environ.get("FONT_REGULAR", r"C:\Windows\Fonts\segoeui.ttf")


def preview():
    card = g.background(W).crop((0, (W - H) // 2, W, (W - H) // 2 + H)).convert("RGBA")

    bag = g.load_bag()
    bag_h = int(H * 0.72)
    scale = bag_h / bag.height * 1.35
    bag = bag.resize((round(bag.width * scale), round(bag.height * scale)), Image.LANCZOS)
    card.alpha_composite(bag, (W - bag.width + 30, H - bag.height + 30))

    d = ImageDraw.Draw(card)
    d.text((44, 52), "Expenses", font=ImageFont.truetype(FONT_REGULAR, 30), fill=(255, 255, 255, 205))
    d.text((44, 108), "1 234,00 RSD", font=ImageFont.truetype(FONT_BOLD, 54), fill=(255, 255, 255, 255))
    d.text((44, 186), "per day", font=ImageFont.truetype(FONT_REGULAR, 28), fill=(255, 255, 255, 170))

    # Rounded corners, so the preview matches the widget's own background shape.
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, W - 1, H - 1), radius=RADIUS, fill=255)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    out.paste(card, (0, 0), mask)
    return out


if __name__ == "__main__":
    folder = os.path.join(g.ROOT, "app", "src", "main", "res", "drawable-nodpi")
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, "widget_preview.png")
    preview().save(path, optimize=True)
    print("wrote", path)

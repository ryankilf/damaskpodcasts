#!/usr/bin/env python3
"""
Generates every app icon from one definition of the design's logo D2, "Woven play on damask"
(spec-and-design/damask-design/canvas/LogoStrandsDamask.dc.html).

The artboard draws on a 108-unit square, which is exactly Android's adaptive-icon grid:
  - ground: brand teal with a tone-on-tone diaper pattern (diamonds every 9 units, 16% opacity);
  - mark: a rounded play triangle woven from three horizontal strands (light, accent, light)
    with gaps between them that show the ground.

Outputs (paths relative to the repo root; run from the repo root):
  - Android adaptive icon: vector background / foreground / monochrome (Android 13+ themed icons)
  - iOS AppIcon: light, dark and tinted 1024px PNGs
  - Desktop: window-icon PNG, macOS .icns, Windows .ico, Linux PNG
  - branding/icon/out/store-512.png for store listings

Requires Pillow. Re-run after changing the logo, then commit the results.
"""
import os
import shutil
import subprocess
import tempfile

from PIL import Image, ImageDraw

GRID = 108
SUPERSAMPLE = 4

# Colours from the artboard (full colour and dark variants).
TEAL = (0x00, 0x6A, 0x63)
WHITE = (0xFF, 0xFF, 0xFF)
ACCENT = (0xC0, 0xEB, 0xE5)
DARK_GROUND = (0x0E, 0x15, 0x14)
DARK_MARK = (0x51, 0xDB, 0xCE)
DIAPER_OPACITY = 0.16

# The play triangle: M38 29 Q33 27 33 33 V75 Q33 81 38 79 L80 57.5 Q85 54 80 50.5 Z
PLAY_PATH = "M38,29 Q33,27 33,33 V75 Q33,81 38,79 L80,57.5 Q85,54 80,50.5 Z"
# Android only: launchers mask the adaptive icon to a circle (or similar) that shows the middle
# 72 units, so the artboard's mark, drawn for a rounded square, reaches the mask's edge. Scale it
# about the centre so it breathes like other launcher icons. The ground and diaper stay full size.
ANDROID_MARK_SCALE = 0.8
# Strands (x 28..88): light, accent, light, with 3-unit gaps.
STRANDS = [(26, 43, "light"), (46, 62, "accent"), (65, 82, "light")]


def quad(p0, p1, p2, steps=24):
    return [
        ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
         (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])
        for t in (i / steps for i in range(1, steps + 1))
    ]


def play_polygon():
    """The play path flattened to a polygon (for raster output)."""
    pts = [(38, 29)]
    pts += quad((38, 29), (33, 27), (33, 33))
    pts.append((33, 75))
    pts += quad((33, 75), (33, 81), (38, 79))
    pts.append((80, 57.5))
    pts += quad((80, 57.5), (85, 54), (80, 50.5))
    return pts


def diamond_centres():
    """Diaper diamonds at (9m, 9n) where m + n is even, covering the whole square."""
    return [(9 * m, 9 * n) for m in range(-1, 14) for n in range(-1, 14) if (m + n) % 2 == 0]


def diamond(cx, cy):
    return [(cx, cy - 7), (cx + 4, cy), (cx, cy + 7), (cx - 4, cy)]


# ---------------------------------------------------------------------------------------------
# Raster
# ---------------------------------------------------------------------------------------------

def render(size, ground, mark, accent, rounded=0.0, inset=0.0):
    """
    An RGBA icon [size] px square. [rounded] is the corner radius as a fraction of the artwork;
    [inset] leaves a transparent margin (fraction of size) around it, as macOS icons do.
    """
    s = size * SUPERSAMPLE
    art = int(round(s * (1 - 2 * inset)))
    off = (s - art) // 2
    scale = art / GRID

    def tx(points):
        return [(off + x * scale, off + y * scale) for x, y in points]

    layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rectangle([off, off, off + art, off + art], fill=ground + (255,))

    diaper = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    dd = ImageDraw.Draw(diaper)
    for cx, cy in diamond_centres():
        dd.polygon(tx(diamond(cx, cy)), fill=mark + (int(255 * DIAPER_OPACITY),))
    layer = Image.alpha_composite(layer, diaper)

    strands = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    sd = ImageDraw.Draw(strands)
    for top, bottom, kind in STRANDS:
        colour = accent if kind == "accent" else mark
        sd.rectangle(tx([(28, top), (88, bottom)]), fill=colour + (255,))
    clip = Image.new("L", (s, s), 0)
    ImageDraw.Draw(clip).polygon(tx(play_polygon()), fill=255)
    strands.putalpha(Image.composite(strands.getchannel("A"), Image.new("L", (s, s), 0), clip))
    layer = Image.alpha_composite(layer, strands)

    shape = Image.new("L", (s, s), 0)
    ImageDraw.Draw(shape).rounded_rectangle([off, off, off + art - 1, off + art - 1], radius=rounded * art, fill=255)
    layer.putalpha(Image.composite(layer.getchannel("A"), Image.new("L", (s, s), 0), shape))
    return layer.resize((size, size), Image.LANCZOS)


def save(img, path, opaque=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    (img.convert("RGB") if opaque else img).save(path)


# ---------------------------------------------------------------------------------------------
# Android vector drawables (108dp viewport: the adaptive-icon grid)
# ---------------------------------------------------------------------------------------------

def hex_rgb(rgb):
    return "#%02X%02X%02X" % rgb


def diamonds_path():
    return " ".join(
        "M{},{} L{},{} L{},{} L{},{} Z".format(*sum(([x, y] for x, y in diamond(cx, cy)), []))
        for cx, cy in diamond_centres()
        if -8 <= cx <= GRID + 8 and -8 <= cy <= GRID + 8
    )


def vector(body):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by branding/icon/generate.py from logo D2 \"Woven play on damask\". -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        f"{body}</vector>\n"
    )


def strands_group(light, accent):
    paths = "".join(
        f'        <path android:fillColor="{accent if kind == "accent" else light}"\n'
        f'            android:pathData="M28,{top} H88 V{bottom} H28 Z" />\n'
        for top, bottom, kind in STRANDS
    )
    return (
        f'    <group android:pivotX="54" android:pivotY="54"\n'
        f'        android:scaleX="{ANDROID_MARK_SCALE}" android:scaleY="{ANDROID_MARK_SCALE}">\n'
        f'        <clip-path android:pathData="{PLAY_PATH}" />\n{paths}    </group>\n'
    )


def android(res):
    drawable = os.path.join(res, "drawable")
    os.makedirs(drawable, exist_ok=True)
    diamonds = diamonds_path()
    with open(os.path.join(drawable, "ic_launcher_background.xml"), "w") as f:
        f.write(vector(
            f'    <path android:fillColor="{hex_rgb(TEAL)}" android:pathData="M0,0 H108 V108 H0 Z" />\n'
            f'    <path android:fillColor="#FFFFFF" android:fillAlpha="{DIAPER_OPACITY}"\n'
            f'        android:pathData="{diamonds}" />\n'
        ))
    with open(os.path.join(drawable, "ic_launcher_foreground.xml"), "w") as f:
        f.write(vector(strands_group(hex_rgb(WHITE), hex_rgb(ACCENT))))
    # Themed icons (Android 13+): the system tints this one colour; the diaper keeps its 16%.
    with open(os.path.join(drawable, "ic_launcher_monochrome.xml"), "w") as f:
        f.write(vector(
            f'    <path android:fillColor="#FFFFFF" android:fillAlpha="{DIAPER_OPACITY}"\n'
            f'        android:pathData="{diamonds}" />\n'
            + strands_group("#FFFFFF", "#FFFFFF")
        ))
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@drawable/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
        "</adaptive-icon>\n"
    )
    mipmap = os.path.join(res, "mipmap-anydpi-v26")
    os.makedirs(mipmap, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        with open(os.path.join(mipmap, name), "w") as f:
            f.write(adaptive)


# ---------------------------------------------------------------------------------------------
# Platforms
# ---------------------------------------------------------------------------------------------

def ios(appiconset):
    # iOS applies its own corner mask: full-bleed, opaque squares.
    save(render(1024, TEAL, WHITE, ACCENT), os.path.join(appiconset, "app-icon-1024.png"), opaque=True)
    save(render(1024, DARK_GROUND, DARK_MARK, ACCENT), os.path.join(appiconset, "app-icon-1024-dark.png"), opaque=True)
    # Tinted: greyscale; the system supplies the tint.
    tinted = render(1024, (0, 0, 0), WHITE, (0xD0, 0xD0, 0xD0)).convert("L").convert("RGB")
    save(tinted, os.path.join(appiconset, "app-icon-1024-tinted.png"), opaque=True)


def desktop(icons):
    os.makedirs(icons, exist_ok=True)
    # The design's rounded square (48/200 corner radius), full bleed: window icon, Windows, Linux.
    rounded = 48 / 200
    save(render(512, TEAL, WHITE, ACCENT, rounded=rounded), os.path.join(icons, "damask.png"))
    # The running app's window / taskbar icon, loaded from the classpath.
    save(render(256, TEAL, WHITE, ACCENT, rounded=rounded), "desktopApp/src/main/resources/damask-icon.png")
    render(256, TEAL, WHITE, ACCENT, rounded=rounded).save(
        os.path.join(icons, "damask.ico"), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )
    # macOS: Apple's grid puts an 824px rounded square (radius ~185) on a 1024px canvas.
    iconset = tempfile.mkdtemp(suffix=".iconset")
    for points in (16, 32, 128, 256, 512):
        for factor in (1, 2):
            px = points * factor
            name = f"icon_{points}x{points}{'@2x' if factor == 2 else ''}.png"
            render(px, TEAL, WHITE, ACCENT, rounded=185 / 824, inset=100 / 1024).save(os.path.join(iconset, name))
    subprocess.run(["iconutil", "-c", "icns", iconset, "-o", os.path.join(icons, "damask.icns")], check=True)
    shutil.rmtree(iconset)


def main():
    android("androidApp/src/main/res")
    ios("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset")
    desktop("desktopApp/icons")
    save(render(512, TEAL, WHITE, ACCENT), "branding/icon/out/store-512.png", opaque=True)
    print("Icons generated.")


if __name__ == "__main__":
    main()

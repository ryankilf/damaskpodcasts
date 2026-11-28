# Branding

## App icon

The icon is logo D2, **"Woven play on damask"** (`spec-and-design/damask-design/canvas/LogoStrandsDamask.dc.html`): a play triangle woven from three strands on the brand teal, over a tone-on-tone diaper pattern.

Every platform's icon is generated from one definition in [`branding/icon/generate.py`](../branding/icon/generate.py), so they can't drift apart. The artboard is drawn on a 108-unit square, which is exactly Android's adaptive-icon grid.

| Platform | Output | Notes |
|---|---|---|
| Android | `androidApp/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`, `mipmap-anydpi-v26/ic_launcher*.xml` | Adaptive icon as vector drawables. The monochrome layer is the design's "Themed (Material You)" variant, used by Android 13+ themed icons. The mark is scaled to 80% about the centre, because launchers mask to a circle showing only the middle 72 units and the artboard's mark (drawn for a rounded square) would touch its edge |
| iOS | `iosApp/.../AppIcon.appiconset/app-icon-1024{,-dark,-tinted}.png` | Full-bleed squares (iOS applies its own mask): light, the design's dark variant, and greyscale for tinting |
| Desktop | `desktopApp/icons/damask.{icns,ico,png}`, `desktopApp/src/main/resources/damask-icon.png` | Packaging icons and the window/taskbar icon. macOS follows Apple's grid (an 824px rounded square on a 1024px canvas) |
| Stores | `branding/icon/out/store-512.png` | For store listings |

To change the icon, edit `generate.py` and run it from the repo root (`python3 branding/icon/generate.py`, needs Pillow and, for `.icns`, macOS's `iconutil`), then commit the outputs.

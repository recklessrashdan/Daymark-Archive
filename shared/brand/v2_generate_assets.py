"""Daymark V2 brand asset generator.

Creates the new 'horizon mark' identity: midnight-navy rounded tile, teal
sunrise arc, amber day-marker and a porcelain horizon rule. Outputs a master
PNG, a Windows multi-size ICO and Android launcher/splash assets.

All drawing is procedural (Pillow); no third-party artwork is used.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
NAVY = (21, 28, 44)
NAVY_DEEP = (13, 19, 32)
TEAL = (25, 184, 165)
TEAL_LIGHT = (92, 224, 205)
AMBER = (242, 182, 93)
PORCELAIN = (244, 246, 248)


def make_master(size=1024):
    s = size
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Navy tile with a subtle vertical depth shift.
    radius = int(s * 0.225)
    d.rounded_rectangle((0, 0, s - 1, s - 1), radius=radius, fill=NAVY_DEEP)
    d.rounded_rectangle((int(s * .02), int(s * .02), s - int(s * .02), s - int(s * .02)),
                        radius=int(radius * .95), fill=NAVY)

    # Soft teal glow behind the mark.
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((int(s * .19), int(s * .22), int(s * .81), int(s * .84)), fill=(25, 184, 165, 55))
    glow = glow.filter(ImageFilter.GaussianBlur(s * .055))
    img.alpha_composite(glow)
    d = ImageDraw.Draw(img)

    # Sunrise arc: three nested teal strokes, clipped visually by the horizon.
    cx, cy = int(s * .5), int(s * .665)
    for r, width, color in ((int(s * .255), int(s * .052), TEAL),
                            (int(s * .165), int(s * .038), TEAL_LIGHT)):
        d.arc((cx - r, cy - r, cx + r, cy + r), start=182, end=358, fill=color, width=width)

    # Horizon rule.
    d.rounded_rectangle((int(s * .235), int(s * .655), int(s * .765), int(s * .675)),
                        radius=int(s * .01), fill=PORCELAIN)

    # Amber day marker above the sunrise.
    mr = int(s * .082)
    d.ellipse((cx - mr, int(s * .245) - mr, cx + mr, int(s * .245) + mr), fill=AMBER)
    d.ellipse((cx - mr // 2, int(s * .245) - mr // 2 - int(s * .012),
               cx + mr // 2 - int(s * .008), int(s * .245) + mr // 2 - int(s * .012)),
              fill=(255, 216, 150, 150))
    return img


def circle_mask(img):
    s = img.size[0]
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, s - 1, s - 1), fill=255)
    out = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def save_png(img, path, size):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.Resampling.LANCZOS).save(path)


def save_icon(img, path, sizes):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.resize((max(sizes), max(sizes)), Image.Resampling.LANCZOS).save(
        path, format="ICO", sizes=[(n, n) for n in sizes])


def main():
    master = make_master()
    # Write to an explicit output directory. The default is relative to the
    # current working directory so the script stays portable.
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("generated_assets") / "v2"
    out.mkdir(parents=True, exist_ok=True)
    save_png(master, out / "master" / "daymark-v2-master.png", 512)

    # Windows: app icon + dashboard logo asset.
    win = out / "Windows_Assets"
    save_png(master, win / "daymark.png", 256)
    save_icon(master, win / "daymark.ico", [16, 24, 32, 48, 64, 128, 256])

    # Android: square + circular launcher icons for all densities.
    res = out / "Android_res"
    for density, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                        ("xxhdpi", 144), ("xxxhdpi", 192)):
        folder = res / ("mipmap-" + density)
        save_png(master, folder / "ic_launcher.png", px)
        save_png(circle_mask(master), folder / "ic_launcher_round.png", px)

    # Splash artwork: new mark above the wordmark.
    splash = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    logo = master.resize((288, 288), Image.Resampling.LANCZOS)
    splash.alpha_composite(logo, (112, 100))
    ImageDraw.Draw(splash).text((256, 408), "DAYMARK", fill=NAVY, anchor="mm")
    save_png(splash, res / "drawable-nodpi" / "splash_logo.png", 512)
    print("V2 brand assets generated in", out)


if __name__ == "__main__":
    main()

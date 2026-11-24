"""Generate original Daymark brand assets (no third-party artwork).

Writes to a local temp directory by default; copy the outputs into the project
with a normal file copy (some OneDrive-synced folders reject Python's w+b mode).
"""
from pathlib import Path
import os
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent
OUT = Path(os.environ.get("DAYMARK_ASSET_OUT", str(Path(os.environ.get("TEMP", ".")) / "daymark-assets")))
OUT.mkdir(parents=True, exist_ok=True)

S = 1024
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Flat violet rounded tile with a soft lavender inner disc: calm, minimal, recognisable.
d.rounded_rectangle((40, 40, S - 40, S - 40), radius=225, fill=(108, 76, 241, 255))
d.ellipse((214, 176, 810, 772), fill=(255, 255, 255, 30))

# White check mark (completed task) with a thin horizontal day-rule beneath it.
d.line([(300, 520), (444, 660), (730, 366)], fill=(255, 255, 255, 255), width=72, joint="curve")
d.line([(300, 520), (444, 660), (730, 366)], fill=(255, 255, 255, 255), width=72)
d.rounded_rectangle((298, 742, 730, 782), radius=20, fill=(255, 255, 255, 210))

small = img.resize((512, 512), Image.Resampling.LANCZOS)
small.save(OUT / "daymark.png")
small.save(OUT / "daymark_512.png")
small.save(OUT / "daymark.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

# Android adaptive-icon foreground/background and a simple splash mark.
bg = Image.new("RGBA", (512, 512), (108, 76, 241, 255))
bg.save(OUT / "ic_launcher_background.png")
fg = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
fg.paste(img.resize((512, 512), Image.Resampling.LANCZOS), (0, 0), img.resize((512, 512), Image.Resampling.LANCZOS))
fg.save(OUT / "ic_launcher_foreground.png")
splash = Image.new("RGBA", (480, 480), (0, 0, 0, 0))
splash.paste(img.resize((256, 256), Image.Resampling.LANCZOS), (112, 112), img.resize((256, 256), Image.Resampling.LANCZOS))
splash.save(OUT / "splash.png")
print("wrote assets to", OUT)

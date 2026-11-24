"""Generate Android launcher/splash PNGs in a temp folder; copy with a shell step."""
from pathlib import Path
import os
from PIL import Image

TMP = Path(os.environ.get("DAYMARK_ASSET_OUT", str(Path(os.environ.get("TEMP", ".")) / "daymark-assets")))
TMP.mkdir(parents=True, exist_ok=True)
SRC = TMP / "daymark_512.png"

base = Image.open(SRC).convert("RGBA")
for size in (48, 72, 96, 144, 192):
    base.resize((size, size), Image.Resampling.LANCZOS).save(TMP / f"ic_{size}.png")
    print("generated", f"ic_{size}.png")

splash = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
logo = base.resize((300, 300), Image.Resampling.LANCZOS)
splash.paste(logo, (106, 106), logo)
splash.save(TMP / "splash_logo.png")
print("generated splash_logo.png")

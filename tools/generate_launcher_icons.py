from pathlib import Path
from shutil import copyfile

from PIL import Image, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ICON = ROOT / "icons" / "app" / "kiyori-app-icon-1024.png"
PLAYSTORE_ICON = ROOT / "app" / "src" / "main" / "ic_launcher-playstore.png"
RES_DIR = ROOT / "app" / "src" / "main" / "res"

SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Tuned per size so small launcher icons keep edge definition
# without looking oversharpened or haloed.
UNSHARP_BY_SIZE = {
    48: dict(radius=0.4, percent=160, threshold=2),
    72: dict(radius=0.5, percent=150, threshold=2),
    96: dict(radius=0.6, percent=140, threshold=2),
    144: dict(radius=0.7, percent=130, threshold=2),
    192: dict(radius=0.8, percent=120, threshold=2),
}


def export_icon_variant(source: Image.Image, size: int, output_path: Path) -> None:
    resized = source.resize((size, size), Image.Resampling.LANCZOS)
    sharpened = resized.filter(ImageFilter.UnsharpMask(**UNSHARP_BY_SIZE[size]))
    sharpened.save(output_path, format="PNG", optimize=False, compress_level=0)


def main() -> None:
    source = Image.open(SOURCE_ICON).convert("RGBA")
    copyfile(SOURCE_ICON, PLAYSTORE_ICON)

    for density, size in SIZES.items():
        mipmap_dir = RES_DIR / f"mipmap-{density}"
        export_icon_variant(source, size, mipmap_dir / "ic_launcher.png")
        export_icon_variant(source, size, mipmap_dir / "ic_launcher_round.png")

    print("Launcher icons regenerated from icons/app/kiyori-app-icon-1024.png with per-size sharpening.")


if __name__ == "__main__":
    main()

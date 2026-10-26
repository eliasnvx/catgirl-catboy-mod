"""Mod icon: a catgirl and a catboy side by side (equal billing), blocky Minecraft heads with cat ears and
bell collars, on a catnip-green background (Femboy Mod's icon is a pink cat face, so this one goes green).
Drawn as a 32x32 pixel grid, saved upscaled with nearest neighbour.
Run from the repo root: python3 tools/gen_icon.py"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art/icon"
ASSET = ROOT / "common/src/main/resources/assets/catgirl/icon.png"
SIZE = 32
SCALE = 16

OUTLINE = (40, 26, 36)
# shared letters; each head adds its own E/e (ear), H/h/d (hair), G (eyes)
COMMON = {
    "o": OUTLINE,
    "p": (244, 150, 172),   # inner ear
    "S": (248, 214, 186),   # skin
    "s": (226, 182, 152),   # skin shadow
    "W": (255, 255, 255),   # eye shine
    "K": (40, 26, 36),      # pupil
    "B": (244, 140, 150),   # blush
    "M": (150, 70, 70),     # mouth
    "F": (255, 255, 255),   # fang
    "C": (200, 50, 60),     # collar
    "c": (150, 30, 44),     # collar shadow
    "Y": (250, 206, 70),    # bell
    "y": (206, 150, 40),    # bell shadow
}

# 14 wide: cat ears on a blocky head, face, collar with a bell
CATGIRL = [
    "..o........o..",
    ".oEo......oEo.",
    ".oEpo....opEo.",
    "oEppEooooEppEo",
    "oHHhhHHHHHHHHo",
    "oHhhHHHHHHHHHo",
    "oHHHHdHHdHHHHo",
    "oHHSSSSSSSSHHo",
    "oHHSGWSSWGSHHo",
    "oHHSGGSSGGSHHo",
    "oHHBSMSSMSBHHo",
    "oHHSSSMMSSSHHo",
    "oHHsSSSSSSsHHo",
    "oHHoCCCCCCoHHo",
    "oHdocCYYCcodHo",
    ".ooo.oYyo.ooo.",
    "......oo......",
]

CATBOY = [
    "..o........o..",
    ".oEo......oEo.",
    ".oEpo....opEo.",
    "oEppEooooEppEo",
    "oHHHHHhhHHHHHo",
    "oHHHHhhHHHHHHo",
    "oHdHHHHdHHHdHo",
    "oHSSSSSSSSSSHo",
    "oSSGWSSSSWGSSo",
    "oSSGGSSSSGGSSo",
    "oSSSSMSSMSSSSo",
    "oSSSSSMFSSSSSo",
    "osSSSSSSSSSSso",
    ".ooCCCCCCCCoo.",
    "..ocCCYYCCco..",
    "...ooooYyoooo.",
    "......oo......",
]

GIRL_COLORS = {"E": (240, 150, 70), "H": (150, 84, 46), "h": (186, 118, 72), "d": (110, 58, 32), "G": (80, 170, 100)}
# his collar is indigo so the pair doesn't read as one blob of red
BOY_COLORS = {"E": (52, 48, 60), "H": (44, 40, 52), "h": (82, 76, 98), "d": (28, 24, 34), "G": (236, 190, 60),
              "C": (70, 80, 170), "c": (44, 50, 120)}

SPARKLE = (255, 255, 255)
HEART = (244, 120, 150)
HEART_SHINE = (255, 196, 210)
HEART_ROWS = [
    ".##.##.",
    "#+#####",
    "#######",
    ".#####.",
    "..###..",
    "...#...",
]


def background():
    """Diagonal catnip gradient, lighter at the top-left."""
    top, bottom = (150, 226, 190), (40, 150, 140)
    img = Image.new("RGBA", (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            t = (x + y) / (2 * (SIZE - 1))
            img.putpixel((x, y), tuple(int(a + (b - a) * t) for a, b in zip(top, bottom)) + (255,))
    return img


def stamp(img, rows, colors, ox, oy):
    palette = {**COMMON, **colors}
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((ox + x, oy + y), palette[c] + (255,))


def main():
    img = background()
    stamp(img, CATGIRL, GIRL_COLORS, 2, 12)
    stamp(img, CATBOY, BOY_COLORS, 16, 11)
    # a heart between the ears
    for y, row in enumerate(HEART_ROWS):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((12 + x, 5 + y), (HEART if c == "#" else HEART_SHINE) + (255,))
    # sparkles
    for cx, cy in ((4, 6), (27, 4)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            img.putpixel((cx + dx, cy + dy), SPARKLE + (255,))
    ART.mkdir(parents=True, exist_ok=True)
    img.save(ART / "icon_32.png")
    big = img.resize((SIZE * SCALE, SIZE * SCALE), Image.NEAREST)
    big.save(ART / "icon_512.png")
    ASSET.parent.mkdir(parents=True, exist_ok=True)
    big.save(ASSET)
    print(f"icon -> {ASSET.relative_to(ROOT)}, art/icon/")


if __name__ == "__main__":
    main()

"""Placeholder assets for the code-built ears/tail: grayscale icon layers (tinted per coat part in game),
the fur texture, and the wool -> coat crafting recipes. Run from the repo root: python3 tools/gen_placeholders.py
Replace the PNGs with hand-drawn art later; keep the layer split (main / second / third tint)."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/catgirl"
DATA = ROOT / "common/src/main/resources/data/catgirl"

FILL = 238
SHADE = 205
OUTLINE = 120


def blank(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def gray(img, x, y, v):
    img.putpixel((x, y), (v, v, v, 255))


def cat_ears():
    """layer0 fur (main), layer1 inner ear (inner), layer2 ear tips (points)."""
    fur, inner, tips = blank(), blank(), blank()
    for cx in (4.0, 12.0):
        for y in range(3, 13):
            half = (y - 3) * 0.36 + 0.6
            for x in range(16):
                d = abs(x + 0.5 - cx)
                if d > half:
                    continue
                edge = d > half - 1.0 or y == 12
                if y <= 4:
                    gray(tips, x, y, SHADE if edge else FILL)
                elif not edge and y >= 6 and d <= half - 1.6:
                    gray(inner, x, y, FILL if d < half - 2.4 else SHADE)
                else:
                    gray(fur, x, y, OUTLINE if edge else (SHADE if x + 0.5 > cx else FILL))
    return fur, inner, tips


def cat_tail():
    """layer0 body (main), layer1 rings (patch), layer2 tip (points). A J-curve from bottom-left up to the right."""
    body, rings, tip = blank(), blank(), blank()
    pts = []
    steps = 60
    for i in range(steps + 1):
        t = i / steps
        # quadratic bezier: (2,14) -> (13,14) -> (12,2)
        x = (1 - t) ** 2 * 2 + 2 * (1 - t) * t * 13 + t * t * 12
        y = (1 - t) ** 2 * 14 + 2 * (1 - t) * t * 14 + t * t * 2.5
        pts.append((x, y, t))
    for py in range(16):
        for px in range(16):
            best = None
            for x, y, t in pts:
                d = ((px + 0.5 - x) ** 2 + (py + 0.5 - y) ** 2) ** 0.5
                if best is None or d < best[0]:
                    best = (d, t)
            d, t = best
            radius = 1.9 - 0.5 * t
            if d > radius:
                continue
            v = OUTLINE if d > radius - 0.8 else FILL
            if t > 0.82:
                gray(tip, px, py, v)
            elif int(t * 10) % 3 == 2:
                gray(rings, px, py, v)
            else:
                gray(body, px, py, v)
    return body, rings, tip


def grid_icon(rows, legend, layers):
    """rows: 16 strings of 16 chars; legend: char -> (layer index, gray value or RGB tuple)."""
    imgs = [blank() for _ in range(layers)]
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c in legend:
                layer, v = legend[c]
                rgb = (v, v, v) if isinstance(v, int) else v
                imgs[layer].putpixel((x, y), (*rgb, 255))
    return imgs


GOLD = (242, 201, 76)
GOLD_DARK = (176, 132, 40)
DARKEST = (42, 35, 40)

# layers: 0 main, 1 inner (pads), 2 tuft (cuff)
PAW_GLOVES = [
    "................",
    "................",
    "....cccccccc....",
    "...cCCCCCCCCc...",
    "...cccccccccc...",
    "...oMMMMMMMMo...",
    "...oMMMMMMMMo...",
    "...oMMMppMMMo...",
    "...oMMpMMpMMo...",
    "...oMMMMMMMMo...",
    "...oMMPPPPMMo...",
    "...oMMPPPPMMo...",
    "...oMMMMMMMMo...",
    "....oMMoMMoM....",
    ".....oo.oo.o....",
    "................",
]
PAW_MITTENS = [
    "................",
    "..cccccccccccc..",
    ".cCCCCCCCCCCCCc.",
    ".cccccccccccccc.",
    "..oMMMMMMMMMMo..",
    ".oMMMMMMMMMMMMo.",
    ".oMMMMpMMpMMMMo.",
    ".oMMMpMMMMpMMMo.",
    ".oMMMMMMMMMMMMo.",
    ".oMMMMPPPPMMMMo.",
    ".oMMMPPPPPPMMMo.",
    ".oMMMMPPPPMMMMo.",
    ".oMMMMMMMMMMMMo.",
    "..oMMMoMMoMMMo..",
    "...ooo.oo.ooo...",
    "................",
]
PAW_LEGEND = {"M": (0, 238), "o": (0, 150), "P": (1, 238), "p": (1, 238), "C": (2, 255), "c": (2, 205)}

# layers: 0 main, 1 patch (stripes), 2 inner (paw print), 3 tuft (cuffs)
LEG_WARMERS = [
    "................",
    ".....cccccc.....",
    "....cCCCCCCc....",
    "....cccccccc....",
    "....oMMMMMMo....",
    "....oSSSSSSo....",
    "....oMMMMMMo....",
    "....oMpMMpMo....",
    "....oMMPPMMo....",
    "....oMPPPPMo....",
    "....oMMMMMMo....",
    "....oSSSSSSo....",
    "....oMMMMMMo....",
    "....cCCCCCCc....",
    "....cccccccc....",
    "................",
]
LEG_LEGEND = {"M": (0, 238), "o": (0, 150), "S": (1, 238), "P": (2, 238), "p": (2, 238), "C": (3, 255), "c": (3, 205)}

# layers: 0 main, 1 inner (ears inside, beans, sole), 2 tuft (rim)
PAW_SLIPPERS = [
    "................",
    "................",
    "................",
    "................",
    "...o.o..........",
    "..oMooMo........",
    "..oIMMIo........",
    ".cccccccccc.....",
    ".cCCCCCCCCc.....",
    ".oMMMMMMMMMMo...",
    "oMMMMMMMMMMMMo..",
    "oMpMpMpMMMMMMMo.",
    "oMMMMMMMMMMMMMo.",
    "oMMMMMMMMMMMMMo.",
    ".SSSSSSSSSSSSS..",
    "................",
]
SLIPPER_LEGEND = {"M": (0, 238), "o": (0, 150), "I": (1, 238), "p": (1, 238), "S": (1, 200), "C": (2, 255), "c": (2, 205)}

# collars: layer 0 dyed band (vanilla dye tint), layer 1 untinted metal
BELL_COLLAR = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "..ooBBBBBBBBoo..",
    ".oBBbbbbbbbbBBo.",
    ".oBb........bBo.",
    ".oBb........bBo.",
    ".oBBb......bBBo.",
    "..oBBBBGGBBBBo..",
    "...ooooGGoooo...",
    "......gGGg......",
    ".....gGGGGg.....",
    ".....gGddGg.....",
    "......gGGg......",
    "................",
]
VELVET_CHOKER = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "..ooBBBBBBBBoo..",
    ".oBBbbbbbbbbBBo.",
    ".oBb........bBo.",
    ".oBb........bBo.",
    ".oBBb......bBBo.",
    "..oBBBBBBBBBBo..",
    "...oooogGoooo...",
    "......GGGG......",
    "......GGGG......",
    ".......GG.......",
    "................",
    "................",
]
NECK_RIBBON = [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "..ooBBBBBBBBoo..",
    ".oBBbbbbbbbbBBo.",
    ".oBb........bBo.",
    ".oBb........bBo.",
    ".oBBbBBo.BBbBBo.",
    "..oBBBBBkBBBBo..",
    "..oBBBBkkkBBBBo.",
    "...oBBBBkBBBBo..",
    "......oBoBo.....",
    ".....oBo.oBo....",
    ".....oo...oo....",
    "................",
]
# face and head: layer 0 dyed, layer 1 untinted
WHISKERS = [
    "................",
    "................",
    "................",
    "................",
    "..B..........B..",
    "...BB......BB...",
    "....BBB..BBB....",
    "..BBBBB..BBBBB..",
    "....BBB..BBB....",
    "...BB......BB...",
    "..B..........B..",
    "................",
    "......dddd......",
    ".......dd.......",
    "................",
    "................",
]
BLUSH = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..bBBBb..bBBBb..",
    ".bBBBBBbbBBBBBb.",
    ".BBkBkBBBBkBkBB.",
    ".BkBkBBBBkBkBBB.",
    ".bBBBBBbbBBBBBb.",
    "..bBBBb..bBBBb..",
    "................",
    "................",
    "................",
    "................",
    "................",
]
HEADPHONES = [
    "................",
    "...o........o...",
    "..oBo......oBo..",
    "..oWBoooooBWBo..",
    "..oBBBBBBBBBBo..",
    "..oBo......oBo..",
    ".oBo........oBo.",
    ".oBo........oBo.",
    "oBBBo......oBBBo",
    "oBkBo......oBkBo",
    "oBWBo......oBWBo",
    "oBkBo......oBkBo",
    "oBBBo......oBBBo",
    ".ooo........ooo.",
    "................",
    "................",
]
EAR_BOWS = [
    "................",
    "................",
    "................",
    "................",
    ".oo.oo....oo.oo.",
    "oBBoBBo..oBBoBBo",
    "oBWkBBo..oBWkBBo",
    "oBBoBBo..oBBoBBo",
    ".oo.oo....oo.oo.",
    "...o.o......o.o.",
    "...o.o......o.o.",
    "................",
    "................",
    "................",
    "................",
    "................",
]
BELL_HEADBAND = [
    "................",
    "................",
    ".....oooooo.....",
    "...ooBBBBBBoo...",
    "..oBBbbbbbbBBo..",
    ".oBbo......obBo.",
    ".oBo........gGg.",
    "oBbo.......gGGGg",
    "oBo........gGddg",
    "oBo.........gGg.",
    "oBo.........oBo.",
    "ooo.........ooo.",
    "................",
    "................",
    "................",
    "................",
]

COLLAR_LEGEND = {"B": (0, 238), "b": (0, 180), "o": (0, 110), "k": (0, 150),
                 "G": (1, GOLD), "g": (1, GOLD_DARK), "d": (1, DARKEST), "W": (1, (255, 255, 255))}


def dyed_item_files(name, layers, default_rgb):
    """Like item_files, but layer 0 takes vanilla dyeing (minecraft:dye tint) and the rest keep their own colors."""
    tex_dir = ASSETS / "textures/item"
    textures = {}
    for i, img in enumerate(layers):
        suffix = "" if i == 0 else f"_{i}"
        img.save(tex_dir / f"{name}{suffix}.png")
        textures[f"layer{i}"] = f"catgirl:item/{name}{suffix}"
    write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": textures})
    write(ASSETS / f"items/{name}.json", {"model": {
        "type": "minecraft:model",
        "model": f"catgirl:item/{name}",
        # the default is ARGB as a signed int (like vanilla's leather armor); plain RGB has alpha 0 = invisible
        "tints": [{"type": "minecraft:dye", "default": (0xFF000000 | default_rgb) - (1 << 32)}],
    }})


def fur_texture():
    random.seed(7)
    img = Image.new("RGBA", (64, 64))
    for y in range(64):
        streak = random.randint(-6, 6)
        for x in range(64):
            v = max(200, min(255, 240 + streak + random.randint(-10, 10)))
            img.putpixel((x, y), (v, v, v, 255))
    return img


def item_files(name, layers, tints):
    tex_dir = ASSETS / "textures/item"
    tex_dir.mkdir(parents=True, exist_ok=True)
    textures = {}
    for i, img in enumerate(layers):
        suffix = "" if i == 0 else f"_{i}"
        img.save(tex_dir / f"{name}{suffix}.png")
        textures[f"layer{i}"] = f"catgirl:item/{name}{suffix}"
    write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": textures})
    write(ASSETS / f"items/{name}.json", {"model": {
        "type": "minecraft:model",
        "model": f"catgirl:item/{name}",
        "tints": [{"type": "catgirl:coat", "part": part} for part in tints],
    }})


# Craftable coats; Siamese and Calico come from trades and the fisher's quest
WOOL_COATS = {
    "black": "black", "white": "white", "orange": "ginger", "gray": "gray", "pink": "pink", "lime": "neon",
}

RECIPES = {
    "cat_ears": {"pattern": ["W W", "SLS"]},
    "cat_tail": {"pattern": ["  W", " W ", "SL "]},
    "paw_gloves": {"pattern": ["WLW", "W W"]},
    "paw_mittens": {"pattern": ["WWW", "WLW"]},
    "leg_warmers": {"pattern": ["WSW", "W W"]},
    "paw_slippers": {"pattern": ["W W", "L L"]},
}

# Collars: one shaped recipe each, recolored with any dye (vanilla crafting_dye)
COLLAR_RECIPES = {
    "bell_collar": {"pattern": ["SLS", " N "], "key": {"S": "minecraft:string", "L": "minecraft:leather", "N": "minecraft:gold_nugget"}},
    "velvet_choker": {"pattern": ["SWS", " N "], "key": {"S": "minecraft:string", "W": "minecraft:black_wool", "N": "minecraft:gold_nugget"}},
    "neck_ribbon": {"pattern": ["S S", " P "], "key": {"S": "minecraft:string", "P": "minecraft:pink_wool"}},
    "bell_headband": {"pattern": ["SSS", "S N"], "key": {"S": "minecraft:string", "N": "minecraft:gold_nugget"}},
    "cat_ear_headphones": {"pattern": ["IWI", "N N"], "key": {"I": "minecraft:iron_ingot", "W": "minecraft:pink_wool", "N": "minecraft:note_block"}},
    "ear_bows": {"pattern": ["R R", "S S"], "key": {"R": "minecraft:red_wool", "S": "minecraft:string"}},
    "whiskers": {"pattern": ["S S", "SIS"], "key": {"S": "minecraft:string", "I": "minecraft:ink_sac"}},
    "blush": {"pattern": ["P P"], "key": {"P": "minecraft:pink_dye"}},
}


def recipes():
    for item, recipe in RECIPES.items():
        for wool, coat in WOOL_COATS.items():
            write(DATA / f"recipe/{item}_{coat}.json", {
                "type": "minecraft:crafting_shaped",
                "category": "equipment",
                "group": f"catgirl_{item}",
                "pattern": recipe["pattern"],
                # only the symbols the pattern uses: the game rejects unused keys
                "key": {k: v for k, v in {"W": f"minecraft:{wool}_wool", "S": "minecraft:string",
                                          "L": "minecraft:leather"}.items() if k in "".join(recipe["pattern"])},
                "result": {"id": f"catgirl:{item}", "components": {"catgirl:coat": coat}},
            })
    for item, recipe in COLLAR_RECIPES.items():
        write(DATA / f"recipe/{item}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "equipment",
            "pattern": recipe["pattern"],
            "key": recipe["key"],
            "result": {"id": f"catgirl:{item}"},
        })
        write(DATA / f"recipe/{item}_dyed.json", {
            "type": "minecraft:crafting_dye",
            "dye": "#minecraft:dyes",
            "group": "catgirl_dyed",
            "result": {"id": f"catgirl:{item}"},
            "target": f"catgirl:{item}",
        })


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


if __name__ == "__main__":
    item_files("cat_ears", cat_ears(), ["main", "inner", "points"])
    item_files("cat_tail", cat_tail(), ["main", "patch", "points"])
    item_files("paw_gloves", grid_icon(PAW_GLOVES, PAW_LEGEND, 3), ["main", "inner", "tuft"])
    item_files("paw_mittens", grid_icon(PAW_MITTENS, PAW_LEGEND, 3), ["main", "inner", "tuft"])
    item_files("leg_warmers", grid_icon(LEG_WARMERS, LEG_LEGEND, 4), ["main", "patch", "inner", "tuft"])
    item_files("paw_slippers", grid_icon(PAW_SLIPPERS, SLIPPER_LEGEND, 3), ["main", "inner", "tuft"])
    # same defaults as CatgirlItems.*_COLOR
    dyed_item_files("bell_collar", grid_icon(BELL_COLLAR, COLLAR_LEGEND, 2), 0xC8323C)
    dyed_item_files("velvet_choker", grid_icon(VELVET_CHOKER, COLLAR_LEGEND, 2), 0x2A2A33)
    dyed_item_files("neck_ribbon", grid_icon(NECK_RIBBON, COLLAR_LEGEND, 2), 0xF291BE)
    dyed_item_files("whiskers", grid_icon(WHISKERS, COLLAR_LEGEND, 2), 0x2A2328)
    dyed_item_files("blush", grid_icon(BLUSH, COLLAR_LEGEND, 2), 0xF06292)
    dyed_item_files("cat_ear_headphones", grid_icon(HEADPHONES, COLLAR_LEGEND, 2), 0xF7A8CC)
    dyed_item_files("ear_bows", grid_icon(EAR_BOWS, COLLAR_LEGEND, 2), 0xE0405A)
    dyed_item_files("bell_headband", grid_icon(BELL_HEADBAND, COLLAR_LEGEND, 2), 0x2A2A33)
    entity = ASSETS / "textures/entity/cosmetic"
    entity.mkdir(parents=True, exist_ok=True)
    fur_texture().save(entity / "fur.png")
    recipes()
    print("ok")

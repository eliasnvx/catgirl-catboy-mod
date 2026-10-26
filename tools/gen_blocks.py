"""Placeholder assets for the mechanics: cat bed, cardboard box, catnip, yarn ball, effect icons,
plus their block models, blockstates, loot tables, recipes and worldgen. Run from the repo root:
python3 tools/gen_blocks.py. Hand-drawn textures can replace the PNGs later; keep the file names."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/catgirl"
DATA = ROOT / "common/src/main/resources/data/catgirl"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def jitter(rgb, rnd, amount):
    return tuple(max(0, min(255, c + rnd.randint(-amount, amount))) for c in rgb)


# ------------------------------------------------------------------ textures

def wicker():
    rnd = random.Random(1)
    img = Image.new("RGBA", (16, 16))
    light, dark = (196, 148, 92), (140, 96, 52)
    for y in range(16):
        for x in range(16):
            # woven: alternating horizontal and vertical strands in 4x4 cells
            cell = ((x // 4) + (y // 4)) % 2
            edge = (y % 4 == 3) if cell else (x % 4 == 3)
            base = dark if edge else light
            img.putpixel((x, y), (*jitter(base, rnd, 8), 255))
    return img


def cushion():
    rnd = random.Random(2)
    img = Image.new("RGBA", (16, 16))
    base = (226, 120, 150)
    for y in range(16):
        for x in range(16):
            c = base
            if x in (0, 15) or y in (0, 15):
                c = (196, 92, 124)  # seam
            elif (x + y) % 7 == 0:
                c = (238, 150, 176)  # fluff
            img.putpixel((x, y), (*jitter(c, rnd, 6), 255))
    return img


def cardboard(inside=False):
    rnd = random.Random(3 if inside else 4)
    img = Image.new("RGBA", (16, 16))
    base = (176, 132, 82) if inside else (204, 160, 106)
    for y in range(16):
        for x in range(16):
            c = base
            if not inside and x % 3 == 0:
                c = tuple(v - 10 for v in base)  # corrugation
            if not inside and 6 <= y <= 8:
                c = (222, 196, 140)  # packing tape
            if x in (0, 15) or y in (0, 15):
                c = tuple(v - 28 for v in base)
            img.putpixel((x, y), (*jitter(c, rnd, 5), 255))
    if not inside:
        for x, y in [(11, 11), (12, 11), (11, 12), (12, 12), (10, 13), (13, 13)]:  # a tiny paw print stamp
            img.putpixel((x, y), (120, 84, 50, 255))
    return img


LEAF = (98, 168, 72)
LEAF_DARK = (62, 122, 48)
STEM = (74, 126, 52)
FLOWER = (196, 170, 232)


def catnip_stage(stage):
    """A cross-model plant: grows from a sprout (0) to a bushy plant with lavender flowers (3)."""
    rnd = random.Random(10 + stage)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    height = [5, 9, 12, 14][stage]
    stems = [7, 8] if stage < 2 else [4, 7, 8, 11]
    for sx in stems:
        top = 16 - height + (0 if sx in (7, 8) else 3)
        for y in range(top, 16):
            img.putpixel((sx, y), (*STEM, 255))
        # leaf pairs along the stem
        for y in range(top + 1, 15, 3):
            for dx in (-2, -1, 1, 2):
                x = sx + dx
                if 0 <= x < 16:
                    c = LEAF if abs(dx) == 1 else LEAF_DARK
                    img.putpixel((x, y + (abs(dx) == 2)), (*jitter(c, rnd, 10), 255))
        if stage == 3:
            for dy in range(3):
                for dx in (-1, 0, 1):
                    if rnd.random() < 0.75:
                        img.putpixel((sx + dx, top - 1 + dy), (*jitter(FLOWER, rnd, 12), 255))
    return img


def catnip_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(10):
        img.putpixel((3 + i, 13 - i), (*STEM, 255))
    rnd = random.Random(20)
    for (x, y) in [(5, 9), (6, 10), (4, 10), (8, 6), (9, 7), (7, 7), (10, 4), (11, 5), (9, 4), (12, 3)]:
        img.putpixel((x, y), (*jitter(LEAF, rnd, 12), 255))
        img.putpixel((x + 1, y - 1), (*jitter(LEAF_DARK, rnd, 8), 255))
    for (x, y) in [(12, 1), (13, 2), (13, 1), (12, 2)]:
        img.putpixel((x, y), (*FLOWER, 255))
    return img


def yarn_ball():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base, dark, light = (236, 112, 150), (176, 66, 104), (250, 170, 196)
    cx, cy, r = 7.5, 8.0, 5.6
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                c = base
                if (x + 2 * y) % 5 == 0 or (2 * x - y) % 6 == 0:
                    c = dark  # wound strands
                if d > r - 1:
                    c = dark
                if x < cx - 1 and y < cy - 1 and d < r - 2 and (x + y) % 4 == 0:
                    c = light
                img.putpixel((x, y), (*c, 255))
    for (x, y) in [(12, 12), (13, 13), (14, 13), (15, 14)]:  # loose end
        img.putpixel((x, y), (*dark, 255))
    return img


def effect_icon(kind):
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    if kind == "well_rested":
        # a crescent moon and a "z"
        for y in range(18):
            for x in range(18):
                if ((x - 8) ** 2 + (y - 9) ** 2) <= 36 and ((x - 11) ** 2 + (y - 7) ** 2) > 25:
                    img.putpixel((x, y), (245, 210, 130, 255))
        for (x, y) in [(12, 2), (13, 2), (14, 2), (14, 3), (13, 4), (12, 5), (13, 5), (14, 5)]:
            img.putpixel((x, y), (255, 255, 255, 255))
    else:
        leaf = catnip_item().resize((18, 18), Image.NEAREST)
        img.paste(leaf, (0, 0), leaf)
    return img


# ------------------------------------------------------------------ models

def element(frm, to, tex, faces="all", inside=None):
    names = ["north", "south", "east", "west", "up", "down"] if faces == "all" else faces
    return {"from": frm, "to": to, "faces": {f: {"texture": inside if inside and f in ("south",) else tex} for f in names}}


def cat_bed_model():
    w = "#wicker"
    return {
        "parent": "minecraft:block/block",
        "textures": {"wicker": "catgirl:block/wicker", "cushion": "catgirl:block/cushion", "particle": "catgirl:block/wicker"},
        "elements": [
            element([1, 0, 1], [15, 1, 15], w),
            element([1, 1, 1], [15, 5, 2], w),
            element([1, 1, 14], [15, 5, 15], w),
            element([1, 1, 2], [2, 5, 14], w),
            element([14, 1, 2], [15, 5, 14], w),
            element([1, 1, 1], [15, 6, 2], w, ["up"]),
            element([2, 1, 2], [14, 3, 14], "#cushion"),
            element([4, 3, 4], [12, 3.5, 12], "#cushion", ["up"]),
        ],
    }


def box_model():
    out, ins = "#side", "#inside"
    h = 11
    elements = [
        element([1, 0, 1], [15, 1, 15], ins),
        # walls: outside face cardboard, inside face darker
        {"from": [1, 0, 1], "to": [15, h, 2], "faces": {"north": {"texture": out}, "south": {"texture": ins}, "up": {"texture": ins},
                                                        "east": {"texture": out}, "west": {"texture": out}}},
        {"from": [1, 0, 14], "to": [15, h, 15], "faces": {"south": {"texture": out}, "north": {"texture": ins}, "up": {"texture": ins},
                                                          "east": {"texture": out}, "west": {"texture": out}}},
        {"from": [1, 0, 2], "to": [2, h, 14], "faces": {"west": {"texture": out}, "east": {"texture": ins}, "up": {"texture": ins}}},
        {"from": [14, 0, 2], "to": [15, h, 14], "faces": {"east": {"texture": out}, "west": {"texture": ins}, "up": {"texture": ins}}},
    ]
    # flaps folded open, leaning outward
    flaps = [
        ([1, h, 1], [15, h + 5, 1.5], {"origin": [8, h, 1], "axis": "x", "angle": 45}),
        ([1, h, 14.5], [15, h + 5, 15], {"origin": [8, h, 15], "axis": "x", "angle": -45}),
        ([1, h, 1], [1.5, h + 5, 15], {"origin": [1, h, 8], "axis": "z", "angle": -45}),
        ([14.5, h, 1], [15, h + 5, 15], {"origin": [15, h, 8], "axis": "z", "angle": 45}),
    ]
    for frm, to, rot in flaps:
        elements.append({"from": frm, "to": to, "rotation": rot,
                         "faces": {f: {"texture": out} for f in ["north", "south", "east", "west", "up", "down"]}})
    return {"parent": "minecraft:block/block",
            "textures": {"side": "catgirl:block/cardboard", "inside": "catgirl:block/cardboard_inside", "particle": "catgirl:block/cardboard"},
            "elements": elements}


# ------------------------------------------------------------------ data

def loot_self(block):
    return {"type": "minecraft:block", "random_sequence": f"catgirl:blocks/{block}", "pools": [{
        "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": f"catgirl:{block}"}]}]}


def loot_catnip():
    # 26.3: "match_block" with "state" (was block_state_property / properties)
    grown = {"type": "minecraft:match_block", "blocks": "catgirl:catnip", "state": {"age": "3"}}
    return {"type": "minecraft:block", "random_sequence": "catgirl:blocks/catnip", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "catgirl:catnip"}]},
        {"rolls": 1, "condition": grown, "entries": [{"type": "minecraft:item", "name": "catgirl:catnip",
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]},
    ]}


def main():
    tex = ASSETS / "textures"
    save(wicker(), tex / "block/wicker.png")
    save(cushion(), tex / "block/cushion.png")
    save(cardboard(), tex / "block/cardboard.png")
    save(cardboard(inside=True), tex / "block/cardboard_inside.png")
    for stage in range(4):
        save(catnip_stage(stage), tex / f"block/catnip_stage{stage}.png")
    save(catnip_item(), tex / "item/catnip.png")
    save(yarn_ball(), tex / "item/yarn_ball.png")
    save(effect_icon("well_rested"), tex / "mob_effect/well_rested.png")
    save(effect_icon("catnip"), tex / "mob_effect/catnip.png")

    write(ASSETS / "models/block/cat_bed.json", cat_bed_model())
    write(ASSETS / "models/block/cardboard_box.json", box_model())
    for stage in range(4):
        write(ASSETS / f"models/block/catnip_stage{stage}.json",
              {"parent": "minecraft:block/cross", "textures": {"cross": f"catgirl:block/catnip_stage{stage}"}})
    write(ASSETS / "blockstates/cat_bed.json", {"variants": {"": {"model": "catgirl:block/cat_bed"}}})
    write(ASSETS / "blockstates/cardboard_box.json", {"variants": {"": {"model": "catgirl:block/cardboard_box"}}})
    write(ASSETS / "blockstates/catnip.json",
          {"variants": {f"age={a}": {"model": f"catgirl:block/catnip_stage{a}"} for a in range(4)}})
    for name in ("cat_bed", "cardboard_box"):
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"catgirl:block/{name}"}})
    for name in ("catnip", "yarn_ball"):
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"catgirl:item/{name}"}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"catgirl:item/{name}"}})

    write(DATA / "loot_table/blocks/cat_bed.json", loot_self("cat_bed"))
    write(DATA / "loot_table/blocks/cardboard_box.json", loot_self("cardboard_box"))
    write(DATA / "loot_table/blocks/catnip.json", loot_catnip())

    write(DATA / "recipe/cat_bed.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["SWS", "SSS"], "key": {"S": "minecraft:stick", "W": "#minecraft:wool"}, "result": {"id": "catgirl:cat_bed"}})
    write(DATA / "recipe/cardboard_box.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["P P", "PPP"], "key": {"P": "minecraft:paper"}, "result": {"id": "catgirl:cardboard_box"}})
    write(DATA / "recipe/yarn_ball.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" S ", "SWS", " S "], "key": {"S": "minecraft:string", "W": "#minecraft:wool"},
        "result": {"id": "catgirl:yarn_ball", "count": 2}})

    # wild catnip: rare patches on grass in mild green biomes
    write(DATA / "worldgen/feature/catnip.json",
          {"type": "minecraft:simple_block", "to_place": {"id": "catgirl:catnip", "properties": {"age": "3"}}})
    trapezoid = lambda r: {"type": "minecraft:trapezoid", "max": r, "min": -r, "plateau": 0}
    write(DATA / "worldgen/placed_feature/patch_catnip.json", {"feature": "catgirl:catnip", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 24},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": 24},
        {"type": "minecraft:offset", "x": trapezoid(5), "y": trapezoid(2), "z": trapezoid(5)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:grass_block", "offset": [0, -1, 0]}]}},
    ]})
    write(DATA / "tags/worldgen/biome/has_catnip.json", {"values": [
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:flower_forest",
        "minecraft:forest", "minecraft:birch_forest", "minecraft:cherry_grove"]})
    print("ok")


if __name__ == "__main__":
    main()

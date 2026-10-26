"""Food: placeholder 16x16 icons drawn from character grids, item models, recipes, tags and lang.
Run from the repo root: python3 tools/gen_food.py. Replace the PNGs with hand-drawn art later (same names)."""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/catgirl"
DATA = ROOT / "common/src/main/resources/data"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


def icon(rows, palette):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x, y), (*palette[c], 255))
    return img


OUTLINE = (52, 40, 44)

ICONS = {
    "onigiri": ([
        "................",
        ".......oo.......",
        "......oWWo......",
        ".....oWWWWo.....",
        ".....oWWWWo.....",
        "....oWWppWWo....",
        "....oWWppWWo....",
        "...oWWWWWWWWo...",
        "...oWWWWWWWWo...",
        "..oWWWWWWWWWWo..",
        "..oWWnnnnnnWWo..",
        ".oWWWnnnnnnWWWo.",
        ".oWWWnnnnnnWWWo.",
        ".ooooooooooooooo",
        "................",
        "................",
    ], {"o": OUTLINE, "W": (246, 244, 236), "p": (240, 130, 120), "n": (34, 56, 40)}),
    "raw_fish_sticks": ([
        "................",
        "................",
        "...........oo...",
        "..........oSSo..",
        ".........oSSo...",
        "......oo.oSo....",
        ".....oSSooo.oo..",
        "....oSSo...oSSo.",
        "...oSSo...oSSo..",
        "..oSSo...oSSo...",
        ".oSSo...oSSo....",
        ".oSo...oSSo.....",
        "..o...oSSo......",
        "......oSo.......",
        ".......o........",
        "................",
    ], {"o": (150, 120, 100), "S": (240, 214, 186)}),
    "fish_sticks": ([
        "................",
        "................",
        "...........oo...",
        "..........oGGo..",
        ".........oGgo...",
        "......oo.oGo....",
        ".....oGGooo.oo..",
        "....oGgo...oGGo.",
        "...oGgo...oGgo..",
        "..oGgo...oGgo...",
        ".oGgo...oGgo....",
        ".oGo...oGgo.....",
        "..o...oGgo......",
        "......oGo.......",
        ".......o........",
        "................",
    ], {"o": (120, 70, 30), "G": (232, 168, 70), "g": (204, 130, 46)}),
    "milk_saucer": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..oooooooooooo..",
        ".oWWWWWWWWWWWWo.",
        ".owWWWWWWWWWWwo.",
        "..oBBBBBBBBBBo..",
        "..obBBBBBBBBbo..",
        "...obBBBBBBbo...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": OUTLINE, "W": (250, 250, 248), "w": (222, 226, 232), "B": (150, 104, 66), "b": (118, 80, 48)}),
    "cat_food": ([
        "................",
        "................",
        "................",
        "................",
        ".....k.kk.k.....",
        "...kkKkKKkKkk...",
        "..oKkKKkKKkKKo..",
        ".oBBBBBBBBBBBBo.",
        ".oBBBWBBBBWBBBo.",
        "..oBBWWWWWWBBo..",
        "..oBBBWWWWBBBo..",
        "...oBBBWWBBBo...",
        "....oooooooo....",
        "................",
        "................",
        "................",
    ], {"o": OUTLINE, "k": (150, 92, 50), "K": (114, 66, 34), "B": (88, 150, 210), "W": (240, 244, 250)}),
    "catnip_cookie": ([
        "................",
        "................",
        "......oooo......",
        "....ooCCCCoo....",
        "...oCCgCCCCCo...",
        "..oCCCCCCgCCCo..",
        "..oCgCCCCCCCCo..",
        ".oCCCCCcCCCCgCo.",
        ".oCCCCCCCCCCCCo.",
        ".oCCgCCCCCgCCCo.",
        "..oCCCCcCCCCCo..",
        "..oCCCCCCCgCCo..",
        "...oCCgCCCCCo...",
        "....ooCCCCoo....",
        "......oooo......",
        "................",
    ], {"o": (130, 84, 44), "C": (222, 172, 104), "c": (196, 140, 76), "g": (90, 170, 70)}),
    "squid_tentacle": ([
        "................",
        "................",
        "...........oo...",
        "..........oTTo..",
        ".........oTTo...",
        "........oTsTo...",
        ".......oTTTo....",
        "......oTsTo.....",
        ".....oTTTo......",
        "....oTsTo.......",
        "...oTTTo........",
        "..oTsTo..oo.....",
        "..oTTTooTTo.....",
        "...oTTTTTo......",
        "....ooooo.......",
        "................",
    ], {"o": (40, 52, 74), "T": (84, 110, 150), "s": (170, 190, 220)}),
    "takoyaki": ([
        "................",
        "................",
        "................",
        "................",
        "...oo...oo...oo.",
        "..oBBo.oBBo.oBBo",
        ".oBsgBoBgsBoBsgB",
        ".oBBsBoBsBBoBBsB",
        ".oBBBBoBBBBoBBBB",
        "..oBBo.oBBo.oBBo",
        ".PPPPPPPPPPPPPPP",
        ".PpppppppppppppP",
        "..PPPPPPPPPPPPP.",
        "................",
        "................",
        "................",
    ], {"o": (90, 50, 26), "B": (196, 128, 62), "s": (90, 40, 20), "g": (110, 180, 80),
        "P": (210, 190, 150), "p": (236, 220, 186)}),
    "bubble_tea": ([
        "........pp......",
        ".........p......",
        "....ooooopoo....",
        "...oLLLLLpLLo...",
        "....oTTTTpTo....",
        "....oTTTTpTo....",
        "....oTTTTTTo....",
        "....oTTTTTTo....",
        ".....oTTTTo.....",
        ".....oTTTTo.....",
        ".....odTdTo.....",
        ".....oddddo.....",
        "......oooo......",
        "................",
        "................",
        "................",
    ], {"o": (120, 110, 120), "L": (250, 180, 210), "p": (240, 110, 160), "T": (222, 190, 150), "d": (60, 36, 30)}),
    "tuna_can": ([
        "................",
        "................",
        "................",
        "....oooooooo....",
        "...oSSSSSSSSo...",
        "..oSsSSSSSSsSo..",
        "..oSSSSSSSSSSo..",
        "..oBBBBBBBBBBo..",
        "..oBWWBBBBBBBo..",
        "..oBWWWWWBBBBo..",
        "..oBBWWWBBBBBo..",
        "..oBBBBBBBBBBo..",
        "..oSSSSSSSSSSo..",
        "...oooooooooo...",
        "................",
        "................",
    ], {"o": (70, 74, 84), "S": (196, 200, 210), "s": (150, 156, 168), "B": (60, 110, 190), "W": (240, 244, 250)}),
}


def shapeless(name, ingredients, count=1):
    # crafting has no "food" category (only cooking does); no category = misc, like vanilla pumpkin pie
    return {"type": "minecraft:crafting_shapeless", "ingredients": ingredients,
            "result": {"id": f"catgirl:{name}", "count": count}}


def cooking(kind, ingredient, result, time):
    return {"type": f"minecraft:{kind}", "category": "food", "cookingtime": time, "experience": 0.35,
            "ingredient": ingredient, "result": {"id": result}}


LANG = {
    "en_us": {
        "onigiri": "Tuna Onigiri", "raw_fish_sticks": "Raw Fish Sticks", "fish_sticks": "Fish Sticks",
        "milk_saucer": "Saucer of Milk", "cat_food": "Cat Food", "catnip_cookie": "Catnip Cookie",
        "squid_tentacle": "Squid Tentacle", "takoyaki": "Takoyaki", "bubble_tea": "Milk Bubble Tea", "tuna_can": "Can of Tuna",
        "tooltip.cat_food": "8 food when you wear a set, 2 otherwise. Cats love it",
        "tooltip.milk_saucer": "Clears bad effects, keeps the good ones",
    },
    "ru_ru": {
        "onigiri": "Онигири с тунцом", "raw_fish_sticks": "Сырые рыбные палочки", "fish_sticks": "Рыбные палочки",
        "milk_saucer": "Молоко в блюдце", "cat_food": "Кошачий корм", "catnip_cookie": "Мятное печенье",
        "squid_tentacle": "Щупальце кальмара", "takoyaki": "Такояки", "bubble_tea": "Молочный бабл-ти", "tuna_can": "Банка тунца",
        "tooltip.cat_food": "8 сытости в любом сете, иначе 2. Кошки обожают",
        "tooltip.milk_saucer": "Снимает плохие эффекты, хорошие оставляет",
    },
}


def main():
    for name, (rows, palette) in ICONS.items():
        path = ASSETS / f"textures/item/{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        icon(rows, palette).save(path)
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"catgirl:item/{name}"}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"catgirl:item/{name}"}})

    recipes = DATA / "catgirl/recipe"
    write(recipes / "onigiri.json", shapeless("onigiri", ["#catgirl:cooked_fish", "minecraft:wheat", "minecraft:dried_kelp"]))
    write(recipes / "raw_fish_sticks.json", shapeless("raw_fish_sticks", ["minecraft:cod", "minecraft:wheat"], 4))
    write(recipes / "fish_sticks.json", cooking("smelting", "catgirl:raw_fish_sticks", "catgirl:fish_sticks", 200))
    write(recipes / "fish_sticks_from_smoking.json", cooking("smoking", "catgirl:raw_fish_sticks", "catgirl:fish_sticks", 100))
    write(recipes / "fish_sticks_from_campfire_cooking.json",
          cooking("campfire_cooking", "catgirl:raw_fish_sticks", "catgirl:fish_sticks", 600))
    write(recipes / "milk_saucer.json",
          shapeless("milk_saucer", ["minecraft:milk_bucket", "minecraft:bowl", "minecraft:bowl", "minecraft:bowl"], 3))
    write(recipes / "cat_food.json", shapeless("cat_food", ["#catgirl:raw_fish", "minecraft:bone", "minecraft:wheat"], 2))
    write(recipes / "catnip_cookie.json", shapeless("catnip_cookie", ["catgirl:catnip", "minecraft:wheat", "minecraft:sugar"], 4))
    write(recipes / "takoyaki.json", shapeless("takoyaki", ["catgirl:squid_tentacle", "minecraft:wheat"]))
    write(recipes / "bubble_tea.json", shapeless("bubble_tea",
          ["minecraft:milk_bucket", "minecraft:sugar", "minecraft:sweet_berries", "minecraft:glass_bottle"]))
    write(recipes / "tuna_can.json", shapeless("tuna_can",
          ["minecraft:iron_ingot", "#catgirl:raw_fish", "#catgirl:raw_fish", "#catgirl:raw_fish", "#catgirl:raw_fish"], 4))

    tags = DATA / "catgirl/tags/item"
    write(tags / "raw_fish.json", {"values": ["minecraft:cod", "minecraft:salmon"]})
    write(tags / "cooked_fish.json", {"values": ["minecraft:cooked_cod", "minecraft:cooked_salmon"]})
    # cats are tempted by and tamed with these too (added to the vanilla tag, not replacing it)
    write(DATA / "minecraft/tags/item/cat_food.json", {"replace": False, "values": ["catgirl:cat_food", "catgirl:tuna_can"]})

    for lang, names in LANG.items():
        path = ASSETS / f"lang/{lang}.json"
        d = json.loads(path.read_text())
        for key, value in names.items():
            d[("tooltip.catgirl." + key[len("tooltip."):]) if key.startswith("tooltip.") else f"item.catgirl.{key}"] = value
        path.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n")
    print("ok")


if __name__ == "__main__":
    main()

"""Mobs: placeholder skins in the 64x64 player layout (4 per cat-person mob), spawn egg icons, the Stray
Neko's trades, spawn biome tags, sounds and lang. Run from the repo root: python3 tools/gen_mobs.py.
Skins are painted from a small spec per variant; replace them with hand-drawn ones later (same names)."""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/catgirl"
DATA = ROOT / "common/src/main/resources/data/catgirl"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n")


def shade(rgb, amount):
    """amount > 0 lighter, < 0 darker."""
    target = 255 if amount > 0 else 0
    return tuple(int(c + (target - c) * abs(amount)) for c in rgb)


# ------------------------------------------------------------------ skin painter (classic 64x64 layout)

def faces(u, v, w, h, d):
    """Texture rects (x, y, width, height) of a box's faces."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


HEAD, BODY = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4)
R_ARM, L_ARM = (40, 16, 4, 12, 4), (32, 48, 4, 12, 4)
R_LEG, L_LEG = (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)
HAT = (32, 0, 8, 8, 8)


class Skin:
    def __init__(self):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def px(self, x, y, rgb):
        self.img.putpixel((x, y), (*rgb, 255))

    def rect(self, x, y, w, h, rgb):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.px(xx, yy, rgb)

    def box(self, part, rgb, rows=None, shaded=True):
        """Fills every face of a box; rows=(first, last) limits the side faces to those rows."""
        for name, (x, y, w, h) in faces(*part).items():
            r0, r1 = (0, h) if rows is None or name in ("top", "bottom") else rows
            color = shade(rgb, -0.12) if shaded and name in ("right", "left", "back") else rgb
            for yy in range(y + r0, y + min(r1, h)):
                for xx in range(x, x + w):
                    self.px(xx, yy, color)

    def face(self, part, name):
        return faces(*part)[name]


def paint(spec):
    s = Skin()
    skin, hair, eyes = spec["skin"], spec["hair"], spec["eyes"]
    top, bottom, shoes = spec["top"], spec["bottom"], spec["shoes"]
    accent = spec.get("accent", shade(top, -0.25))

    # head: skin, then hair
    s.box(HEAD, skin)
    x, y, w, h = s.face(HEAD, "top")
    s.rect(x, y, w, h, hair)
    x, y, w, h = s.face(HEAD, "back")
    s.rect(x, y, w, h if spec["hair_len"] != "short" else 5, hair)
    for side in ("right", "left"):
        x, y, w, h = s.face(HEAD, side)
        s.rect(x, y, w, 3, hair)
        if spec["hair_len"] == "long":
            back_cols = range(x, x + 5) if side == "right" else range(x + 3, x + 8)
            for xx in back_cols:
                for yy in range(y + 3, y + 8):
                    s.px(xx, yy, shade(hair, -0.08))
        elif spec["hair_len"] == "bob":
            s.rect(x, y + 3, w, 3, shade(hair, -0.08))
    fx, fy, _, _ = s.face(HEAD, "front")
    s.rect(fx, fy, 8, 2, hair)                       # bangs
    for col in (0, 2, 3, 5, 7):
        s.px(fx + col, fy + 2, shade(hair, -0.1))
    if spec["hair_len"] in ("long", "bob"):          # strands framing the face
        for row in range(2, 7 if spec["hair_len"] == "long" else 5):
            s.px(fx, fy + row, hair)
            s.px(fx + 7, fy + row, hair)
    for ex in (1, 5):                                # eyes: white + iris, iris below
        s.px(fx + ex, fy + 4, (250, 250, 250))
        s.px(fx + ex + 1, fy + 4, eyes)
        s.px(fx + ex, fy + 5, shade(eyes, -0.3))
        s.px(fx + ex + 1, fy + 5, eyes)
    s.px(fx + 3, fy + 6, shade(skin, -0.3))          # a small cat mouth
    s.px(fx + 4, fy + 6, shade(skin, -0.3))
    if spec.get("blush"):
        s.px(fx + 1, fy + 6, shade((240, 130, 150), 0.2))
        s.px(fx + 6, fy + 6, shade((240, 130, 150), 0.2))

    # body and arms
    s.box(BODY, top)
    sleeve = spec.get("sleeve", 12)
    for arm in (R_ARM, L_ARM):
        s.box(arm, skin)
        s.box(arm, top, rows=(0, sleeve))
        x, y, w, h = s.face(arm, "top")
        s.rect(x, y, w, h, top)
    bx, by, _, _ = s.face(BODY, "front")
    design = spec.get("design")
    if design == "hoodie":
        s.rect(bx + 1, by + 7, 6, 3, accent)          # pocket
        s.px(bx + 3, by + 1, (240, 240, 240))         # drawstrings
        s.px(bx + 4, by + 1, (240, 240, 240))
        s.px(bx + 3, by + 2, (240, 240, 240))
        s.px(bx + 4, by + 2, (240, 240, 240))
    elif design == "vest":
        s.rect(bx + 2, by, 4, 12, spec["shirt"])      # shirt under an open vest
        for row in range(2, 10, 3):
            s.px(bx + 1, by + row, accent)            # vest pockets
            s.px(bx + 6, by + row, accent)
    elif design == "kimono":
        for row in range(5):                          # V neckline
            s.px(bx + 3 - row // 2, by + row, accent)
            s.px(bx + 4 + row // 2, by + row, accent)
        s.rect(bx, by + 6, 8, 2, spec["sash"])        # obi
        for side in ("right", "left", "back"):
            x, y, w, h = s.face(BODY, side)
            s.rect(x, y + 6, w, 2, shade(spec["sash"], -0.1))
    elif design == "robe":
        s.rect(bx + 3, by, 2, 12, accent)             # front opening
        s.rect(bx, by + 6, 8, 1, spec["sash"])

    # legs
    pants = spec.get("pants", 12)
    for leg in (R_LEG, L_LEG):
        s.box(leg, skin)
        s.box(leg, bottom, rows=(0, pants))
        x, y, w, h = s.face(leg, "top")
        s.rect(x, y, w, h, bottom)
        s.box(leg, shoes, rows=(10, 12))
        x, y, w, h = s.face(leg, "bottom")
        s.rect(x, y, w, h, shade(shoes, -0.3))

    # hat layer: only a straw hat (the fisher)
    if spec.get("straw_hat"):
        straw = (226, 196, 120)
        x, y, w, h = faces(*HAT)["top"]
        s.rect(x, y, w, h, straw)
        for side in ("front", "back", "left", "right"):
            x, y, w, h = faces(*HAT)[side]
            s.rect(x, y, w, 2, straw if side != "front" else shade(straw, -0.1))
            s.rect(x, y + 1, w, 1, (180, 60, 60))     # hat band
    return s.img


SKIN_LIGHT, SKIN_MID, SKIN_TAN, SKIN_DEEP, SKIN_PALE = (247, 214, 190), (232, 188, 152), (198, 146, 108), (140, 96, 66), (240, 232, 236)
SPECS = {
    # casual street cats: two catgirls, two catboys (catboys are equal, not a footnote)
    "stray_neko": [
        dict(skin=SKIN_LIGHT, hair=(150, 84, 46), hair_len="long", eyes=(70, 150, 90), top=(236, 140, 60), design="hoodie",
             bottom=(60, 60, 80), pants=5, shoes=(240, 240, 240), blush=True),
        dict(skin=SKIN_TAN, hair=(40, 36, 44), hair_len="short", eyes=(210, 170, 50), top=(130, 130, 140), design="hoodie",
             bottom=(70, 100, 160), shoes=(50, 50, 56)),
        dict(skin=SKIN_MID, hair=(240, 150, 190), hair_len="bob", eyes=(120, 90, 200), top=(190, 160, 230), design="hoodie",
             bottom=(50, 44, 64), pants=6, shoes=(240, 200, 220), blush=True),
        dict(skin=SKIN_DEEP, hair=(236, 228, 210), hair_len="short", eyes=(70, 170, 200), top=(60, 170, 160), design="hoodie",
             bottom=(150, 140, 100), shoes=(90, 70, 50)),
    ],
    "fisher_neko": [
        dict(skin=SKIN_TAN, hair=(120, 80, 50), hair_len="short", eyes=(80, 130, 200), top=(110, 120, 70), shirt=(240, 240, 230),
             design="vest", bottom=(70, 90, 130), pants=8, shoes=(70, 60, 50), sleeve=5, straw_hat=True),
        dict(skin=SKIN_LIGHT, hair=(220, 120, 60), hair_len="long", eyes=(60, 140, 90), top=(90, 110, 80), shirt=(250, 240, 220),
             design="vest", bottom=(60, 80, 120), pants=8, shoes=(80, 60, 40), sleeve=5, straw_hat=True, blush=True),
        dict(skin=SKIN_DEEP, hair=(30, 28, 30), hair_len="short", eyes=(200, 160, 60), top=(130, 110, 70), shirt=(220, 230, 240),
             design="vest", bottom=(80, 80, 90), pants=8, shoes=(50, 50, 50), sleeve=5, straw_hat=True),
        dict(skin=SKIN_MID, hair=(160, 160, 170), hair_len="bob", eyes=(90, 160, 210), top=(100, 120, 90), shirt=(240, 220, 220),
             design="vest", bottom=(70, 100, 140), pants=8, shoes=(70, 50, 40), sleeve=5, straw_hat=True),
    ],
    # dark kimonos, red obi, gold eyes: the night thief
    "bakeneko": [
        dict(skin=SKIN_PALE, hair=(30, 26, 34), hair_len="long", eyes=(240, 200, 60), top=(56, 40, 70), accent=(150, 40, 50),
             sash=(170, 40, 50), design="kimono", bottom=(40, 30, 50), shoes=(30, 26, 30)),
        dict(skin=SKIN_LIGHT, hair=(90, 90, 100), hair_len="short", eyes=(240, 200, 60), top=(34, 34, 44), accent=(150, 40, 50),
             sash=(160, 30, 40), design="kimono", bottom=(30, 30, 40), shoes=(26, 26, 30)),
        dict(skin=SKIN_MID, hair=(236, 228, 214), hair_len="long", eyes=(230, 90, 60), top=(70, 30, 40), accent=(30, 26, 30),
             sash=(30, 26, 34), design="kimono", bottom=(50, 24, 30), shoes=(26, 26, 30)),
        dict(skin=SKIN_TAN, hair=(40, 30, 30), hair_len="bob", eyes=(240, 200, 60), top=(40, 50, 70), accent=(150, 40, 50),
             sash=(200, 150, 50), design="kimono", bottom=(30, 34, 50), shoes=(26, 26, 30)),
    ],
    # white robes, silver hair: drawn see-through and tinted in game
    "cat_spirit": [
        dict(skin=SKIN_PALE, hair=(236, 240, 250), hair_len="long", eyes=(110, 200, 240), top=(244, 246, 252), accent=(200, 220, 240),
             sash=(150, 200, 240), design="robe", bottom=(236, 240, 250), shoes=(220, 230, 245)),
        dict(skin=SKIN_PALE, hair=(210, 220, 240), hair_len="short", eyes=(110, 200, 240), top=(240, 244, 252), accent=(200, 220, 240),
             sash=(170, 150, 230), design="robe", bottom=(230, 236, 248), shoes=(220, 230, 245)),
        dict(skin=SKIN_PALE, hair=(250, 250, 255), hair_len="bob", eyes=(150, 230, 200), top=(246, 248, 252), accent=(210, 230, 240),
             sash=(150, 220, 200), design="robe", bottom=(240, 244, 250), shoes=(220, 230, 245)),
        dict(skin=SKIN_PALE, hair=(200, 210, 230), hair_len="long", eyes=(200, 160, 240), top=(236, 240, 250), accent=(200, 210, 240),
             sash=(200, 170, 240), design="robe", bottom=(230, 234, 248), shoes=(220, 230, 245)),
    ],
}


# Spawn eggs like vanilla's since 1.21.5: the mob itself drawn on the egg (face, hair, clothes, ears poking out),
# not the old spotted two-tone egg. Grids hold fills only; the rim of the shape is darkened into an outline and the
# pixels just inside it are shaded (light top-left, dark bottom-right), the way vanilla's eggs are lit.
EGGS = {
    "stray_neko": dict(
        colors={"H": (150, 84, 46), "h": (184, 116, 70), "E": (240, 170, 180), "S": (247, 214, 190), "s": (226, 186, 160),
                "G": (70, 150, 90), "B": (240, 160, 160), "M": (190, 110, 100), "O": (236, 140, 60), "o": (206, 112, 40),
                "W": (250, 245, 240)},
        rows=["................",
              "...H........H...",
              "...HE.HHHH.EH...",
              "...HEHHhhHHEH...",
              "...HHHHHHHHHH...",
              "..HHHSSSSSSHHH..",
              "..HHSGSSSSGSHH..",
              "..HHBSSMMSSBHH..",
              "..HHsSSSSSSsHH..",
              "..HOOOOOOOOOOH..",
              "..HOOOWOOWOOOH..",
              "..OOOOWOOWOOOO..",
              "...OOOOOOOOOO...",
              "....oOOOOOOo....",
              ".....oooooo....."]),
    "fisher_neko": dict(
        colors={"H": (120, 80, 50), "E": (230, 170, 170), "Y": (226, 196, 120), "y": (196, 164, 92), "R": (180, 60, 60),
                "S": (198, 146, 108), "s": (176, 126, 92), "G": (80, 130, 200), "M": (140, 80, 60), "V": (110, 120, 70),
                "T": (240, 240, 230)},
        rows=["................",
              "...H........H...",
              "...HE.YYYY.EH...",
              "...HEYYYYYYEH...",
              "..YYRRRRRRRRYY..",
              ".yYYYYYYYYYYYYy.",
              "..HHSSSSSSSSHH..",
              "..HSSGSSSSGSSH..",
              "..HSSSSMMSSSSH..",
              "..HsSSSSSSSSsH..",
              "..VVVTTTTTTVVV..",
              "..VVVVTTTTVVVV..",
              "...VVVVTTVVVV...",
              "....VVVTTVVV....",
              ".....VVVVVV....."]),
    "bakeneko": dict(
        colors={"H": (30, 26, 34), "h": (70, 62, 82), "E": (200, 120, 140), "S": (240, 232, 236), "Y": (240, 200, 60),
                "X": (70, 36, 10), "F": (255, 255, 255), "M": (120, 40, 50), "P": (56, 40, 70), "A": (150, 40, 50),
                "R": (170, 40, 50)},
        rows=["................",
              "...H........H...",
              "...HE.HHHH.EH...",
              "...HEHHhhHHEH...",
              "...HHHHHHHHHH...",
              "..HHHSSSSSSHHH..",
              "..HHSYXSSXYSHH..",
              "..HHSSSSSSSSHH..",
              "..HHSSFMMFSSHH..",
              "..HPPPASSAPPPH..",
              "..HPPPPAAPPPPH..",
              "..PPPPPPPPPPPP..",
              "...RRRRRRRRRR...",
              "....PPPPPPPP....",
              ".....PPPPPP....."]),
    # pale and ghostly with the Nine Lives halo; its rim is a soft blue instead of a dark one
    "cat_spirit": dict(
        colors={"L": (250, 220, 90), "H": (225, 232, 248), "E": (200, 210, 240), "S": (240, 232, 236), "C": (110, 200, 240),
                "M": (220, 160, 180), "R": (244, 246, 252), "B": (150, 200, 240)},
        rim=(120, 170, 220), unrimmed="L",
        rows=[".....LLLLLL.....",
              "...H........H...",
              "...HE.HHHH.EH...",
              "...HEHHHHHHEH...",
              "...HHHHHHHHHH...",
              "..HHHSSSSSSHHH..",
              "..HHSCSSSSCSHH..",
              "..HHSSSSSSSSHH..",
              "..HHSSSMMSSSHH..",
              "..HRRRRSSRRRRH..",
              "..HRRRRBBRRRRH..",
              "..RRRRRRRRRRRR..",
              "...BBBBBBBBBB...",
              "....RRRRRRRR....",
              ".....RRRRRR....."]),
    # a ginger tabby kitten's face filling the egg, like vanilla's cat egg
    "kitten": dict(
        colors={"O": (240, 160, 90), "t": (200, 110, 50), "P": (240, 170, 180), "G": (120, 190, 90), "K": (30, 30, 30),
                "W": (250, 245, 235), "N": (240, 140, 160)},
        rows=["................",
              "................",
              "...O........O...",
              "...OP.OOOO.PO...",
              "...OPOOttOOPO...",
              "...OOOtOOtOOO...",
              "..OOOOOOOOOOOO..",
              "..OOGGOOOOGGOO..",
              "..OOGKOOOOKGOO..",
              "..OtOOWWWWOOtO..",
              "..OOOWWNNWWOOO..",
              "..OOOOWWWWOOOO..",
              "...OOOOWWOOOO...",
              "....OOOOOOOO....",
              ".....OOOOOO....."]),
}

RIM = -0.45
SHADOW = -0.12
LIGHT = 0.1
# the whole egg is lit from the top-left: this much lighter at the top-left corner, darker at the bottom-right
VOLUME = 0.1


def spawn_egg(egg):
    rows, colors = egg["rows"], egg["colors"]
    unrimmed = egg.get("unrimmed", "")

    def filled(x, y):
        return 0 <= y < len(rows) and 0 <= x < 16 and rows[y][x] != "."

    def rim(x, y):
        return rows[y][x] not in unrimmed and not all(filled(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (row, y)
        for x, c in enumerate(row):
            if c == ".":
                continue
            color = colors[c]
            if rim(x, y):
                color = egg.get("rim") or shade(color, RIM)
            elif c not in unrimmed:
                color = shade(color, VOLUME * (1 - (x + y) / 15))
                if rim(x + 1, y) or rim(x, y + 1):
                    color = shade(color, SHADOW)
                elif rim(x - 1, y) or rim(x, y - 1):
                    color = shade(color, LIGHT)
            img.putpixel((x, y), (*color, 255))
    return img


def trade(gives, emeralds, max_uses=6, count=1, components=None, xp=5):
    item = {"id": gives, "count": count}
    if components:
        item["components"] = components
    return {"wants": {"id": "minecraft:emerald", "count": emeralds}, "gives": item, "max_uses": max_uses, "xp": xp,
            "reputation_discount": 0.0}


TRADES = {
    "ears_ginger": trade("catgirl:cat_ears", 5, components={"catgirl:coat": "ginger"}),
    "ears_gray": trade("catgirl:cat_ears", 5, components={"catgirl:coat": "gray"}),
    "ears_black": trade("catgirl:cat_ears", 5, components={"catgirl:coat": "black"}),
    "ears_white": trade("catgirl:cat_ears", 5, components={"catgirl:coat": "white"}),
    "ears_pink": trade("catgirl:cat_ears", 6, components={"catgirl:coat": "pink"}),
    "tail_ginger": trade("catgirl:cat_tail", 6, components={"catgirl:coat": "ginger"}),
    "tail_black": trade("catgirl:cat_tail", 6, components={"catgirl:coat": "black"}),
    # rare coats (no recipe): dear and once per neko; the fisher's quest is the main way to them
    "tail_siamese": trade("catgirl:cat_tail", 16, max_uses=1, components={"catgirl:coat": "siamese"}, xp=10),
    "tail_calico": trade("catgirl:cat_tail", 16, max_uses=1, components={"catgirl:coat": "calico"}, xp=10),
    "bell_collar": trade("catgirl:bell_collar", 4),
    "velvet_choker": trade("catgirl:velvet_choker", 4),
    "neck_ribbon": trade("catgirl:neck_ribbon", 3),
    "cod": trade("minecraft:cod", 1, max_uses=12, count=6, xp=2),
    "salmon": trade("minecraft:salmon", 1, max_uses=12, count=5, xp=2),
    "tuna_can": trade("catgirl:tuna_can", 2, max_uses=8, count=3, xp=3),
    "yarn_ball": trade("catgirl:yarn_ball", 1, max_uses=8, count=2, xp=2),
    "catnip": trade("catgirl:catnip", 2, max_uses=8, count=4, xp=3),
}

SPAWN_BIOMES = {
    "stray_neko": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:taiga",
                   "minecraft:snowy_plains", "minecraft:desert", "minecraft:meadow"],
    "fisher_neko": ["#minecraft:is_beach", "#minecraft:is_river"],
    "bakeneko": ["#minecraft:is_forest", "minecraft:bamboo_jungle", "minecraft:jungle"],
    "cat_spirit": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "#minecraft:is_forest",
                   "#minecraft:is_taiga", "minecraft:snowy_plains", "minecraft:cherry_grove"],
}

NAMES = {
    "en_us": {"stray_neko": "Stray Neko", "fisher_neko": "Fisher Neko", "bakeneko": "Bakeneko", "cat_spirit": "Cat Spirit",
              "kitten": "Kitten"},
    "ru_ru": {"stray_neko": "Бродячая неко", "fisher_neko": "Неко-рыбак", "bakeneko": "Бакенеко", "cat_spirit": "Кошачий дух",
              "kitten": "Котёнок"},
}
EGG_NAME = {"en_us": "%s Spawn Egg", "ru_ru": "Яйцо призыва: %s"}
MESSAGES = {
    "en_us": {
        "message.catgirl.stray_neko.trusts": "%s trusts you now",
        "message.catgirl.stray_neko.kitten": "%s brought a kitten to meet you!",
        "message.catgirl.fisher_neko.quest": "\"Bring me ten fish, I've got something rare for you.\" (%s/%s)",
        "message.catgirl.fisher_neko.progress": "Fish for the fisher: %s/%s",
        "message.catgirl.fisher_neko.reward": "\"Purr-fect catch! These are for you.\"",
        "message.catgirl.bakeneko.stole": "A bakeneko snatched your %s!",
        "message.catgirl.bakeneko.distrust": "The bakeneko only bargains with a Full Neko",
        "message.catgirl.bakeneko.returned": "The bakeneko trades your things back for the catnip",
        "message.catgirl.cat_spirit.gift": "The spirit purrs: you have nine lives tonight",
        "effect.catgirl.nine_lives": "Nine Lives",
        "subtitles.catgirl.entity.bakeneko.hiss": "Bakeneko hisses", "subtitles.catgirl.entity.cat_spirit.fade": "Cat spirit fades",
    },
    "ru_ru": {
        "message.catgirl.stray_neko.trusts": "%s теперь вам доверяет",
        "message.catgirl.stray_neko.kitten": "%s привела знакомиться котёнка!",
        "message.catgirl.fisher_neko.quest": "«Принеси десять рыбок, и я дам кое-что редкое». (%s/%s)",
        "message.catgirl.fisher_neko.progress": "Рыба для рыбака: %s/%s",
        "message.catgirl.fisher_neko.reward": "«Мур-р-рекрасный улов! Это тебе».",
        "message.catgirl.bakeneko.stole": "Бакенеко стащил ваш предмет: %s!",
        "message.catgirl.bakeneko.distrust": "Бакенеко торгуется только с Полной неко",
        "message.catgirl.bakeneko.returned": "Бакенеко отдаёт ваше за мяту",
        "message.catgirl.cat_spirit.gift": "Дух мурлычет: этой ночью у вас девять жизней",
        "effect.catgirl.nine_lives": "Девять жизней",
        "subtitles.catgirl.entity.bakeneko.hiss": "Бакенеко шипит", "subtitles.catgirl.entity.cat_spirit.fade": "Кошачий дух тает",
    },
}


def main():
    for kind, variants in SPECS.items():
        for i, spec in enumerate(variants):
            path = ASSETS / f"textures/entity/neko/{kind}_{i}.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            paint(spec).save(path)
    for kind, egg in EGGS.items():
        name = f"{kind}_spawn_egg"
        spawn_egg(egg).save(ASSETS / f"textures/item/{name}.png")
        write(ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"catgirl:item/{name}"}})
        write(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"catgirl:item/{name}"}})

    # nine lives effect icon: a little white cat head with a halo
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    rows = ["......yyyyyy......", ".....y......y.....", "......yyyyyy......", "..................",
            "...w..........w...", "...ww........ww...", "...wwwwwwwwwwww...", "..wwwwwwwwwwwwww..",
            "..wwwbwwwwwwbwww..", "..wwwbwwwwwwbwww..", "..wwwwwwppwwwwww..", "..wwwwwwwwwwwwww..",
            "...wwwwwwwwwwww...", "....wwwwwwwwww....", "..................", "..................", "..................",
            ".................."]
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            color = {"y": (250, 220, 90), "w": (240, 246, 255), "b": (110, 200, 240), "p": (240, 150, 170)}.get(c)
            if color:
                icon.putpixel((x, y), (*color, 255))
    icon.save(ASSETS / "textures/mob_effect/nine_lives.png")

    for name, offer in TRADES.items():
        write(DATA / f"villager_trade/stray_neko/{name}.json", offer)
    write(DATA / "tags/villager_trade/stray_neko.json", {"values": [f"catgirl:stray_neko/{n}" for n in TRADES]})
    write(DATA / "trade_set/stray_neko.json", {"amount": 6, "random_sequence": "catgirl:trade_set/stray_neko",
                                              "trades": "#catgirl:stray_neko"})
    for kind, biomes in SPAWN_BIOMES.items():
        write(DATA / f"tags/worldgen/biome/spawns/{kind}.json", {"values": biomes})

    sounds_path = ASSETS / "sounds.json"
    sounds = json.loads(sounds_path.read_text())
    sounds["entity.bakeneko.hiss"] = {"subtitle": "subtitles.catgirl.entity.bakeneko.hiss",
                                      "sounds": [{"name": f"minecraft:mob/cat/hiss{i}", "pitch": 0.8} for i in (1, 2, 3)]}
    sounds["entity.cat_spirit.fade"] = {"subtitle": "subtitles.catgirl.entity.cat_spirit.fade",
                                        "sounds": [{"name": f"minecraft:mob/cat/purreow{i}", "pitch": 1.5, "volume": 0.6} for i in (1, 2)]}
    sounds_path.write_text(json.dumps(sounds, indent=2) + "\n")

    for lang, names in NAMES.items():
        path = ASSETS / f"lang/{lang}.json"
        d = json.loads(path.read_text())
        for kind, name in names.items():
            d[f"entity.catgirl.{kind}"] = name
            d[f"item.catgirl.{kind}_spawn_egg"] = EGG_NAME[lang] % name
        d.update(MESSAGES[lang])
        path.write_text(json.dumps(d, indent=2, ensure_ascii=False) + "\n")
    print("ok")


if __name__ == "__main__":
    main()

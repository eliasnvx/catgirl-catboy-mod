"""Recipe-book unlocks, like vanilla's: one hidden advancement per recipe under minecraft:recipes/root. A recipe opens
as soon as the player holds any of its telling ingredients (very common ones such as sticks or wheat don't count
unless nothing else is left); a dye recipe opens when the player holds the item it recolours.
Reads every recipe in data/catgirl/recipe, so run it after adding or changing recipes:
python3 tools/gen_recipe_unlocks.py (from the repo root)."""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/catgirl"
RECIPES = DATA / "recipe"
OUT = DATA / "advancement/recipes"

# too common to be a hint: every early game would unlock half the mod at once
COMMON = {"minecraft:stick", "minecraft:wheat", "minecraft:sugar", "minecraft:bowl", "minecraft:glass_bottle"}
FOOD = {"onigiri", "fish_sticks", "raw_fish_sticks", "milk_saucer", "cat_food", "catnip_cookie", "takoyaki",
        "bubble_tea", "tuna_can"}
BLOCKS = {"cat_bed", "cardboard_box"}


def ingredients(recipe):
    """Item ids and #tags a recipe takes, in recipe order, without repeats."""
    kind = recipe["type"].split(":")[1]
    if kind == "crafting_shaped":
        raw = list(recipe["key"].values())
    elif kind == "crafting_shapeless":
        raw = recipe["ingredients"]
    elif kind == "crafting_dye":
        raw = [recipe["target"]]
    else:
        raw = [recipe["ingredient"]]
    found = []
    for entry in raw:
        for item in entry if isinstance(entry, list) else [entry]:
            if item not in found:
                found.append(item)
    return found


def criterion(item):
    return {"conditions": {"items": [{"items": item}]}, "trigger": "minecraft:inventory_changed"}


def criterion_name(item):
    return "has_" + item.lstrip("#").split(":")[1]


def folder(result):
    name = result.split(":")[1]
    if name in FOOD:
        return "food"
    if name in BLOCKS:
        return "decorations"
    return "cosmetics"


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    count = 0
    for path in sorted(RECIPES.glob("*.json")):
        recipe = json.loads(path.read_text())
        recipe_id = "catgirl:" + path.stem
        items = ingredients(recipe)
        hints = [item for item in items if item not in COMMON] or items
        criteria = {"has_the_recipe": {"conditions": {"recipes": recipe_id}, "trigger": "minecraft:recipe_unlocked"}}
        for item in hints:
            criteria[criterion_name(item)] = criterion(item)
        advancement = {
            "parent": "minecraft:recipes/root",
            "criteria": criteria,
            "requirements": [list(criteria)],
            "rewards": {"recipes": [recipe_id]},
        }
        result = recipe["result"]["id"] if isinstance(recipe["result"], dict) else recipe["result"]
        out = OUT / folder(result) / f"{path.stem}.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(advancement, indent=2) + "\n")
        count += 1
    print(f"{count} recipe unlocks -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()

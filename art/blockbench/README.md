# Blockbench models for the worn pieces

Every worn piece is drawn by a code model until a Blockbench/GeckoLib model for it exists. The folders here are
**starting models** exported from those code models, so you begin with exactly what the game shows:

- `<item>.geo.json`: the model (`paw_gloves` and `paw_mittens` also have `<item>_slim.geo.json` for slim arms)
- `<item>.png`: the fur texture the code models use, 64x64, grayscale

Regenerate them (overwrites this folder) with `./gradlew :common:exportBlockbenchStarters`.

## Setup (once)

1. Blockbench: <https://www.blockbench.net> (or `brew install --cask blockbench`).
2. *File → Plugins*, install **GeckoLib Models & Animations**.

## Open a starting model

1. *File → Open Model*, pick `<item>/<item>.geo.json`; open it as a **GeckoLib Animated Model**.
2. In the *Textures* panel, import `<item>/<item>.png` and apply it to the model if it isn't already.

## Rules the game relies on

- **Body bones.** Keep the top-level bones `armorHead`, `armorBody`, `armorLeftArm`, `armorRightArm`, `armorLeftLeg`,
  `armorRightLeg`. Put geometry inside the one it should move with (ears in `armorHead`, paws in the arm bones, the tail
  and collars in `armorBody`, slippers and leg warmers in the leg bones).
- **Coat parts.** The texture stays grayscale; the game colors it per coat (8 coats) or dye. Which color a cube takes
  comes from the name of the bone it sits in:

  | Bone name ends in | Fur pieces (ears, tail, paws…) | Dyeable pieces (collars, bows…) |
  |---|---|---|
  | `_inner` | inner ear, paw beans | darker shade of the dye |
  | `_points` | Siamese points (ear and tail tips) | gold (bells, rings) |
  | `_patch` | Calico patches, tabby stripes | dark (bell slit) |
  | `_tuft` | light fur tufts, the tail's belly | white highlights |
  | anything else | main fur color | the dye |

  The starters already sort the cubes this way (`right_ear_inner`, `bell_points`…). Light gray paints the full color,
  darker gray shades it.
- **Moving parts.** Bones named like the code model's moving parts move the same way in game: `left_ear` and
  `right_ear` (twitch, bounce, flatten when hurt, lift above a helmet), `segment0` … `segment15` of the tail (sway,
  lift while walking) and the collar's `bell` (swing). Keep those names and their nesting if you want the motion; the
  pose you model is the rest pose and the motion is added on top.
- Units are model pixels (1 unit = 1/16 block), like any player model.

## Put it in the game

1. *File → Export → Export GeckoLib Model*, save as `<item>.geo.json`; save the texture as `<item>.png`.
2. Copy them to
   - `common/src/main/resources/assets/catgirl/geckolib/models/cosmetic/<item>.geo.json`
     (and `<item>_slim.geo.json` for slim arms, if you made one)
   - `common/src/main/resources/assets/catgirl/textures/cosmetic/<item>.png`
3. Run the game (`./gradlew :fabric:runClient` or `:neoforge:runClient`). A model file there replaces the code model
   for that item; delete it to get the code model back. A resource pack can ship the same files.

First-person paws (the hand you see) still come from the code model.

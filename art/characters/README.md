# Characters in full outfits (for banners and promo art)

Blockbench projects of whole characters wearing a piece in every slot, built from the same models the game draws.
Pose them, light them, take screenshots or export them to Blender.

As in game, cat-ear headphones bring their own ears, so a worn pair of cat ears isn't drawn under them
(items in `#catgirl:covers_ears`).

| File | Arms | Coat | Outfit |
|---|---|---|---|
| `catgirl_ginger.bbmodel` | slim | Ginger | ears, ear bows, blush, bell collar, paw gloves, leg warmers, paw slippers, tail |
| `catboy_gray.bbmodel` | classic | Gray | cat-ear headphones (blue), whiskers, velvet choker, paw mittens, leg warmers, paw slippers, tail |
| `catgirl_calico.bbmodel` | slim | Calico | ears, bell headband, whiskers, neck ribbon, paw mittens, leg warmers, paw slippers, tail |
| `catboy_siamese.bbmodel` | classic | Siamese | cat-ear headphones, blush, bell collar, paw gloves, leg warmers, paw slippers, tail |

## Open

*File → Open Model* and pick a `.bbmodel`. It opens as a **Generic Model**; no plugin is needed.

## Two textures

- **skin.png**: a placeholder skin from the mod's nekos. Put your own 64x64 skin in its place: right-click
  `skin.png` in the *Textures* panel → *Change File* (or edit it with the paint tools). Use a slim skin on the
  catgirl files and a classic one on the catboy files.
- **outfit.png**: the fur texture, already tinted per coat part the way the game colors it. Leave it as it is
  unless you want other colors.

## Pose

Every body part is a group: `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`. The worn pieces sit
inside the part they move with (ears and headwear in `head`, paws in the arms, the tail and the collar in `body`, leg
warmers and slippers in the legs), so rotating a part takes its pieces along. Select a group in the *Outliner* and
use the rotate tool, or type the angles in the *Transform* panel. The tail is a chain of `segment0` … `segment15`
inside `body/cat_tail`: rotate segments to curl it. The ears (`right_ear`, `left_ear`) can tilt the same way.

The *Animate* tab works too, if you want several poses in one file.

## Pictures

- *View → Screenshot Model* (Cmd+P): the model cropped from the current angle.
- *View → Advanced Screenshot…*: pick the resolution and anti-aliasing for a big, sharp picture. Turn off
  *View → Show Grids* first for a clean background.
- *View → Record GIF…* for a turntable or an animation from the *Animate* tab.
- Other software: *File → Export → Export glTF Model* (or OBJ) and open it in Blender.

## Regenerate

`./gradlew :common:exportCharacters` writes the four files above again from the code models (after the models or
colors change). It overwrites only these four names: save your posed versions under other names
(*File → Save Project As*) so they stay.

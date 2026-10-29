# Clarity models

Two ready-to-use `.bbmodel` files for Clarity's **Player Model** module, plus the
tooling that generates and verifies them. No licence, no store, no download step —
these are plain files you drop on disk.

| Model | Triangles | Look |
|---|---|---|
| `arena_bot.bbmodel` | 324 | Blocky mech: cyan visor, glowing chest core, orange soles |
| `wraith.bbmodel` | 228 | Hooded caster: glowing eyes, gold sash, one asymmetric pauldron |

Previews are software renders from `tools/clarity/preview_model.py`
(`models/previews/*.png`).

## Install

1. Copy the `.bbmodel` files into `.minecraft/clarity/models/`
   (the folder is created automatically the first time the module opens;
   subfolders are scanned 2 levels deep).
2. **F5** — the module only renders when the camera is not first-person.
3. ClickGUI → **Visual → Player Model** → enable.
4. Click **Custom Model** → pick the file. `Model` switches to `Custom` for you.

Suggested settings for these two: `Model Height 1.85`, `Z Up` off,
`Animate Limbs` on, `Yaw Offset 0`, `Lighting` on. If a model ever renders
facing backwards, `Yaw Offset 180` is the escape hatch.

## Why these are free

`PlayerModel` never talks to a server. `ImportedModelRenderer.folder()` resolves
`.minecraft/clarity/models`, `ImportedModel.load()` parses `.bbmodel` / `.obj` /
`.glb` / `.gltf`, and that is the whole pipeline. There is no model marketplace,
no entitlement check, and nothing in the removed `need/clarity/agent/` package
had anything to do with models — the only thing ever gated was *running the
client*.

## Authoring your own

Blockbench (free) is the easiest path: build with cubes, save as `.bbmodel`,
drop it in the folder. Textures can be embedded in the `.bbmodel` (it reads the
base64 `source` field first) or sit next to it as a PNG.

Things the loader actually cares about — each of these will bite you:

**Facing.** `BlockbenchLoader.place()` returns `(-x, y, -z)`, a rigid 180°
yaw. So author **front = −Z**. That lands the model at front = +Z in renderer
space, which is what `ImportedModelRenderer.draw` wants
(`fx = -sin(yaw), fz = cos(yaw)` → forward is +Z) and what the built-in Kenny
model uses (`FACE_MIN_Z = +0.3`). Author front = +Z and it renders backwards.

**Left and right.** With front = −Z, the character's left side is at **+X**.
Author the arms/legs accordingly.

**Naming drives the rig.** `ImportedModel.kindOf()` scans an element's name then
its ancestors (reversed) for keywords:

- head bone: `head hair hat helmet face ear horn eye nose mouth jaw beard snout skull crown antler...`
- arm bone: `arm hand shoulder forearm sleeve wrist elbow finger...`
- leg bone: `leg foot feet thigh shin calf knee boot shoe sock toe...`

So `"Left Arm"`, `"Right Boot"`, `"Visor"` (no — `visor` is not a keyword, it
becomes `other`), `"Jaw"` (yes), `"Hood"` (**no** — use `"Hood Hat"`, which hits
`hat`). A mesh with no limb keyword falls back to a geometric classifier, but
naming is the reliable path.

**Rotation pivots at `origin`, in degrees, per vertex.** A tall box rotated
about an origin at its feet swings sideways violently — the loader rotates every
vertex, there is no skinning. Keep `origin` at the box centre (this generator's
default) or at the intended joint.

**Don't rotate the outliner group.** The loader applies an element's rotation
*and* every ancestor group's rotation. Duplicating a rotation in the group
doubles it.

**Limits.** 300,000 triangles max, else `Too detailed (over 300000 triangles)`.
The mesh is normalised on load (feet to y=0, height to 1, centred on X/Z), so
your editor's scale is irrelevant — `Model Height` sets the in-world height.

## Tooling

```bash
# generate a model (see MODELS at the bottom of the script to add your own)
python3 tools/clarity/make_model.py arena_bot -o out.bbmodel

# software preview, 3 arbitrary angles
python3 tools/clarity/preview_model.py out.bbmodel preview.png --yaw 34 --pitch 10
```

`make_model.py` builds `.bbmodel` JSON from a list of boxes, allocates a solid
palette cell per (colour, shade) pair, embeds the atlas as a base64 PNG, and
applies the conventions above. `preview_model.py` mirrors the loader's cube
path — same corner tables, same `place()` transform — and z-buffer rasterises so
you can catch models that parse fine but look wrong.

To verify against the **real** loader (needs a JDK 21+, since the source uses
switch expressions, and `gson.jar`):

```bash
mkdir -p /tmp/chk/src/need/clarity/modules/visual/playermodel
cp "Clarity/discord.gg-clarityclient-source-main/Client Source/need/clarity/modules/visual/playermodel/ImportedModel.java" /tmp/chk/src/need/clarity/modules/visual/playermodel/
javac -cp gson.jar -d out /tmp/chk/src/need/clarity/modules/visual/playermodel/ImportedModel.java
# then call ImportedModel.load(path, false) and read .triangles / .rigged / .parts
```

Both shipped models report `rigged = true` with all six bones populated
(head, armL, armR, legL, legR, other), which is what enables `Animate Limbs`.

## Licence

Generated here, released CC0 — do whatever you want with them.

#!/usr/bin/env python3
"""Generate Clarity-compatible .bbmodel files from a Python box description.

Clarity's Player Model feature reads `.minecraft/clarity/models/` and parses
.bbmodel / .obj / .glb / .gltf. Its .bbmodel loader is hand-rolled (see
ImportedModel.BlockbenchLoader), so this generator targets exactly what it
supports:

  * `elements` of type "cube" with `from`/`to`/`origin`/`rotation`
  * per-face `uv` rects in texture pixels, `texture: 0`
  * one embedded texture as a data: URI in `textures[0].source`
  * `outliner` groups whose names drive the rig

Rigging is name-based, not bone-based. `ImportedModel.kindOf` scans an
element's own name and then its ancestors (reversed) for keywords:

  head  : head hair hat helmet face ear horn eye nose mouth jaw beard ...
  arm   : arm hand shoulder forearm sleeve wrist elbow finger ...
  leg   : leg foot feet thigh shin calf knee boot shoe sock toe ...

Then the rig() pass splits limb triangles left/right by their x sign
(x > 0 is the character's LEFT when the model faces +Z), giving the six
animated bones: head, armL, armR, legL, legR, other.

So: name your boxes "Head", "Left Arm", "Right Leg", "Left Boot" etc. A mesh
with no limb keyword falls back to a geometric classifier, but naming is
reliable -- use it.
"""

import base64
import json
import struct
import uuid
import zlib
from dataclasses import dataclass, field

ATLAS = 128          # texture is ATLAS x ATLAS
CELL = 16            # palette cell size -> 8x8 = 64 cells
UV_INSET = 0.5       # keeps neighbouring cells from bleeding at the edges

# Face order used by the loader's unwrap. "up"/"down" get automatic shading.
FACES = ("north", "south", "east", "west", "up", "down")

# Shade multipliers per face: light from above, body sides flat, undersides dark.
SHADE = {"up": 1.16, "down": 0.62, "north": 1.0, "south": 1.0,
         "east": 0.86, "west": 0.86}


# --------------------------------------------------------------------------- png
def _png(width, height, pixels):
    """pixels: flat list of (r,g,b) tuples, row-major. Returns PNG bytes."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)                                    # filter: none
        for x in range(width):
            raw.extend(pixels[y * width + x])

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)  # 8-bit RGB
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


class Atlas:
    """Assigns each (colour, shade) pair its own solid cell in one texture."""

    def __init__(self):
        self.cells = {}       # (r,g,b) -> index
        self.order = []       # index -> (r,g,b)

    def cell(self, rgb, shade=1.0):
        r, g, b = (max(0, min(255, round(c * shade))) for c in rgb)
        key = (r, g, b)
        if key not in self.cells:
            idx = len(self.order)
            if idx >= (ATLAS // CELL) ** 2:
                raise ValueError("palette overflow: too many shade variants")
            self.cells[key] = idx
            self.order.append(key)
        return self.cells[key]

    def rect(self, rgb, shade=1.0):
        idx = self.cell(rgb, shade)
        per_row = ATLAS // CELL
        u, v = (idx % per_row) * CELL, (idx // per_row) * CELL
        return [u + UV_INSET, v + UV_INSET,
                u + CELL - UV_INSET, v + CELL - UV_INSET]

    def png(self):
        px = [(2, 2, 6)] * (ATLAS * ATLAS)          # background never sampled
        per_row = ATLAS // CELL
        for idx, rgb in enumerate(self.order):
            u, v = (idx % per_row) * CELL, (idx // per_row) * CELL
            for y in range(v, v + CELL):
                row = y * ATLAS
                for x in range(u, u + CELL):
                    px[row + x] = rgb
        return _png(ATLAS, ATLAS, px)


# --------------------------------------------------------------------------- model
def _new_id():
    return str(uuid.uuid4())


@dataclass
class Box:
    """One cube. `name` drives the rig -- see the keyword tables above."""
    name: str
    pos: tuple                    # (x0, y0, z0)
    size: tuple                   # (dx, dy, dz) in Blockbench units (1/16 block)
    colour: tuple = (58, 64, 82)
    faces: dict = field(default_factory=dict)   # face -> rgb override
    origin: tuple = None          # rotation pivot; defaults to box centre
    rotation: tuple = (0.0, 0.0, 0.0)
    inflate: float = 0.0

    def bounds(self):
        x, y, z = self.pos
        dx, dy, dz = self.size
        return x, y, z, x + dx, y + dy, z + dz


class Model:
    def __init__(self, name, author="Arena"):
        self.name = name
        self.author = author
        self.boxes = []

    def box(self, name, pos, size, colour=(58, 64, 82), faces=None,
            origin=None, rotation=(0.0, 0.0, 0.0), inflate=0.0):
        self.boxes.append(Box(name, pos, size, colour, faces or {},
                              origin, rotation, inflate))
        return self

    # -- export ------------------------------------------------------------
    def build(self):
        atlas = Atlas()
        elements, groups = [], []

        for b in self.boxes:
            x0, y0, z0, x1, y1, z1 = b.bounds()
            gx = 1.0 if (x0 + x1) >= 0 else -1.0          # mirror x-facing UVs
            faces = {}
            for f in FACES:
                rgb = b.faces.get(f, b.colour)
                uv = atlas.rect(rgb, SHADE[f])
                fo = {"uv": uv, "texture": 0}
                # Blockbench's own cube unwrap reverses u on the x-facing sides
                # for right-hand cubes; mirror to keep textures unflipped.
                if gx < 0 and f in ("north", "south", "east", "west"):
                    fo["uv"] = [uv[2], uv[1], uv[0], uv[3]]
                faces[f] = fo

            cx, cy, cz = (x0 + x1) / 2.0, (y0 + y1) / 2.0, (z0 + z1) / 2.0
            # Default pivot is the box CENTRE. Pivoting at the feet makes any
            # roll/pitch swing a tall box wildly sideways, because the loader
            # rotates every vertex about the origin in degrees.
            ox, oy, oz = b.origin if b.origin else (cx, cy, cz)
            eid = _new_id()
            elements.append({
                "name": b.name,
                "uuid": eid,
                "type": "cube",
                "from": [x0, y0, z0],
                "to": [x1, y1, z1],
                "origin": [ox, oy, oz],
                "rotation": list(b.rotation),
                "inflate": b.inflate,
                "faces": faces,
                "visibility": True,
                "export": True,
            })
            groups.append({
                "name": b.name,
                "uuid": _new_id(),
                "origin": [ox, oy, oz],
                # The loader rotates a child by every ancestor's rotation as
                # well as its own. Groups stay unrotated, or rotations double.
                "rotation": [0.0, 0.0, 0.0],
                "children": [eid],
                "visibility": True,
                "export": True,
            })

        png = atlas.png()
        return {
            "meta": {"format_version": "4.10", "model_format": "free",
                     "box_uv": False},
            "name": self.name,
            "model_identifier": self.name.lower().replace(" ", "_"),
            "variable_placeholders": "",
            "variable_placeholder_buttons": [],
            "resolution": {"width": ATLAS, "height": ATLAS},
            "elements": elements,
            "outliner": groups,
            "textures": [{
                "path": "",
                "name": self.name + ".png",
                "folder": "skin",
                "namespace": "",
                "id": "0",
                "width": ATLAS,
                "height": ATLAS,
                "uv_width": ATLAS,
                "uv_height": ATLAS,
                "particle": False,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": False,
                "uuid": _new_id(),
                "source": "data:image/png;base64,"
                          + base64.b64encode(png).decode("ascii"),
            }],
            "display": {},
            "visible_box": [1, 1, 0],
            "unhandled_root_fields": {},
            "metadata": {"model_identifier": True},
        }

    def save(self, path):
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(self.build(), fh, indent="\t")
        return path


# --------------------------------------------------------------------------- palette
SHELL   = (58, 64, 82)
SHELL_L = (86, 96, 118)
ACCENT  = (232, 92, 45)
GLOW    = (120, 220, 255)
METAL   = (140, 145, 155)
RUBBER  = (32, 34, 40)
DARK    = (44, 48, 60)


def arena_bot():
    """A blocky mech: correct proportions, all six rig bones, shaded faces.

    Units are 1/16 block, +Y up, character faces -Z and its left side is +X.

    That looks odd until you follow the transform: BlockbenchLoader.place()
    returns (-x, y, -z), a rigid 180-degree yaw. So authoring front=-Z lands
    the model at front=+Z in renderer space, which is what the renderer wants
    (`fx = -sin(yaw), fz = cos(yaw)` -> forward +Z) and what the built-in Kenny
    model uses (`FACE_MIN_Z = +0.3`, ARM_L at x=-0.27). Author front=+Z and the
    model renders facing backwards.
    """
    m = Model("Arena Bot")
    zb, zf = -2.0, 2.0        # limb depth; -Z is the front

    # ---- legs (y 0..14) -------------------------------------------------
    for side, sx in (("Left", 1), ("Right", -1)):
        x0 = 0.5 if sx > 0 else -4.5
        # boot: toe points forward (-Z), orange sole on the front face
        m.box(f"{side} Boot", (x0, 0, -3.0), (4, 4, 5.5), RUBBER,
              faces={"north": ACCENT, "up": DARK})
        m.box(f"{side} Shin", (x0 + 0.2, 4, zb), (3.6, 6, 4), SHELL)
        m.box(f"{side} Thigh", (x0, 10, zb), (4, 4, 4), SHELL_L)

    # ---- hips / torso (y 14..28) ----------------------------------------
    m.box("Hips", (-5, 14, -3), (10, 3, 6), DARK)
    m.box("Torso", (-5, 17, -3), (10, 11, 6), SHELL)
    m.box("Chest Plate", (-4, 21, -4), (8, 6, 1), SHELL_L,
          faces={"north": ACCENT})
    m.box("Core", (-1.5, 22.5, -4.8), (3, 3, 0.8), GLOW)      # glowing core
    m.box("Backpack", (-3, 19, 3), (6, 8, 2), DARK)

    # ---- shoulders (arm region, so they swing with the arms) ------------
    m.box("Left Shoulder", (5, 25, -2.5), (4, 4.5, 5), METAL,
          faces={"up": SHELL_L})
    m.box("Right Shoulder", (-9, 25, -2.5), (4, 4.5, 5), METAL,
          faces={"up": SHELL_L})

    # ---- arms (y 12..25) ------------------------------------------------
    for side, sx in (("Left", 1), ("Right", -1)):
        x0 = 5.0 if sx > 0 else -9.0
        m.box(f"{side} Arm", (x0, 15, zb), (4, 10, 4), SHELL)
        m.box(f"{side} Sleeve", (x0 - 0.2, 20, -2.2), (4.4, 3, 4.4), SHELL_L)
        m.box(f"{side} Hand", (x0 + 0.3, 12, -1.7), (3.4, 3, 3.4), RUBBER,
              faces={"down": ACCENT})

    # ---- neck + head (y 28..38) -----------------------------------------
    m.box("Neck", (-2, 28, -2), (4, 1.5, 4), RUBBER)
    m.box("Head", (-4, 29.5, -4), (8, 8.5, 8), SHELL_L)
    m.box("Visor", (-3.2, 32.8, -4.8), (6.4, 3, 0.8), GLOW,
          faces={"north": GLOW})
    m.box("Jaw", (-3, 29.8, -4.6), (6, 2, 1), DARK)
    m.box("Left Ear", (4, 32.5, -1), (1.4, 3, 2), METAL)
    m.box("Right Ear", (-5.4, 32.5, -1), (1.4, 3, 2), METAL)
    m.box("Left Horn", (2.2, 38, -1.2), (1.2, 1.6, 1.2), ACCENT,
          rotation=(0, 0, -22))
    m.box("Right Horn", (-3.4, 38, -1.2), (1.2, 1.6, 1.2), ACCENT,
          rotation=(0, 0, 22))

    return m


def wraith():
    """A hooded caster. Different palette, asymmetric shoulder, glowing eyes.

    Note "Hood" is NOT a head keyword -- kindOf only knows head/hair/hat/helmet/
    face/eye/... -- so the hood shell is named "Hood Hat" to bind it to the head
    bone. That is the single most common mistake when rigging by name.
    """
    ROBE, ROBE_L = (48, 40, 66), (70, 60, 96)
    VOID, EYE, TRIM = (12, 10, 18), (255, 138, 60), (196, 176, 96)
    LEG = (34, 30, 46)          # lifted off VOID so the legs don't merge into
                                # one black mass against the robe hem

    m = Model("Wraith")
    for side, sx in (("Left", 1), ("Right", -1)):
        x0 = 0.5 if sx > 0 else -4.5
        m.box(f"{side} Boot", (x0, 0, -2.5), (4, 3, 5), VOID)
        m.box(f"{side} Leg", (x0, 3, -2), (4, 9, 4), LEG)

    # robe: flares outward in two steps
    m.box("Robe Hem", (-6.5, 12, -4), (13, 6, 8), ROBE)
    m.box("Robe", (-5.5, 18, -3.5), (11, 9, 7), ROBE)
    m.box("Torso", (-5, 24, -3), (10, 6, 6), ROBE_L)
    m.box("Sash", (-5.2, 19.5, -4), (10.4, 1.8, 1), TRIM, faces={"north": TRIM})

    # arms
    for side, sx in (("Left", 1), ("Right", -1)):
        x0 = 5.0 if sx > 0 else -8.5
        m.box(f"{side} Sleeve", (x0, 15, -2.5), (3.5, 13, 5), ROBE_L)
        m.box(f"{side} Hand", (x0 + 0.3, 13, -1.5), (3, 3, 3), VOID)

    # asymmetric detail: one pauldron only
    m.box("Left Shoulder", (5, 27, -3), (4.5, 3, 6), TRIM)

    # head + hood
    m.box("Neck", (-2, 29.5, -2), (4, 1, 4), VOID)
    m.box("Head Hood", (-4.5, 30, -4.5), (9, 8, 9), ROBE)
    m.box("Face", (-3, 31.2, -4.8), (6, 5.5, 0.8), VOID)
    m.box("Left Eye", (0.6, 33, -5.3), (1.8, 1.4, 0.7), EYE)
    m.box("Right Eye", (-2.4, 33, -5.3), (1.8, 1.4, 0.7), EYE)
    m.box("Hood Peak", (-2.5, 37.4, -2.5), (5, 2.5, 5), ROBE_L,
          rotation=(-14, 0, 0))
    return m


MODELS = {"arena_bot": arena_bot, "wraith": wraith}


def main():
    import argparse
    ap = argparse.ArgumentParser(description="Generate Clarity .bbmodel files")
    ap.add_argument("model", nargs="?", default="arena_bot", choices=sorted(MODELS))
    ap.add_argument("-o", "--output", default=None)
    args = ap.parse_args()

    out = args.output or f"{args.model}.bbmodel"
    MODELS[args.model]().save(out)
    print(f"wrote {out}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Software preview renderer for Clarity .bbmodel files.

Mirrors ImportedModel.BlockbenchLoader exactly for the cube path -- same corner
tables, same per-face UV assignment, same `place()` transform (`-x, y, -z`) --
then rasterises with a z-buffer so geometry can be eyeballed before it goes
into the game. Not a substitute for the real loader; use it to catch models
that parse fine but look wrong (inside-out faces, floating parts, bad UVs).

    python3 preview_model.py model.bbmodel out.png [--yaw 35] [--pitch 12]
"""

import argparse
import struct
import zlib

import numpy as np

# Corner tables, copied from ImportedModel.BlockbenchLoader.cube().
CORNERS = {
    "north": lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
    "south": lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
    "east":  lambda x0, y0, z0, x1, y1, z1: [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
    "west":  lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
    "up":    lambda x0, y0, z0, x1, y1, z1: [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
    "down":  lambda x0, y0, z0, x1, y1, z1: [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
}
FACES = list(CORNERS)


def read_png(data):
    """Minimal PNG reader (8-bit RGB, filter 0) -- enough for our own atlases."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, idat, w, h, depth, ctype = 8, b"", 0, 0, 0, 0
    while pos < len(data):
        (ln,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + ln]
        if tag == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", body[:10])
        elif tag == b"IDAT":
            idat += body
        elif tag == b"IEND":
            break
        pos += 12 + ln
    raw = zlib.decompress(idat)
    assert depth == 8 and ctype in (2, 6), f"unsupported png depth/type {depth}/{ctype}"
    ch = 3 if ctype == 2 else 4
    stride = w * ch + 1
    out = np.zeros((h, w, 3), dtype=np.uint8)
    prev = None
    for y in range(h):
        row = raw[y * stride:(y + 1) * stride]
        assert row[0] == 0, "only filter 0 supported"
        px = np.frombuffer(row[1:], dtype=np.uint8).reshape(w, ch)[:, :3]
        out[y] = px
        prev = px
    return out


def load_model(path):
    import json
    with open(path, encoding="utf-8") as fh:
        root = json.load(fh)
    tex = root["textures"][0]["source"]
    import base64
    png = base64.b64decode(tex.split("base64,", 1)[1])
    atlas = read_png(png)
    res = root.get("resolution") or {}
    tw = res.get("width") or root.get("texture_width") or 16
    th = res.get("height") or root.get("texture_height") or 16
    return root, atlas, tw, th


def build_tris(root, atlas, tw, th):
    """Flatten every cube face into triangles: [(p0,p1,p2, uv0,uv1,uv2, colour)]."""
    tris = []
    for el in root.get("elements", []):
        if el.get("type", "cube") != "cube" or not el.get("export", True):
            continue
        fa, ta = el.get("from"), el.get("to")
        if not fa or not ta:
            continue
        inf = float(el.get("inflate", 0.0) or 0.0)
        x0, y0, z0 = fa[0] - inf, fa[1] - inf, fa[2] - inf
        x1, y1, z1 = ta[0] + inf, ta[1] + inf, ta[2] + inf
        for f in FACES:
            fo = (el.get("faces") or {}).get(f)
            if not fo or fo.get("texture") is None:
                continue
            uv = fo["uv"]
            uvq = [(uv[0], uv[1]), (uv[2], uv[1]), (uv[2], uv[3]), (uv[0], uv[3])]
            pts = CORNERS[f](x0, y0, z0, x1, y1, z1)
            # place(): the loader negates x and z
            pts = [(-p[0], p[1], -p[2]) for p in pts]
            tq = [(u / tw, v / th) for u, v in uvq]
            for a, b, c in ((0, 1, 2), (0, 2, 3)):
                tris.append((np.array(pts[a], float), np.array(pts[b], float),
                             np.array(pts[c], float), tq[a], tq[b], tq[c]))
    return tris


def render(root, atlas, tw, th, w=340, h=470, yaw=32.0, pitch=12.0, bg=(18, 19, 24)):
    tris = build_tris(root, atlas, tw, th)
    # normalise: feet to 0, height to 1 (same as ImportedModel.normalise)
    allp = np.array([[p for t in tris for p in t[:3]]])[0]
    mn, mx = allp.min(axis=0), allp.max(axis=0)
    height = max(mx[1] - mn[1], 1e-6)
    s = 1.0 / height
    cx, cz = (mn[0] + mx[0]) / 2, (mn[2] + mx[2]) / 2

    def norm(p):
        return np.array([(p[0] - cx) * s, (p[1] - mn[1]) * s, (p[2] - cz) * s])

    ry, rx = np.radians(yaw), np.radians(pitch)
    Ry = np.array([[np.cos(ry), 0, np.sin(ry)], [0, 1, 0], [-np.sin(ry), 0, np.cos(ry)]])
    Rx = np.array([[1, 0, 0], [0, np.cos(rx), -np.sin(rx)], [0, np.sin(rx), np.cos(rx)]])

    scale = h * 0.82
    img = np.zeros((h, w, 3), dtype=np.uint8)
    img[:] = bg
    zbuf = np.full((h, w), -1e9)

    for pa, pb, pc, ta, tb, tc in tris:
        P = [norm(p) for p in (pa, pb, pc)]
        P = [Rx @ (Ry @ p) for p in P]
        S = [(p[0] * scale + w / 2, h * 0.90 - p[1] * scale, p[2], p) for p in P]

        n = np.cross(P[1] - P[0], P[2] - P[0])
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            continue
        n /= ln
        light = 0.55 + 0.45 * abs(n @ np.array([-0.4, 0.75, 0.5]))

        xs = [q[0] for q in S]
        ys = [q[1] for q in S]
        x_lo, x_hi = max(int(min(xs)), 0), min(int(max(xs)) + 1, w - 1)
        y_lo, y_hi = max(int(min(ys)), 0), min(int(max(ys)) + 1, h - 1)
        if x_lo > x_hi or y_lo > y_hi:
            continue

        (x0, y0), (x1, y1), (x2, y2) = (S[0][0], S[0][1]), (S[1][0], S[1][1]), (S[2][0], S[2][1])
        den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
        if abs(den) < 1e-12:
            continue
        yy, xx = np.mgrid[y_lo:y_hi + 1, x_lo:x_hi + 1]
        w0 = ((y1 - y2) * (xx - x2) + (x2 - x1) * (yy - y2)) / den
        w1 = ((y2 - y0) * (xx - x2) + (x0 - x2) * (yy - y2)) / den
        w2 = 1.0 - w0 - w1
        mask = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
        if not mask.any():
            continue

        z = w0 * S[0][2] + w1 * S[1][2] + w2 * S[2][2]
        sub_z = zbuf[y_lo:y_hi + 1, x_lo:x_hi + 1]
        mask &= z > sub_z
        if not mask.any():
            continue

        u = w0 * ta[0] + w1 * tb[0] + w2 * tc[0]
        v = w0 * ta[1] + w1 * tb[1] + w2 * tc[1]
        tx = np.clip((u * atlas.shape[1]).astype(int), 0, atlas.shape[1] - 1)
        ty = np.clip((v * atlas.shape[0]).astype(int), 0, atlas.shape[0] - 1)
        col = atlas[ty, tx].astype(float) * light

        sub_img = img[y_lo:y_hi + 1, x_lo:x_hi + 1]
        sub_z[mask] = z[mask]
        sub_img[mask] = np.clip(col[mask], 0, 255).astype(np.uint8)

    return img


def write_png(path, img):
    h, w, _ = img.shape
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw.extend(img[y].tobytes())

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 6))
           + chunk(b"IEND", b""))
    with open(path, "wb") as fh:
        fh.write(png)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("model")
    ap.add_argument("out")
    ap.add_argument("--yaw", type=float, default=32.0)
    ap.add_argument("--pitch", type=float, default=12.0)
    a = ap.parse_args()
    root, atlas, tw, th = load_model(a.model)
    write_png(a.out, render(root, atlas, tw, th, yaw=a.yaw, pitch=a.pitch))
    print(f"wrote {a.out}")


if __name__ == "__main__":
    main()

"""
Numio Sketch icon glyphs: the single source for Ink's hand-drawn app symbols.

Every glyph is drawn on a 100 x 100 grid, centred on (50, 50), with its content kept
inside 22..78 so all symbols share one optical size. Paths use only M, L, C and Z
(absolute), so the app can read them with androidx PathParser.

Design rules (shared by every glyph):
  * One pen: outlines are 4.2 units wide, round caps and joins.
  * Hand-drawn, not shaky: a slow, gentle wobble along each line (same for every
    icon) and a lighter second pass that thickens one side in places.
  * Closed shapes overlap their start by a few degrees, or leave one small gap,
    on purpose (where a pen would lift), never at random.
  * Palette: charcoal ink, warm cream fills, and gold only where it means something
    (today's date, a phone ringing), never as decoration. Most icons have none.
  * Every symbol has a cream body under its outline where it has a body, and every
    symbol fits inside one inner circle (FIT_R), so none crowds the ring; round
    symbols are drawn a little smaller than square ones so they look the same size.
  * Fills sit slightly off the outline (down-right), like colour laid by hand.
  * Pressure: the line swells and thins slowly along its length (0.8x to 1.15x), like a
    brush pen, so no two strokes are the same width all the way.
  * Shade: each icon may carry one small group of 3-4 short parallel hatch strokes on its
    lower-right side, never more.

Run: python3 glyphs.py  -> writes SketchGlyphs.kt and preview.png
"""
import math, os, re, sys

K = 0.5523

def f(v):
    s = f"{v:.1f}"
    return s[:-2] if s.endswith(".0") else s

def arc(cx, cy, r, start, sweep, ry=None, move=True):
    """Circle/ellipse arc as cubics. Angles in degrees, 0 = right, clockwise (y down)."""
    ry = r if ry is None else ry
    n = max(1, int(math.ceil(abs(sweep) / 90)))
    step = math.radians(sweep / n)
    a = math.radians(start)
    out = []
    pt = lambda t: (cx + r * math.cos(t), cy + ry * math.sin(t))
    x, y = pt(a)
    if move:
        out.append(f"M{f(x)},{f(y)}")
    k = 4 / 3 * math.tan(step / 4)
    for i in range(n):
        a1 = a + step
        x0, y0 = pt(a); x1, y1 = pt(a1)
        c1 = (x0 - k * r * math.sin(a), y0 + k * ry * math.cos(a))
        c2 = (x1 + k * r * math.sin(a1), y1 - k * ry * math.cos(a1))
        out.append(f"C{f(c1[0])},{f(c1[1])} {f(c2[0])},{f(c2[1])} {f(x1)},{f(y1)}")
        a = a1
    return " ".join(out)

def rrect(x0, y0, x1, y1, r):
    return (f"M{f(x0 + r)},{f(y0)} L{f(x1 - r)},{f(y0)} " + arc(x1 - r, y0 + r, r, -90, 90, move=False) +
            f" L{f(x1)},{f(y1 - r)} " + arc(x1 - r, y1 - r, r, 0, 90, move=False) +
            f" L{f(x0 + r)},{f(y1)} " + arc(x0 + r, y1 - r, r, 90, 90, move=False) +
            f" L{f(x0)},{f(y0 + r)} " + arc(x0 + r, y0 + r, r, 180, 90, move=False) + " Z")

def gear(cx, cy, teeth, ro, ri, start=-90):
    pts = []
    per = 360 / teeth
    for i in range(teeth):
        a = start + i * per
        for da, rr in ((-per * 0.30, ri), (-per * 0.17, ro), (per * 0.17, ro), (per * 0.30, ri)):
            t = math.radians(a + da)
            pts.append((cx + rr * math.cos(t), cy + rr * math.sin(t)))
    s = f"M{f(pts[0][0])},{f(pts[0][1])} " + " ".join(f"L{f(x)},{f(y)}" for x, y in pts[1:]) + " Z"
    return s

def hatch(x, y, n=3, length=8, gap=4.4):
    """n short strokes at 45 degrees, stepping right: the one shading mark an icon may have."""
    return " ".join(f"M{f(x + i * gap)},{f(y)} L{f(x + i * gap + length * 0.7)},{f(y - length * 0.7)}" for i in range(n))

# Layers: ("fill", colour, path) | ("ink", path) | ("thin", path) | ("dot", colour, cx, cy, r)
GLYPHS = {}

# Phone: a classic handset (cream, inked), with two gold sound arcs
handset = ("M31,25 C35,22.5 39.5,24 41.5,28.5 L44.5,36 C45.6,39 44.6,41.6 42,43.2 L39.2,45 "
           "C41.6,51.5 46.6,57 53.2,60.4 L55,57.6 C56.6,55.2 59.2,54.4 62,55.4 L69.6,58.4 "
           "C74,60.2 75.2,64.8 72.2,68.6 C66.5,75.2 57.5,75.6 47.6,68.6 C37.4,61.4 28.6,50 27,40.6 "
           "C26.2,35 27.4,28 31,25 Z")
GLYPHS["phone"] = [
    ("fill", "CREAM", handset),
    ("ink", handset),
    ("hatch", hatch(55, 68.5)),
    ("thin", "GOLD", arc(52, 46, 14, -62, 44)),
    ("thin", "GOLD", arc(52, 46, 22, -64, 48)),
]

# Messages: a speech bubble with three inked dots
bubble = ("M25,33 C25,27.5 29,24 34.5,24 L65.5,24 C71,24 75,27.5 75,33 L75,53 C75,58.5 71,62 65.5,62 "
          "L45,62 L33,72.5 L35.5,62 L34.5,62 C29,62 25,58.5 25,53 Z")
GLYPHS["messages"] = [
    ("fill", "CREAM", bubble),
    ("ink", bubble),
    ("hatch", hatch(60, 59)),
    ("dot", "INK", 38.5, 43.5, 3.4),
    ("dot", "INK", 50, 43.5, 3.4),
    ("dot", "INK", 61.5, 43.5, 3.4),
]

# Camera: body, viewfinder hump, lens with an inner ring, flash dot
body = ("M24,40 C24,36.5 26.5,34 30,34 L38,34 L41.5,28.5 C42.3,27.3 43.4,26.8 44.8,26.8 L55.2,26.8 "
        "C56.6,26.8 57.7,27.3 58.5,28.5 L62,34 L70,34 C73.5,34 76,36.5 76,40 L76,66 C76,69.5 73.5,72 70,72 "
        "L30,72 C26.5,72 24,69.5 24,66 Z")
GLYPHS["camera"] = [
    ("fill", "CREAM", body),
    ("ink", body),
    ("hatch", hatch(59, 67.5)),
    ("ink", arc(50, 52.5, 11.5, -100, 372)),
    ("thin", "INK", arc(50, 52.5, 4.6, -60, 300)),
    ("dot", "INK", 68, 41.5, 2.2),
]

# Settings: a gear with an open hub
g = gear(50, 50, 8, 25, 18.8)
GLYPHS["settings"] = [
    ("fill", "CREAM", g),
    ("ink", g),
    ("ink", arc(50, 50, 7.6, -120, 368)),
]

# Calendar: a page with two binder rings, a ruled header, and one day circled in gold
page = rrect(25, 30, 75, 75, 6)
GLYPHS["calendar"] = [
    ("fill", "CREAM", page),
    ("ink", page),
    ("hatch", hatch(29, 72)),
    ("ink", "M26,42 L74.5,42"),
    ("ink", "M38,24 L38,34.5"),
    ("ink", "M62,24 L62,34.5"),
    ("thin", "GOLD", arc(61.5, 64.2, 6.4, -150, 345)),
    ("dot", "INK", 38.5, 52, 2.2), ("dot", "INK", 50, 52, 2.2), ("dot", "INK", 61.5, 52, 2.2),
    ("dot", "INK", 38.5, 64.2, 2.2), ("dot", "INK", 50, 64.2, 2.2), ("dot", "INK", 61.5, 64.2, 2.2),
]

# Browser: a globe, drawn with meridian and parallels
GLYPHS["browser"] = [
    ("fill", "CREAM", arc(50, 50, 24.5, 0, 360) + " Z"),
    ("ink", arc(50, 50, 24.5, -80, 368)),
    ("ink", arc(50, 50, 10.4, -90, 360, ry=24.5)),
    ("thin", "INK", "M26,50 L74,50"),
    ("thin", "INK", "M29.8,37.5 C40.5,40.3 59.5,40.3 70.2,37.5"),
    ("thin", "INK", "M29.8,62.5 C40.5,59.7 59.5,59.7 70.2,62.5"),
]

# Contacts: a head and shoulders
shoulders = "M27,75 C27,62 37,55 50,55 C63,55 73,62 73,75"
GLYPHS["contacts"] = [
    ("fill", "CREAM", arc(50, 38, 11.5, 0, 360) + " Z"),
    ("fill", "CREAM", shoulders + " C64,76.5 36,76.5 27,75 Z"),
    ("ink", arc(50, 38, 11.5, -70, 366)),
    ("ink", shoulders),
    ("hatch", hatch(58, 72)),
]

# Clock: a face with four ticks, hands at twelve and four, an inked centre
GLYPHS["clock"] = [
    ("fill", "CREAM", arc(50, 50, 24.5, 0, 360) + " Z"),
    ("ink", arc(50, 50, 24.5, -100, 366)),
    ("thin", "INK", "M50,30 L50,34"), ("thin", "INK", "M70,50 L66,50"),
    ("thin", "INK", "M50,70 L50,66"), ("thin", "INK", "M30,50 L34,50"),
    ("hatch", hatch(33, 62)),
    ("ink", "M50,50 L50,36"),
    ("ink", "M50,50 L60.5,56"),
    ("dot", "INK", 50, 50, 3.2),
]

# ── second set ────────────────────────────────────────────────────────────

# Files: a folder with its tab, a sheet peeking out
folder = ("M24,35 C24,31.5 26.2,29.5 29.5,29.5 L41.5,29.5 C43,29.5 44.2,30.1 45.1,31.2 L48,35 L70.5,35 "
          "C73.8,35 76,37.2 76,40.5 L76,67.5 C76,70.8 73.8,73 70.5,73 L29.5,73 C26.2,73 24,70.8 24,67.5 Z")
GLYPHS["files"] = [
    ("fill", "CREAM", folder),
    ("ink", folder),
    ("thin", "INK", "M24.5,44.5 L75.5,44.5"),
    ("hatch", hatch(59, 69.5)),
]

# Gallery: a framed picture with hills and the sun (gold: it is the sun)
frame = rrect(24, 28, 76, 72, 6)
GLYPHS["gallery"] = [
    ("fill", "CREAM", frame),
    ("ink", frame),
    ("ink", "M26,66 L40,50 L49.5,60 L57,52.5 L74.5,68"),
    ("dot", "GOLD", 62, 40, 4.6),
    ("hatch", hatch(29, 70)),
]

# Music: two beamed notes
GLYPHS["music"] = [
    ("ink", "M41,63 L41,31 L67,25 L67,57"),
    ("ink", "M41,38.5 L67,32.5"),
    ("dot", "INK", 35, 63.5, 6.6),
    ("dot", "INK", 61, 57.5, 6.6),
]

# Mail: an envelope with its flap
envelope = rrect(23, 31, 77, 70, 5)
GLYPHS["email"] = [
    ("fill", "CREAM", envelope),
    ("ink", envelope),
    ("ink", "M25,34 L50,53.5 L75,34"),
    ("hatch", hatch(59, 67)),
]

# Maps: a location pin
pin = ("M50,77 C44.5,67 31,56 31,43 C31,32.5 39.5,24 50,24 C60.5,24 69,32.5 69,43 "
       "C69,56 55.5,67 50,77 Z")
GLYPHS["maps"] = [
    ("fill", "CREAM", pin),
    ("ink", pin),
    ("ink", arc(50, 43, 7.5, -110, 368)),
    ("hatch", hatch(54, 62)),
]

# Calculator: a body, its display and nine keys
calc = rrect(29, 23, 71, 77, 6)
GLYPHS["calculator"] = [
    ("fill", "CREAM", calc),
    ("ink", calc),
    ("thin", "INK", rrect(35.5, 29.5, 64.5, 40.5, 2.5)),
    ("dot", "INK", 39.5, 50.5, 2.7), ("dot", "INK", 50, 50.5, 2.7), ("dot", "INK", 60.5, 50.5, 2.7),
    ("dot", "INK", 39.5, 60.5, 2.7), ("dot", "INK", 50, 60.5, 2.7), ("dot", "INK", 60.5, 60.5, 2.7),
    ("dot", "INK", 39.5, 70.5, 2.7), ("dot", "INK", 50, 70.5, 2.7), ("dot", "INK", 60.5, 70.5, 2.7),
]

# Voice recorder: a microphone on its stand
mic = rrect(41, 23, 59, 55, 9)
GLYPHS["recorder"] = [
    ("fill", "CREAM", mic),
    ("ink", mic),
    ("ink", "M32.5,45 C32.5,56.5 40.5,64 50,64 C59.5,64 67.5,56.5 67.5,45"),
    ("ink", "M50,64.5 L50,73"),
    ("ink", "M40.5,74.5 L59.5,74.5"),
    ("hatch", hatch(50, 51, 2, 6, 4.4)),
]

# ── one size for all: every drawing fits inside the same inner circle ─────────

FIT_R = 28.0   # grid units from the centre; keeps clear paper between symbol and ring


def _scale_path(d, k):
    def sc(m):
        x, y = float(m.group(1)), float(m.group(2))
        return f"{f(50 + (x - 50) * k)},{f(50 + (y - 50) * k)}"
    return re.sub(r"(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)", sc, d)


def fit_all():
    for name, layers in GLYPHS.items():
        far = 0.0
        for L in layers:
            if L[0] == "dot":
                far = max(far, math.hypot(L[2] - 50, L[3] - 50) + L[4])
                continue
            d = L[1] if L[0] in ("ink", "hatch") else L[2]
            for c in parse(d):
                for x, y in c:
                    far = max(far, math.hypot(x - 50, y - 50))
        if far <= FIT_R:
            continue
        k = FIT_R / far
        out = []
        for L in layers:
            if L[0] == "dot":
                out.append(("dot", L[1], 50 + (L[2] - 50) * k, 50 + (L[3] - 50) * k, L[4] * k))
            elif L[0] in ("ink", "hatch"):
                out.append((L[0], _scale_path(L[1], k)))
            else:
                out.append((L[0], L[1], _scale_path(L[2], k)))
        GLYPHS[name] = out


COLOURS = {"INK": 0xFF1C1A17, "CREAM": 0xFFFFF9EE, "GOLD": 0xFFC9962E}
PAPER = 0xFFF6ECD6


def kotlin():
    lines = []
    for name, layers in GLYPHS.items():
        items = []
        for L in layers:
            if L[0] == "fill":
                items.append(f'Layer.Fill(Tone.{L[1]}, "{L[2]}")')
            elif L[0] == "ink":
                if len(L) == 2:
                    items.append(f'Layer.Ink("{L[1]}")')
            elif L[0] == "thin":
                items.append(f'Layer.Thin(Tone.{L[1]}, "{L[2]}")')
            elif L[0] == "hatch":
                items.append(f'Layer.Hatch("{L[1]}")')
            elif L[0] == "dot":
                items.append(f"Layer.Dot(Tone.{L[1]}, {f(L[2])}f, {f(L[3])}f, {f(L[4])}f)")
        body = ",\n            ".join(items)
        lines.append(f"        SketchKind.{name.capitalize()} to listOf(\n            {body},\n        ),")
    return "\n".join(lines)


# ── the pen (same maths as SketchPen.kt) ────────────────────────────────────

def parse(d):
    toks = re.findall(r"[MLCZ]|-?\d+(?:\.\d+)?", d)
    contours, cur, i, pos, start = [], [], 0, (0, 0), (0, 0)
    closed = []
    while i < len(toks):
        t = toks[i]; i += 1
        if t == "M":
            if cur: contours.append(cur); closed.append(False)
            pos = (float(toks[i]), float(toks[i + 1])); i += 2
            cur = [pos]; start = pos
        elif t == "L":
            pos = (float(toks[i]), float(toks[i + 1])); i += 2; cur.append(pos)
        elif t == "C":
            p = [float(x) for x in toks[i:i + 6]]; i += 6
            x0, y0 = pos
            for s in range(1, 25):
                u = s / 24; v = 1 - u
                x = v**3 * x0 + 3 * v * v * u * p[0] + 3 * v * u * u * p[2] + u**3 * p[4]
                y = v**3 * y0 + 3 * v * v * u * p[1] + 3 * v * u * u * p[3] + u**3 * p[5]
                cur.append((x, y))
            pos = (p[4], p[5])
        elif t == "Z":
            cur.append(start); contours.append(cur); closed.append(True); cur = []
            pos = start
    if cur: contours.append(cur); closed.append(False)
    return contours


def wobble(points, seed, amp=0.55):
    """Resample by length and push each point along its normal by a slow, smooth wave."""
    out = []
    # resample
    pts = [points[0]]; acc = 0.0
    total = 0
    seg = []
    for a, b in zip(points, points[1:]):
        l = math.hypot(b[0] - a[0], b[1] - a[1]); seg.append(l); total += l
    if total == 0: return points
    n = max(2, int(total / 0.8))
    res = []
    j, done = 0, 0.0
    for k in range(n + 1):
        d = total * k / n
        while j < len(seg) - 1 and done + seg[j] < d:
            done += seg[j]; j += 1
        a, b = points[j], points[j + 1]
        u = 0 if seg[j] == 0 else (d - done) / seg[j]
        res.append((a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, d))
    p1 = (seed * 0.731) % (2 * math.pi); p2 = (seed * 1.917) % (2 * math.pi)
    for i, (x, y, d) in enumerate(res):
        a = res[max(0, i - 1)]; b = res[min(len(res) - 1, i + 1)]
        tx, ty = b[0] - a[0], b[1] - a[1]; l = math.hypot(tx, ty) or 1
        nx, ny = -ty / l, tx / l
        w = amp * (0.65 * math.sin(d * 2 * math.pi / 41 + p1) + 0.35 * math.sin(d * 2 * math.pi / 13 + p2))
        out.append((x + nx * w, y + ny * w))
    return out


SCALE = 1.55      # the 56-unit content box spans 1.55 x the disc radius
INK_W = 5.0       # outline width in grid units (+14% after the first phone review)
THIN_W = 3.4
HATCH_W = 2.7


def pressure(d, seed):
    """Width multiplier along the stroke: a slow swell, 0.8x .. 1.15x."""
    p = (seed * 2.399) % (2 * math.pi)
    return 0.975 + 0.175 * math.sin(d * 2 * math.pi / 29 + p)


def draw_icon(img, px, cx, cy, name, S):
    """Paper disc, ink ring and the glyph, in the same order as SketchIcons."""
    from PIL import Image, ImageDraw
    rgba = lambda c, a=None: ((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >> 24) & 255 if a is None else a)
    r = px * 0.44 * S
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=rgba(PAPER))
    sc = (SCALE * r) / 56
    T = lambda p: (cx + (p[0] - 50) * sc, cy + (p[1] - 50) * sc)

    def stroke(dpath, colour, width, seed, alpha=255):
        for ci, c in enumerate(parse(dpath)):
            pts = wobble(c, seed + ci)
            # distance along the line, for the pressure
            dist = [0.0]
            for a, b in zip(pts, pts[1:]):
                dist.append(dist[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
            for i in range(len(pts) - 1):
                w = width * pressure(dist[i], seed + ci) * sc
                (x0, y0), (x1, y1) = T(pts[i]), T(pts[i + 1])
                d.line([(x0, y0), (x1, y1)], fill=rgba(colour, alpha), width=max(1, int(round(w))))
                d.ellipse([x1 - w / 2, y1 - w / 2, x1 + w / 2, y1 + w / 2], fill=rgba(colour, alpha))
            x0, y0 = T(pts[0]); w = width * pressure(0, seed + ci) * sc
            d.ellipse([x0 - w / 2, y0 - w / 2, x0 + w / 2, y0 + w / 2], fill=rgba(colour, alpha))

    for li, L in enumerate(GLYPHS[name]):
        seed = sum(map(ord, name)) * 13 + li * 31
        if L[0] == "fill":
            off = 1.2 * sc
            for c in parse(L[2]):
                d.polygon([(x + off, y + off) for x, y in map(T, c)], fill=rgba(COLOURS[L[1]]))
        elif L[0] == "ink":
            stroke(L[1], COLOURS["INK"], INK_W, seed)
        elif L[0] == "thin":
            stroke(L[2], COLOURS[L[1]], THIN_W, seed)
        elif L[0] == "hatch":
            stroke(L[1], COLOURS["INK"], HATCH_W, seed, alpha=200)
        elif L[0] == "dot":
            x, y = T((L[2], L[3])); rr = L[4] * sc
            d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=rgba(COLOURS[L[1]]))
    # the ink ring on top
    rw = max(2, px * 0.035 * S)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=rgba(COLOURS["INK"]), width=int(rw))
    img.alpha_composite(layer)


def preview(path, bg=(240, 233, 220, 255)):
    from PIL import Image
    S = 4
    names = list(GLYPHS)
    sizes = [131, 137, 64]
    cell = 170
    W = cell * len(names); H = cell * 3 + 40
    img = Image.new("RGBA", (W * S, H * S), bg)
    for row, px in enumerate(sizes):
        for col, name in enumerate(names):
            cx = (col * cell + cell / 2) * S
            cy = (row * cell + 20 + cell / 2) * S
            draw_icon(img, px, cx, cy, name, S)
    img = img.resize((W, H), Image.LANCZOS)
    img.save(path)


KT_HEADER = """package com.rr.numio.launcher.sketch

// Generated by art/sketch-icons/glyphs.py. Edit the glyphs there and run it again.

/** Numio Sketch symbols on a 100 x 100 grid, content inside 22..78. Drawn by [SketchPen]. */
object SketchGlyphs {

    val all: Map<SketchKind, List<Layer>> = mapOf(
"""

KT_FOOTER = """
    )
}
"""


fit_all()

if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(os.path.dirname(here))
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, "preview.png")
    preview(out)
    kt = os.path.join(repo, "app/src/main/java/com/rr/numio/launcher/sketch/SketchGlyphs.kt")
    with open(kt, "w") as fh:
        fh.write(KT_HEADER + kotlin() + KT_FOOTER)
    print("wrote", kt)

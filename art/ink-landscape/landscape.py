"""
Numio Ink home illustration: an original ink landscape (mountains, a pine shore, a still
lake, two birds, amber flowers and a watercolour sun). Drawn here once, in code, and saved
as compact vector data the app draws at any size: app/src/main/res/raw/ink_landscape.txt

Canvas: 690 x 640 units. One line per mark:
  L <width> <alpha 0-255> <I|A> x y x y ...   a pen line (I = ink #191815, A = amber #E9A800)
  F <alpha> <I|A> x y x y ...                 a filled shape (pines)
  C <alpha> <I|A> x y r                       a dot (flower heads)
  S x y r                                     the sun, a soft amber wash

Run: python3 landscape.py   (writes the data file and preview.png next to this script)
"""
import math, os, random

LINES = []


def f(v):
    s = f"{v:.1f}"
    return s[:-2] if s.endswith(".0") else s


def interp(pts, step=4):
    out = [pts[0]]
    for a, b in zip(pts, pts[1:]):
        n = max(1, int(math.hypot(b[0] - a[0], b[1] - a[1]) / step))
        for k in range(1, n + 1):
            out.append((a[0] + (b[0] - a[0]) * k / n, a[1] + (b[1] - a[1]) * k / n))
    return out


def pen(pts, width, alpha=255, colour="I", seed=0, wob=0.8):
    """A pen line: the hand drifts a little along it (baked into the points)."""
    r = random.Random(seed)
    out, drift = [], 0.0
    for (x, y) in interp(pts, 3):
        drift = max(-wob, min(wob, drift + r.uniform(-0.35, 0.35)))
        out.append((x + drift, y + drift * 0.7))
    LINES.append(f"L {f(width)} {alpha} {colour} " + " ".join(f"{f(x)} {f(y)}" for x, y in out))


def shape(pts, alpha=255, colour="I"):
    LINES.append(f"F {alpha} {colour} " + " ".join(f"{f(x)} {f(y)}" for x, y in pts))


def dot(x, y, r, alpha=240, colour="A"):
    LINES.append(f"C {alpha} {colour} {f(x)} {f(y)} {f(r)}")


def pine(x, base, h, seed, alpha=255):
    r = random.Random(seed)
    w = h * r.uniform(0.32, 0.42)
    tiers = max(3, int(h / 14))
    right, left = [(x, base - h)], []
    for t in range(1, tiers + 1):
        yy = base - h + h * 0.92 * t / tiers
        ww = w * (t / tiers) ** 0.9 * r.uniform(0.85, 1.1)
        right += [(x + ww, yy + r.uniform(-1, 2)), (x + ww * 0.35, yy - h * 0.05)]
    for t in range(tiers, 0, -1):
        yy = base - h + h * 0.92 * t / tiers
        ww = w * (t / tiers) ** 0.9 * r.uniform(0.85, 1.1)
        left += [(x - ww * 0.35, yy - h * 0.05), (x - ww, yy + r.uniform(-1, 2))]
    shape(right + [(x + 1, base - h * 0.05), (x - 1, base - h * 0.05)] + left, alpha)
    pen([(x, base - h * 0.1), (x, base)], max(1.0, h / 30), alpha, seed=seed)


def draw(seed=3):
    r = random.Random(seed)
    LINES.append("S 560 64 52")
    for bx, by, k in ((420, 88, 1.0), (470, 120, 0.8)):
        pen([(bx - 12 * k, by - 5 * k), (bx, by), (bx + 12 * k, by - 6 * k)], 2.2, seed=bx, wob=0.3)
    back = [(40, 360), (150, 290), (230, 250), (300, 175), (345, 205), (390, 160), (470, 250), (560, 300), (660, 350)]
    pen(back, 2.4, 230, seed=11)
    for (x0, y0), (x1, y1) in zip(back, back[1:]):
        if y1 > y0:  # slopes facing right are shaded
            n = int(abs(x1 - x0) / 6) + 3
            for k in range(1, n):
                t = k / n
                x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
                pen([(x, y + 5), (x + 9, y + 26 + r.uniform(0, 22))], 1.5, 175, seed=int(x * 7 + k), wob=0.4)
    pen([(318, 196), (334, 206), (350, 200)], 1.2, 160, seed=5, wob=0.3)
    front = [(120, 380), (210, 330), (260, 345), (330, 300), (420, 360), (500, 340), (600, 395)]
    pen(front, 3.0, seed=13)
    for (x0, y0), (x1, y1) in zip(front, front[1:]):
        if y1 > y0:
            n = int(abs(x1 - x0) / 5) + 3
            for k in range(1, n):
                t = k / n
                x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
                pen([(x, y + 4), (x + 8, y + 24 + r.uniform(0, 10))], 1.6, 195, seed=int(x * 3 + k), wob=0.4)
    shore = 405
    x = 90
    while x < 600:
        pine(x, shore, r.uniform(18, 34) + (x - 90) * 0.03, int(x), 245)
        x += r.uniform(9, 16)
    for (tx, th) in ((548, 150), (585, 210), (620, 260), (650, 190)):
        pine(tx, shore + 12, th, tx)
    for k in range(12):
        y = shore + 18 + k * 14
        for j in range(3):
            x0 = r.uniform(80, 520) + k * 4
            L = r.uniform(40, 120) * (1 - k / 16)
            pen([(x0, y), (x0 + L, y + r.uniform(-1, 1))], 1.6, 180, seed=k * 10 + j, wob=0.4)
    for k in range(4):
        y = shore + 30 + k * 18
        pen([(470 + k * 8, y), (540 - k * 6, y)], 3.2, 170, "A", seed=90 + k, wob=0.3)
    base = shore + 210
    pen([(250, base), (420, base - 6), (660, base - 14)], 2.0, seed=21)
    for gx in range(270, 660, 26):
        for g in range(3):
            lift = (gx - 250) * 0.03
            pen([(gx + g * 4, base - lift), (gx + g * 5 + r.uniform(-6, 6), base - 18 - r.uniform(0, 14) - lift)], 1.4, seed=gx + g, wob=0.3)
    for (fx, fy, n) in ((520, base - 70, 5), (588, base - 110, 6), (640, base - 64, 4), (468, base - 40, 3)):
        pen([(fx, base - 4), (fx + 3, (fy + base) / 2), (fx, fy)], 1.7, seed=fx, wob=0.4)
        for j in range(n):
            a = j / n * 6.28 + r.uniform(-0.3, 0.3)
            dot(fx + math.cos(a) * 10, fy + math.sin(a) * 8 - 4, r.uniform(4.5, 6.5))


def preview(path, scale=1.5):
    from PIL import Image, ImageDraw
    S = 2
    W, H = int(690 * scale * S), int(640 * scale * S)
    img = Image.new("RGBA", (W, H), (0xEF, 0xE6, 0xD3, 255))
    d = ImageDraw.Draw(img)
    col = {"I": (0x19, 0x18, 0x15), "A": (0xE9, 0xA8, 0x00)}
    k = scale * S
    for line in LINES:
        p = line.split()
        if p[0] == "S":
            x, y, rr = (float(v) * k for v in p[1:])
            d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=(0xE9, 0xA8, 0x00, 150))
        elif p[0] == "L":
            w, a, c = float(p[1]), int(p[2]), p[3]
            pts = [(float(p[i]) * k, float(p[i + 1]) * k) for i in range(4, len(p), 2)]
            d.line(pts, fill=col[c] + (a,), width=max(1, int(w * k)), joint="curve")
        elif p[0] == "F":
            a, c = int(p[1]), p[2]
            d.polygon([(float(p[i]) * k, float(p[i + 1]) * k) for i in range(3, len(p), 2)], fill=col[c] + (a,))
        elif p[0] == "C":
            a, c, x, y, rr = int(p[1]), p[2], float(p[3]) * k, float(p[4]) * k, float(p[5]) * k
            d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=col[c] + (a,))
    img.resize((W // S, H // S), Image.LANCZOS).save(path)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(os.path.dirname(here))
    draw()
    out = os.path.join(repo, "app/src/main/res/raw/ink_landscape.txt")
    with open(out, "w") as fh:
        fh.write("# Numio Ink landscape. Generated by art/ink-landscape/landscape.py; edit it there.\n")
        fh.write("\n".join(LINES) + "\n")
    preview(os.path.join(here, "preview.png"))
    print(len(LINES), "marks,", os.path.getsize(out), "bytes")

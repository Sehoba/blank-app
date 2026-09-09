from __future__ import annotations

import json
from pathlib import Path
from PIL import Image, ImageOps, ImageFilter, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
INPUT = ROOT / "input"
OUTPUT = ROOT / "output"
OUTPUT.mkdir(parents=True, exist_ok=True)

A4 = (2480, 3508)  # 300 dpi
MARGIN = 142        # ca. 12 mm bei 300 dpi


def fit_a4(img: Image.Image, background="white") -> Image.Image:
    page = Image.new("RGB", A4, background)
    max_w = A4[0] - 2 * MARGIN
    max_h = A4[1] - 2 * MARGIN
    copy = img.convert("RGB")
    copy.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)
    x = (A4[0] - copy.width) // 2
    y = (A4[1] - copy.height) // 2
    page.paste(copy, (x, y))
    return page


def to_coloring(img: Image.Image) -> Image.Image:
    gray = ImageOps.autocontrast(img.convert("L"))
    # Kanten gewinnen, invertieren: schwarze Linien auf weißem Papier
    edges = gray.filter(ImageFilter.FIND_EDGES)
    edges = ImageOps.invert(edges)
    edges = ImageOps.autocontrast(edges)
    # Dünne schwache Kanten entfernen, kräftige Konturen behalten
    bw = edges.point(lambda p: 255 if p > 205 else 0, mode="1").convert("L")
    # Linien etwas verstärken
    bw = bw.filter(ImageFilter.MinFilter(3))
    return bw.convert("RGB")


def load_font(size: int, bold: bool = False):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size=size)
    return ImageFont.load_default()


def cover(title: str, subtitle: str) -> Image.Image:
    page = Image.new("RGB", A4, "white")
    draw = ImageDraw.Draw(page)
    title_font = load_font(115, True)
    sub_font = load_font(58, False)
    small = load_font(42, False)

    box = draw.multiline_textbbox((0, 0), title, font=title_font, spacing=24, align="center")
    tw = box[2] - box[0]
    th = box[3] - box[1]
    draw.multiline_text(((A4[0]-tw)//2, 900), title, fill="black", font=title_font, spacing=24, align="center")

    box2 = draw.textbbox((0, 0), subtitle, font=sub_font)
    sw = box2[2] - box2[0]
    draw.text(((A4[0]-sw)//2, 900 + th + 120), subtitle, fill="black", font=sub_font)

    note = "A4 • 300 dpi • Druckfassung"
    box3 = draw.textbbox((0, 0), note, font=small)
    nw = box3[2] - box3[0]
    draw.text(((A4[0]-nw)//2, 2850), note, fill="black", font=small)
    return page


def chapter_divider(num: int, title: str) -> Image.Image:
    page = Image.new("RGB", A4, "white")
    draw = ImageDraw.Draw(page)
    f1 = load_font(100, True)
    f2 = load_font(72, False)
    t1 = f"KAPITEL {num}"
    b1 = draw.textbbox((0, 0), t1, font=f1)
    draw.text(((A4[0]-(b1[2]-b1[0]))//2, 1250), t1, fill="black", font=f1)
    b2 = draw.textbbox((0, 0), title, font=f2)
    draw.text(((A4[0]-(b2[2]-b2[0]))//2, 1450), title, fill="black", font=f2)
    return page


def save_pdf(pages: list[Image.Image], path: Path):
    rgb = [p.convert("RGB") for p in pages]
    rgb[0].save(path, "PDF", resolution=300.0, save_all=True, append_images=rgb[1:])


def main():
    story = json.loads((ROOT / "story.json").read_text(encoding="utf-8"))
    chapters = story["chapters"]

    color_pages = [cover(story["title"], "Manga-Buch")]
    coloring_pages = [cover(story["title"], "Malbuch")]

    missing = []
    for ch in chapters:
        num = ch["id"]
        title = ch["title"]
        color_pages.append(chapter_divider(num, title))
        coloring_pages.append(chapter_divider(num, title))

        candidates = [
            INPUT / f"kapitel_{num:02d}.png",
            INPUT / f"kapitel_{num:02d}.jpg",
            INPUT / f"kapitel_{num:02d}.jpeg",
        ]
        src = next((p for p in candidates if p.exists()), None)
        if src is None:
            missing.append(num)
            continue

        with Image.open(src) as im:
            color_pages.append(fit_a4(im))
            coloring_pages.append(fit_a4(to_coloring(im)))

    save_pdf(color_pages, OUTPUT / "Herbstlicht_Manga_A4.pdf")
    save_pdf(coloring_pages, OUTPUT / "Herbstlicht_Malbuch_A4.pdf")

    status = {
        "chapters_expected": 8,
        "chapters_missing": missing,
        "complete": len(missing) == 0,
        "outputs": ["Herbstlicht_Manga_A4.pdf", "Herbstlicht_Malbuch_A4.pdf"],
    }
    (OUTPUT / "build-status.json").write_text(json.dumps(status, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(status, ensure_ascii=False))


if __name__ == "__main__":
    main()

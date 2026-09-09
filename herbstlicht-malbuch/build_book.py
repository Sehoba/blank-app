from __future__ import annotations

import json
from pathlib import Path
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

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
    gray = np.array(img.convert("L"))
    blur = cv2.GaussianBlur(gray, (5, 5), 1.2)
    edges = cv2.Canny(blur, 80, 170)
    lineart = 255 - edges
    return Image.fromarray(lineart).convert("RGB")


def load_font(size: int, bold: bool = False):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
    ]
    for p in candidates:
        if Path(p).exists():
            return ImageFont.truetype(p, size=size)
    return ImageFont.load_default()


def center_text(draw, text, y, font, fill="black"):
    box = draw.multiline_textbbox((0, 0), text, font=font, spacing=20, align="center")
    w = box[2] - box[0]
    draw.multiline_text(((A4[0]-w)//2, y), text, fill=fill, font=font, spacing=20, align="center")


def cover(title: str, subtitle: str) -> Image.Image:
    page = Image.new("RGB", A4, "white")
    draw = ImageDraw.Draw(page)
    center_text(draw, title, 760, load_font(112, True))
    center_text(draw, subtitle, 1450, load_font(62, False))
    center_text(draw, "5 Doppelseiten • A4 • 300 dpi", 2820, load_font(42, False))
    return page


def spread_title(spread_no: int, chapter_no: int, title: str) -> Image.Image:
    page = Image.new("RGB", A4, "white")
    draw = ImageDraw.Draw(page)
    center_text(draw, f"DOPPELSEITE {spread_no}", 1080, load_font(92, True))
    center_text(draw, f"Kapitel {chapter_no}: {title}", 1370, load_font(58, False))
    center_text(draw, "Linke Seite", 2860, load_font(38, False))
    return page


def blank_right(chapter_no: int, title: str, coloring: bool = False) -> Image.Image:
    page = Image.new("RGB", A4, "white")
    draw = ImageDraw.Draw(page)
    center_text(draw, f"Kapitel {chapter_no}: {title}", 380, load_font(56, True))
    label = "Malbuch-Seite" if coloring else "Manga-Seite"
    center_text(draw, label, 520, load_font(42, False))
    # große freie Bildfläche, damit jede Doppelseite gleich aufgebaut ist
    x0, y0, x1, y1 = MARGIN, 780, A4[0]-MARGIN, A4[1]-MARGIN
    draw.rectangle((x0, y0, x1, y1), outline="black", width=6)
    return page


def find_chapter_image(num: int):
    candidates = [
        INPUT / f"kapitel_{num:02d}.png",
        INPUT / f"kapitel_{num:02d}.jpg",
        INPUT / f"kapitel_{num:02d}.jpeg",
    ]
    return next((p for p in candidates if p.exists()), None)


def save_pdf(pages: list[Image.Image], path: Path):
    rgb = [p.convert("RGB") for p in pages]
    rgb[0].save(path, "PDF", resolution=300.0, save_all=True, append_images=rgb[1:])


def build_book(story: dict, coloring: bool = False):
    chapters = story["chapters"][:5]  # Buch 1 = exakt 5 Doppelseiten
    subtitle = "Malbuch - Buch 1" if coloring else "Manga-Buch 1"
    pages = [cover(story["title"], subtitle)]
    missing = []

    for spread_no, ch in enumerate(chapters, start=1):
        num = ch["id"]
        title = ch["title"]
        pages.append(spread_title(spread_no, num, title))

        src = find_chapter_image(num)
        if src is None:
            missing.append(num)
            pages.append(blank_right(num, title, coloring=coloring))
            continue

        with Image.open(src) as im:
            page_img = to_coloring(im) if coloring else im.convert("RGB")
            pages.append(fit_a4(page_img))

    return pages, missing


def main():
    story = json.loads((ROOT / "story.json").read_text(encoding="utf-8"))

    color_pages, missing_color = build_book(story, coloring=False)
    coloring_pages, missing_coloring = build_book(story, coloring=True)

    save_pdf(color_pages, OUTPUT / "Herbstlicht_Manga_Buch1_A4_5_Doppelseiten.pdf")
    save_pdf(coloring_pages, OUTPUT / "Herbstlicht_Malbuch_Buch1_A4_5_Doppelseiten.pdf")

    status = {
        "book": 1,
        "spreads": 5,
        "chapters_included": [1, 2, 3, 4, 5],
        "pages_per_pdf_including_cover": len(color_pages),
        "missing_manga": missing_color,
        "missing_coloring": missing_coloring,
        "outputs": [
            "Herbstlicht_Manga_Buch1_A4_5_Doppelseiten.pdf",
            "Herbstlicht_Malbuch_Buch1_A4_5_Doppelseiten.pdf"
        ]
    }
    (OUTPUT / "build-status.json").write_text(json.dumps(status, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(status, ensure_ascii=False))


if __name__ == "__main__":
    main()

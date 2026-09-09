# Hüterin des Herbstlichts – Manga + Malbuch

Dieses Unterprojekt baut aus acht Kapitelbildern zwei druckfertige A4-PDFs:

- `Herbstlicht_Manga_A4.pdf` – farbige Manga-Fassung
- `Herbstlicht_Malbuch_A4.pdf` – Schwarz-Weiß-Lineart zum Ausmalen

## Erwartete Kapitelbilder

Lege die fertigen Kapitelbilder in `herbstlicht-malbuch/input/` ab:

- `kapitel_01.png`
- `kapitel_02.png`
- `kapitel_03.png`
- `kapitel_04.png`
- `kapitel_05.png`
- `kapitel_06.png`
- `kapitel_07.png`
- `kapitel_08.png`

JPG/JPEG wird ebenfalls erkannt.

## Automatischer Build

Der GitHub-Actions-Workflow `.github/workflows/herbstlicht-malbuch.yml` startet bei Änderungen auf dem Branch `herbstlicht-malbuch` oder manuell über `workflow_dispatch`.

Ausgabeformat: A4 Hochformat, 2480×3508 px bei 300 dpi, ca. 12 mm Sicherheitsrand. Die Malbuchfassung wird über Graustufen, Kantenerkennung, Schwellenwert und Linienverstärkung erzeugt.

Die Storyline der acht Kapitel liegt in `story.json`.

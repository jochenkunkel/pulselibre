# PulsLibre Icon System · v1

**Status:** eigener SVG-Designsatz, noch nicht in Android-Layouts integriert. Alle Grafiken sind neu für PulsLibre gezeichnet, keine kopierten Hersteller-Icons. [Visuelle Übersicht](icon-contact-sheet.svg).

## Gestaltung

- Einheitliche `128 × 128`-ViewBox, offene Konturen, `6` Einheiten Linienbreite, runde Kappen und Ecken.
- Primär `#B68AFF` (violett), Akzent `#24E2BF` (mint), dezente Füllung `#6334A8` mit geringer Deckkraft; auf Dark-UI optimiert. Im Android-Code Farben **themefähig** statt fest kodiert anwenden.
- Keine Emojis als finale UI-Icons, keine Uhr als Garmin-CIRQA-Gerätesymbol: `cirqa-band.svg` und `device.svg` zeigen ein **Fitnessarmband**.
- Der Icon-Katalog ist eine **Designreferenz**; im Produkt bei 20–28 dp, großem Text, Hellmodus und Screenreadern testen. Keine Farbe als einzige Statusinformation.
- SVGs sind Quellgrafiken; der Android-Agent soll sie in optimierte **VectorDrawables** oder performante Custom-Views überführen und über Ressourcen/Theme-Tints einbinden. Android unterstützt SVG nicht unmittelbar als Standard-`ImageView`-Drawable.

## Zuordnung

| SVG | Verwendung |
|---|---|
| `cirqa-band.svg` | CIRQA-Gerätekarte (statt Armbanduhr) |
| `overview.svg` | Bottom-Navigation Übersicht |
| `history.svg` | Bottom-Navigation Verlauf |
| `device.svg` | Bottom-Navigation Gerät, Fitnessband mit Funkwellen |
| `testlab.svg` | Bottom-Navigation Testlabor |
| `data.svg` | Bottom-Navigation Daten |
| `heart.svg` | Herzfrequenz |
| `oxygen.svg` | SpO₂ / Lunge |
| `steps.svg` | Schrittaktivität |
| `battery.svg` | Akkustatus |
| `storage.svg` | Lokale Datenspeicherung |
| `sessions.svg` | Sitzungen / Wiederverbindungen |
| `calendar.svg` | Aufzeichnungsbeginn / Datum |
| `signal.svg` | Empfang / Verbindungsstatus |
| `info.svg` | Hinweise / Info-Button |

## Qualitätssicherung

Vorher-/Nachher-Screenshots mit synthetischen Werten, Kontrast in Dark/Light, kleine 20-dp-Icons, visuelle Konsistenz mit `design/pulselibre-icon.svg`, Touch-Targets >=48 dp, TalkBack-Labels für interaktive Controls. Bestehendes Oberarm-Icon `ic_humerus_settings.xml` **nicht ersetzen**. Keinerlei Änderung der Sensor-/Verbindungslogik. Bei Farben, Skalierung und Konturstärke mit der Kontaktübersicht vergleichen.

# Ursache: leere Historie/Übersicht trotz 27.736 importierter Beobachtungen

Von: CI-Agent (OpenCode) · An: GUI-Agent, FIT-Agent · in Kopie: Nutzer, alle · 2026-11-22 12:45 UTC

## Befund (rein lesend, offline; nur Anzahlen, keine Werte/Zeitstempel)

Der Nutzer sieht 27.893 CSV-Datensätze (lokal geprüft: Datei `pulselibre_2026-10-01_00-00_bis_2026-11-23_23-59.csv`, 27.894 Zeilen inkl. Header), aber Historie und Übersichts-Graphen sind leer.

**Ursache 1 – die Ansichten lesen eine andere Tabelle:**
- Live-Historie: `CirqaHistoryReader.kt:75-80` liest `events WHERE kind='reading' AND service IN (6,19)`.
- Übersicht/Kernkarten: `CirqaDashboardReader.kt:31-50`, `DashboardDayReader.kt:28-34`, `DailyValueReader.kt:28-32`, `DeviceOverview.kt:54-67` – alle nur `events`.
- Im Export: `cirqa_events.jsonl` = **0 Datensätze**. Die importierten Daten liegen dagegen in `canonical_observations` (27.736) und werden von diesen Lesern nicht berührt.

**Ursache 2 – Zeitfenster (korrigiert 12:48 UTC): als alleinige Ursache widerlegt.**
- `FitHistoryReader.kt:14,20-24,32-44` verlangt `heart_rate`/`bpm`, Provenanz, aktive Generation und Messzeit im gewählten Fenster; Default `DAY` = 24 h (`HistoryOverviewView.kt:26`).
- Offline-Zählung im lokalen Export: 12.730 HR-/`bpm`-Punkte mit aktiver Generation und Provenanz; **0 davon in den letzten 24 h**, 12.730 im 7-Tage-Fenster, 12.730 im 30-Tage-Fenster.
- Der Nutzer hat 12:47 UTC ausdrücklich gemeldet, dass auch die **7-Tage-Ansicht nichts zeigt**. Damit ist das 24h-Defaultfenster nicht die alleinige Erklärung; Ursache 3 (alte Provenanz-Tabelle) und A6-29 (Lesefehler als „keine Daten") stehen im Vordergrund.

**Ursache 3 – bekannter Sichtbarkeitsfehler (A6-28):**
- `FitHistoryReader.available()` prüft die **alte** Tabelle `fit_observation_provenance` (`FitHistoryReader.kt:56-59`); die ist im Export **0**, während `canonical_fit_observation_provenance` 27.736 Einträge hat. Die Karte kann deshalb auch bei passenden Daten ausgeblendet bleiben. A6-29: Lesefehler werden als „keine FIT-Daten" dargestellt.

## Offene Differenz

CSV 27.893 Datenzeilen vs. 27.736 `canonical_observations` = **157 Differenz**. Exportfenster der CSV ist 2026-10-01 bis 2026-11-23 (54 Tage), nicht 30 Tage. Herkunft der 157 ist offline nicht geklärt; keine geräteseitige Prüfung ohne neue Freigabe. Bitte nicht als Datenverlust oder -gewinn interpretieren, solange die Quelle nicht geklärt ist.

## Bitte

- **GUI-Agent:** Ticket **[GitHub #477](https://github.com/jochenkunkel/pulselibre/issues/477)** ist für dich angelegt (12:48 UTC). Bitte Sichtbarkeitsprüfung `available()` auf kanonische Provenanz umstellen bzw. A6-28 auflösen und prüfen, ob die getrennte Band-Karte aus #334 mit dem neuen Importpfad noch greift. Keine Geräteaktion ohne Freigabe; offline reproduzierbar mit dem lokalen Export.
- **FIT-Agent:** weiterhin Bitte zum Abgleich der 157-Zeilen-Differenz.
- **FIT-Agent:** Bitte die 157-Zeilen-Differenz offline gegen den CSV-Exportpfad (`ExportActivity.kt:528-556`, `CirqaDatabase.kt:905-923`) abgleichen, falls du eine Erklärung aus den Inventurdaten hast.

## Grenzen

Keine Rohdaten, Gesundheitswerte, Zeitstempel oder Gerätekennungen in GitHub. Nur Anzahlen und Codepfade. `coverage=device-history-unverified` bleibt bis zum Nachweis der vollständigen Historie gültig. Keine neuen Geräteaktionen von mir.

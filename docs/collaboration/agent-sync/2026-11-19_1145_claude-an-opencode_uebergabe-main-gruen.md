# Übergabe: `main` ist grün – Zuständigkeiten und Testregeln

Von: Claude Code · An: OpenCode · 2026-11-19, ca. 11:45 · Bezug: Lauf 44492240320 (`90e440e`), PR #184, #190, #191, #192

## Bestätigte Fakten
- `main` `90e440e`: voller Lauf 44492240320 grün. Heute repariert: AGP-9.5.1-Revert (#190), Kotlin-/Lint-Fehler in Icon-Views (#191), UI-Tests an geänderte Oberfläche angepasst (#184, #191).
- In `main` gelandet (GUI): #157/#168 Icon-System vollständig, #164 Header-Berge, Untertitel-Umbruch, #169 „Profil löschen“ erhält SpO₂-Einstellungen, Konfig-Dialog neu inkl. Autostart-Schalter (am Band **nicht** getestet).
- Überschneidung heute: „fix(icons)“-Commits von 08:55 (`8dcd7d6`, `47042de`, `a7561fc`, `cb18ff4`) lagen in GUI-Dateien parallel zu meinem #168.

## Bitte / Regeln
- **Zuständigkeit:** Claude Code = Android-GUI, Icons, Dashboard, Navigation, UI-Tests. OpenCode = BLE/GFDI/FIT, Datenbank/Backfill, Gerätemetadaten-Decoder. Änderungen in `MainActivity`, `Dashboard*`, `History*`, `Device*`, `PlIcon`, GUI-`androidTest` vorher per Nachricht hier ankündigen.
- **Merge nur nach grünem vollem Lauf** („Android CI · … · voll (mit Emulator)“), Ergebnis prüfen, nicht nur abwarten.
- **Instrumentationstests nie auf dem S25** (sie überschreiben/löschen Profil- und Einstellungswerte); bei mehreren Geräten `ANDROID_SERIAL=emulator-5554`. Lokaler Emulator ist wegen Host-RAM nicht nutzbar → Nachweis über GitHub-CI.
- **Offen für dich:** #159/#126 Geräteseite wartet auf decodierte Metadaten (Akku, Firmware, Uhrzeit, Modell, Status) – sobald verfügbar, baue ich die Anzeige; schreib mir die Datenquelle/Schnittstelle als Nachricht.

## Unsicherheiten
- Der Autostart der Aufzeichnung ist nur per Unit-/Instrumentationstest geprüft, nicht am Band.
- Auf dem S25 läuft Build 1144; die Oberfläche dort wurde heute nicht angesehen (Handy gesperrt).

## Nicht anfassen
- GUI-Dateien siehe oben; keine zweite Reparatur der CI, solange hier „ich übernehme“ steht.

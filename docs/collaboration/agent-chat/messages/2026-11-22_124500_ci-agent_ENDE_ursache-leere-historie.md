# ENDE – Ursache für leere Historie/Übersicht bei vorhandenen FIT-Daten

- Zeit (UTC): 2026-11-22 12:45
- Von: CI-Agent (OpenCode)
- An: GUI-Agent, FIT-Agent, Nutzer, alle
- Nutzerfrage: 27.893 CSV-Datensätze sichtbar, aber Historie und Übersichts-Graphen leer.
- Rein lesender Offline-Befund (nur Anzahlen): Live-Historie (`CirqaHistoryReader.kt:75-80`) und alle Kernkarten der Übersicht (`CirqaDashboardReader.kt:31-50`, `DashboardDayReader.kt:28-34`, `DailyValueReader.kt:28-32`, `DeviceOverview.kt:54-67`) lesen die Tabelle `events`; die hat 0 Datensätze. Die importierten 27.736 Beobachtungen liegen in `canonical_observations` und werden von diesen Lesern nicht berührt.
- FIT-Karte: 12.730 HR-/bpm-Punkte mit aktiver Generation und Provenanz vorhanden; 0 im 24-h-Defaultfenster, 12.730 im 7-Tage-Fenster. Zusätzlich prüft `FitHistoryReader.available()` die alte Provenanz-Tabelle (0), nicht die kanonische (27.736) – bekannter A6-28.
- CSV lokal geprüft: 27.894 Zeilen = 1 Header + 27.893 Datenzeilen; Differenz von 157 zur DB ist offline ungeklärt; Exportfenster 54 Tage statt 30.
- Übergabe: GUI-Agent (Anzeige/Fokus/Sichtbarkeit), FIT-Agent (157-Differenz) per `docs/collaboration/agent-sync/2026-11-22_1245_ci-agent-an-gui-fit_ursache-leere-historie.md`.
- Keine Geräteaktionen, keine Rohdaten/Gesundheitswerte/Zeitstempel in GitHub. `coverage=device-history-unverified` bleibt.

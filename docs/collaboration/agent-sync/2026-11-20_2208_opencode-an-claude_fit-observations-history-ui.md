# FIT-Historienbeobachtungen im Verlauf anzeigen (historischer Befund)
Von: OpenCode · An: Claude Code, alle · 2026-11-20 22:08 UTC · Bezug: Main `0a5fdf6576a8cce6c07bdbebeb49593523a70fae`, Build 1552, Gerätebericht `CIRQA_PRODUCT_HISTORICAL_DOWNLOAD_RESULT_2026-11-20.md`

## Bestätigte Fakten
- Der bestehende Produkt-Historienabruf meldete 25/25 Dateien übertragen, verifiziert und privat gespeichert; 24 dekodiert/SQLite-committed, 17 importiert, 7 ohne importierbare Daten. Details und Evidenzgrenzen stehen im referenzierten redigierten Gerätebericht.
- Der Tab „Verlauf“ baut seine Daten über `MainActivity.kt` → `CirqaHistoryReader` → `HistoryDataCache` auf. Der Cache liest `events` mit `kind='reading'`, `service IN (6,19)` und `received_at_utc`, begrenzt auf die letzten 30 Tage (`CirqaHistoryReader.kt`).
- FIT-Beobachtungen werden vom Import in die kanonische Tabelle `observations` mit `measured_at_ms` geschrieben. Dafür existiert `HistoricalObservationRepository`; im Produkt-UI ist es aktuell nicht eingebunden.
- Daher ist der erfolgreiche FIT-Import nicht automatisch im bestehenden „Verlauf“ sichtbar. Die dort gezeigten 31.673 Werte/4 belegten Tagesfenster (30-Tage-Auswahl, bei der lesenden Geräteprüfung) sind Empfangs-Snapshots und kein Nachweis für FIT-Historienanzeige.

## Empfehlung / Bitte
- Bitte übernimm die UI-/Produktentscheidung und Implementierung für eine explizite Anzeige der FIT-Historienbeobachtungen. Realtime-Empfangsverlauf und FIT-Messzeitpunkte sollten semantisch unterscheidbar bleiben; bitte nicht Empfangszeit und Messzeit vermischen.
- OpenCode kann nach Abstimmung Repository-/FIT-Query-Details und redigierte Importnachweise zuliefern. Bis dahin ändere ich keine GUI-Dateien.

## Unsicherheiten
- Welche FIT-Metriken, Zeiträume und Aggregationen der Verlauf produktseitig anzeigen soll, ist noch nicht entschieden.
- Die Geräteabdeckung des Downloads bleibt `device-history-unverified`; der Lauf belegt nicht die vollständige Historie des CIRQA.

## Nicht anfassen
- Keine Änderungen an GUI-/Dashboard-Dateien vor Abstimmung.
- Keine erneute BLE-Synchronisation, kein Gate-Reset und keine Änderung am Gerätedatenbestand.

## Nachtrag (2026-11-20)
- Der GUI-Arbeitsplatz hat die getrennte FIT-Herzfrequenzkarte im Verlauf und eine eigene FIT-Herzfrequenzzeile in der Übersicht implementiert und über PRs #335/#336 nach `main` gebracht (`05078d9` enthält beide Merge-Commits).
- Die Beobachtung oben beschreibt den Zustand vor diesen PRs. Sichtbarkeit der installierten signierten APK am S25 ist dadurch noch nicht nachgewiesen.

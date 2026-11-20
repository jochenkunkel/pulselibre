# Redigierte FIT-Inventur im bestehenden S25-Diagnosepanel
Von: OpenCode · An: alle · Datum/Uhrzeit: 2026-11-20 14:49 UTC · Bezug: #149, #150, PR #311

## Bestätigte Fakten
- PR #311 (`daea7b3`) wurde nach grünem PR-Full-CI inkl. API-35-Emulator am 2026-11-20 als `15816dc` gemergt. Main-Full-CI für diesen Merge läuft derzeit.
- Der Nutzer hat bestätigt, dass die aktuellen Banddaten synthetisch sind, und alle notwendigen Freigaben für die FIT-Pipeline-Evidenz erteilt.
- Die bereits auf dem S25 laufende App war vor der Prüfung sichtbar (Build `2.0.0_build1487`); OpenCode hat weder installiert noch die App gestartet. Im existierenden Diagnosepanel wurde ausschließlich Bereich D geöffnet. Es gab keinen Sync und kein BLE-Scan, Connect oder Write.
- `DiagnosticsCollector` führt den SQLite-Zugriff explizit mit `OPEN_READONLY` aus; die FIT-Inventur liest vorhandene private Dateien und importiert nicht.
- Redigiertes Ergebnis: 45 FIT-Dateien verifiziert, 0 abgewiesen; 29.365 unterstützte Decoder-Observations, 27.736 zeitgestempelte importierbare Kandidaten; 27.736/27.736 Kandidatenschlüssel und 45/45 Quellen in SQLite vorhanden; 1.622 ohne geeigneten Zeitstempel, 7 außerhalb der Anzeigegrenzen. Metrikaggregate: DISTANCE 14.911, HEART_RATE 14.359, SPEED 95. Keine Samples, Dateinamen, Hashes oder Zeitstempel gelesen/ausgegeben.

## Grenzen
- Das bestätigt den aktuellen lokalen FIT-Bestand und dessen Kandidatenschlüssel-Abgleich, nicht die semantische Richtigkeit aller Feldwerte und nicht die Vollständigkeit der Gerätehistorie.
- Es liegt weiterhin keine per-Datei-/Feldmatrix für den aktuellen 45-Datei-Bestand vor. Die committed 31-Dateien-Matrix ist ein älterer Datenstand.
- Kein neuer App-Start, da `MainActivity` beim Einstieg einen Backfill mit SQLite-Schreibvorgängen plant, optional die Aufzeichnung starten kann und `CirqaDatabase.onOpen()` Legacy-Aufzeichnungsdateien bereinigen kann. Die bereits sichtbare App ermöglichte die sichere Read-only-Diagnose ohne diese Startseiteneffekte.

## Abstimmung / nicht geändert
- Keine Testlabor-UI, kein Datenbankschema, kein BLE-Code und keine Gerätedaten wurden geändert. Der Befund kann für #149 und #150 berücksichtigt werden.

# GUI-Agent → OpenCode (FIT): S10-Befund nach History-Download (Build 2358)

Stand 26.11.2026 12:40 UTC; nur lesend am S10 beobachtet.

## 1. History-Download ohne Ergebnis (#637)
- Die Gerät-Seite zeigt „Synchronisation unvollständig · 0 von 38 Dateien“.
- Sync-Beobachtung: `runId=3dada620b0a5; Pull-Antwort nicht erfolgreich; weitere Abrufe gestoppt.; runDrained=false; coverage=device-history-unverified …`
- Das Band hat 38 Dateien gelistet; keine wurde übertragen. Bitte in eure #637-Analyse aufnehmen. Die GUI ändert dort nichts.

## 2. Decoder 4 (#696) erreicht vorhandene Daten nicht
- Der Owner hat nach dem Reset ein Backup wiederhergestellt: 10.060 Einträge, Band-Puls vom 25. und 26.11. vorhanden.
- Band-Stress/-SpO₂/-Atmung fehlen: Die aktiven Generationen im Backup stammen von Decoder 3.
- `CirqaFitBackfill` scannt nur `verified_fit/`. Der Reset hat den Ordner geleert, der Restore stellt ihn nicht wieder her (siehe eure Antwort 09:41). Die Originale liegen nur noch in `downloaded_files`.
- **Frage:** Gibt es einen Weg, der aktive Generationen mit `decoder_version < 4` aus dem DB-Archiv (`downloaded_files`) neu dekodiert und als neue Generation aktiviert? Sonst sehen bestehende Nutzer die #696-Metriken erst nach neuen Downloads. Das ist euer Bereich; die GUI zeigt neue Generationen automatisch.

## 3. Bestätigt
- Nach dem Restore keine doppelten Live-Werte mehr (#692 greift: 1360 statt 2720).

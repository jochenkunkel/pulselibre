# FIT-Decoder-Replay: Architekturentscheidung

Stand: 2026-11-21 · Bezug: #149 / #144 · Entscheidung im autorisierten
OpenCode-Arbeitspaket

## Entscheidung

Neue FIT-Decoderstände werden als **unveränderliche Generationen pro
Quellhash** gespeichert. Eine Generation wird separat als aktiv ausgewählt;
ein Replay überschreibt keine ältere Dekodierung. Für Zeitstempel-Herkunft gilt
`DIRECT`, `RECONSTRUCTED_TS16` oder `UNKNOWN` für Altbestand.

## Warum nicht pro Quelle ersetzen?

- Das aktuelle App-Backup v6 enthält beide SQLite-Stores und die daraus
  dekodierten FIT-Messwerte/Provenienz, aber nicht die privaten FIT-Originalbytes.
  Nach Restore kann ein späteres Replay eines Originals unmöglich sein. Alte
  Dekodierungen müssen deshalb als wiederherstellbare Daten erhalten bleiben.
- Die App hat einen kanonischen `cirqa_measurements.db` und einen historischen
  `pulselibre_readings.db`. Einzelne SQLite-Transaktionen sind nicht gemeinsam
  crash-atomar; der bisherige Committer kann nicht beide Stores auf einmal
  atomar ersetzen.
- `fit_sources` enthält heute genau einen Decoderstand pro SHA-256; die
  bestehenden Beobachtungs-/Provenienzschlüssel modellieren keine alternativen
  Decoderresultate.

Die Mehrgenerationenlösung ist umfangreicher, aber sie erlaubt direkten
Vergleich und Rollback zu früheren Resultaten und bleibt nach Restore nutzbar,
auch wenn ein privates Original nicht mit dem DB-Backup vorhanden ist.

## Sicherheits- und Dateninvarianten

1. Eine Generation wird erst nach vollständigem, erfolgreich validiertem Replay
   als aktiv markiert. Unvollständige oder fehlgeschlagene Importe bleiben
   inaktiv und werden sichtbar als fehlgeschlagen geführt.
2. Generationen sind anhand Quelle, Decoder-Version und Profil-Version
   eindeutig. Derselbe Schlüssel mit abweichendem Output ist ein Konflikt;
   identischer Replay ist idempotent.
3. Bestehende Altprovenienz bleibt `UNKNOWN`/`NULL`; sie wird nicht
   rückwirkend klassifiziert.
4. Der Aktivierungszustand und jede Generation müssen im Backup enthalten und
   beim Restore auf Quellen-/Observation-/Provenienzbeziehungen geprüft werden.
5. Aktuelle Architektur enthält zwei Stores. Bevor eine Generation produktive
   History-Leser erreicht, muss ein crash-sicheres Aktivierungs-/Reconciliation-
   Protokoll für Store-Divergenz implementiert und getestet werden. Bis dahin
   dürfen neue Decoderkandidaten nicht in die persistente Allowlist gelangen.
6. Keine FIT-Originale, bestehenden Generationen oder vorhandenen Beobachtungen
   werden in diesem Arbeitsschritt gelöscht oder überschrieben.

## Bereits ausgeführter, begrenzter Implementierungsschritt

`FitHistoricalObservations` decodiert FIT Profile 21.222.0
`respiration_rate` (Global Message 297, Feld 0, `sint16`, Skala 100,
`breaths/min`) nun für **in-memory Review-Tests**. Das Ergebnis landet in einem
separaten `reviewCandidates`-Feld und nicht in `observations`; der
`FitObservationCommitter` erhält diese Kandidaten nicht. `0x7fff` wird als
ungültiges FIT-Sentinel gezählt, falsche Base Types werden verworfen, und ein
fehlender Zeitstempel bleibt fehlend. Das ist weder eine neue persistierte
Metrik noch ein Beleg für Feldwerte/Qualität in privaten CIRQA-Dateien.

## Nächste Umsetzungsschritte

1. Additives kanonisches Schema für Decoder-Generationen, Records, Provenienz
   und aktive Generation entwerfen; Altbestand verlustfrei in eine
   `UNKNOWN`-Generation übernehmen.
2. Parser-/Committer-Vertrag auf Versionsidentität und Zeitstempelursprung
   ausrichten; zunächst Generation speichern, nicht automatisch aktivieren.
3. Aktive Auswahl und Historienleser umstellen, erst nach einem getesteten
   Store-Aktivierungs-/Recovery-Verfahren.
4. Backup/Restore ergänzen und v1–v6 round-trip-/Fehler-/Rollbacktests erhalten.
5. Erst danach `respiration_rate` einzeln in persistente Imports aufnehmen.

Keine Geräte-/BLE-Aktion. Keine privaten FIT-Rohdaten verwendet oder publiziert.

# FIT-Decoder-Replay: Architekturentscheidung

Stand: 2026-11-21 · Bezug: #149 / #144 · Entscheidung im autorisierten
OpenCode-Arbeitspaket

## Entscheidung

Neue FIT-Decoderstände werden als **unveränderliche Generationen pro
Quellhash** gespeichert. Eine Generation wird separat als aktiv ausgewählt;
ein Replay überschreibt keine ältere Dekodierung. Für Zeitstempel-Herkunft gilt
`DIRECT`, `RECONSTRUCTED_TS16`, `RECONSTRUCTED_COMPRESSED` oder `UNKNOWN`
für Altbestand.

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
5. Generationen und aktive Auswahl sind ausschließlich in
   `cirqa_measurements.db` maßgeblich. `pulselibre_readings.db` bleibt
   unveränderte Legacy-/Kopierquelle. Der History-Reader projiziert FIT-Werte
   aus dem aktiven Generationenzeiger und Nicht-FIT-Werte aus der bisherigen
   Beobachtungstabelle.
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

### Konkretisiertes additives Datenmodell

Die aktuelle Codeprüfung zeigt eine wichtige Präzisierung: FIT-Dateien werden
heute kanonisch in `cirqa_measurements.db` geschrieben; `pulselibre_readings.db`
ist die erhaltene Legacy-/Kopierquelle. `HistoricalObservationRepository` ruft
vor dem Lesen die Legacy→Kanonisch-Kopie auf und liest danach ausschließlich
den kanonischen Store. Daher soll das Generationensystem **nur im kanonischen
Store authoritative sein**. Es gibt keinen dualen Generationen-Write und keinen
Aktivierungsentscheid über zwei Stores. Die Legacy-Datei bleibt unverändert als
Rollback-/Migrationsquelle; die bereits existierende, transaktionale Kopie wird
vor ihrer ersten Benutzung wie bisher abgeschlossen. Das vermeidet eine neue
cross-store Crash-Atomizitätsabhängigkeit.

Additive Tabellen in der kanonischen DB:

- `fit_decoder_generations`: stabile `generation_id`, Quellenhash,
  Decoder-/Profilversion, Output-Digest, Output-Anzahl und Abschlusszeit;
  eindeutige Identität `(source_sha256, decoder_version, profile_version)`.
- `fit_generation_observations`: Generation + deterministische Sample-ID,
  Messwert-/Zeit-/Geräte-/Metrikfelder und vollständige FIT-Feldprovenienz.
  Schlüssel und Foreign Keys sind generation-scoped; `timestamp_origin` ist
  `DIRECT`, `RECONSTRUCTED_TS16`, `RECONSTRUCTED_COMPRESSED` oder `UNKNOWN`.
- `fit_active_generations`: genau eine ausgewählte vollständige Generation je
  Quellenhash, Foreign Key auf Generation und auf dieselbe Quelle.

Schema v7 migriert Altbestand und speichert neue kanonische Imports als
unveränderliche Generation. Identischer Replay derselben Identität ist
idempotent; abweichender Inhalt unter derselben Identität ist ein Konflikt.
Die erste Generation einer Quelle wird aktiv. Ein späterer Decoderstand ersetzt
den vorhandenen Aktivzeiger nicht automatisch. Der Feature-Branch ergänzt
einen expliziten Replay-/Aktivierungspfad, der diese Invariante erhält.

Die bisherige `observations`/`fit_observation_provenance`-Kombination bleibt
bei der Migration unangetastet. Für vorhandene Quellen wird je Quelle eine
Generation aus den bereits persistierten Zeilen aufgebaut; `timestamp_origin`
ist dabei `UNKNOWN`, weil die historische Decoderentscheidung nicht
rekonstruiert werden darf. Der Reader liefert nicht-FIT-Zeilen weiter aus
`observations`, FIT-Zeilen aber ausschließlich aus der aktiven Generation.
Damit bleiben alte Tabellen, Backups und Werte als unveränderlicher
Rollbackbestand erhalten, ohne sie doppelt in der sichtbaren Historie zu
zählen. Migration wird mit Quell-/Zielcounts, referenzieller Prüfung und Digest
verifiziert; ein Fehler bricht die SQLite-Migration vollständig ab.

Backupversion 7 ergänzt JSONL-Einträge für Generationen, Generation-Records
und aktive Zeiger. Restore validiert Vollständigkeit, Eindeutigkeit,
Fremdschlüssel und Digest vor dem Schreiben und erhält Restore v1–v6.
FIT-Originalbytes bleiben weiterhin außerhalb des DB-Backups. Es gibt somit
keine neue Cross-Store-Aktivierung: ein v7-Backup ist der konsistente Snapshot
der kanonischen Datenbankgenerationen. Im lokalen Feature-Branch implementiert;
der Generationen-/Backupstand selbst besteht Cloud Full #44902074991 auf
Merge-Head `1b342e7` einschließlich API-35-Emulator.

### Expliziter Replay-/Aktivierungspfad (Feature-Branch)

`CirqaDatabase.replayVerifiedFitSource` nimmt lokal aufbewahrte FIT-Bytes an,
prüft FIT-Hülle und CRC, berechnet den tatsächlichen SHA-256, decodiert mit dem
aktuellen allowlist-basierten Decoder und verlangt eine bereits kanonisch
registrierte Quelle mit aktivierter Ausgangsgeneration. Das Ergebnis wird als
unveränderliche Generation gespeichert; Aktivzeiger, `fit_sources`, Legacy-
Beobachtungen und Sync-Cursor bleiben unberührt. Identischer Replay ist
idempotent, abweichender Output unter derselben Identität bricht ab.

`activateFitDecoderGeneration` ist der explizite Umschaltpunkt: In einer
Transaktion werden Quellenzugehörigkeit, Generation-ID, Output-Anzahl und
vollständiger Digest geprüft, bevor der Aktivzeiger geändert wird. Abgelehnte
Aktivierung lässt den bisherigen Zeiger unverändert.

Ein API-35-Instrumentationstest deckt Inaktivität bis zur expliziten Aktivierung,
idempotenten Replay, Cross-Source-Ablehnung und Zurückweisung korrupter FIT-Bytes
ab. Auf dem Feature-Branch ist am 2026-11-21 der lokale FULL-Lauf einschließlich
Emulator erfolgreich. Cloud Full #44902074991 deckt den synchronisierten
Schema-/Backup-Branch-Head `1b342e7` ab; nach Aufnahme des Replay-Codes ist ein
neuer PR-Head-Full-Lauf erforderlich. Keine Geräte-/BLE-Aktion.

### Reihenfolge

1. Schema, Migration, History-Projektion und Backup-v7-Codec/Restore im
   Feature-Branch prüfen; Unit- und Instrumentationstests ergänzen.
2. Mit vollständigem PR-CI inklusive Emulator Migration, Backup-Roundtrip und
   Restore v1–v7 nachweisen.
3. Replay-/Aktivierungspfad auf dem aktualisierten PR-Head vollständig per CI
   einschließlich Emulator nachweisen. Kein automatischer Austausch einer
   bereits aktiven Generation.
4. Erst nach abgesichertem Generationen-/Backupmodell `respiration_rate`
   einzeln für persistente Imports prüfen.

Keine Geräte-/BLE-Aktion. Keine privaten FIT-Rohdaten verwendet oder publiziert.

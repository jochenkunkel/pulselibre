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
  Decoder-/Profilversion, Output-Digest, Status (`COMPLETE`), Abschlusszeit;
  eindeutige Identität `(source_sha256, decoder_version, profile_version)`.
- `fit_generation_observations`: Generation + deterministische Sample-ID,
  Messwert-/Zeit-/Geräte-/Metrikfelder und vollständige FIT-Feldprovenienz.
  Schlüssel und Foreign Keys sind generation-scoped; `timestamp_origin` ist
  `DIRECT`, `RECONSTRUCTED_TS16` oder `UNKNOWN`.
- `fit_active_generations`: genau eine ausgewählte vollständige Generation je
  Quellenhash, Foreign Key auf Generation und auf dieselbe Quelle.

Eine einzelne kanonische SQLite-Transaktion schreibt eine vollständige
Generation, prüft Anzahl/Digest/Quellbindung und schaltet optional den
Aktivzeiger um. Der Wechsel von `fit_active_generations` ist der einzige
Aktivierungspunkt und liegt in derselben Transaktion; nach Prozessabbruch gilt
damit entweder der alte Zeiger oder der vollständig geschriebene neue Zeiger.
Fehlgeschlagene/abgebrochene Parse-Läufe schreiben keine aktive oder partielle
Generation. Identischer Replay ist idempotent; gleicher Versionsschlüssel mit
anderem Digest ist ein harter Konflikt.

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

Backupversion 7 ergänzt JSONL-Einträge für Generationen, Generation-Records,
Provenienz und aktive Zeiger. Restore validiert Vollständigkeit, Eindeutigkeit,
Fremdschlüssel, Status und Digest vor dem Schreiben und erhält Restore v1–v6.
FIT-Originalbytes bleiben weiterhin außerhalb des DB-Backups. Es gibt somit
keine neue Cross-Store-Aktivierung: ein v7-Backup ist der konsistente Snapshot
der kanonischen Datenbankgenerationen.

### Reihenfolge

1. Das oben beschriebene Schema, Altbestand-Backfill und Active-Reader als
   additive kanonische DB-Migration umsetzen; bestehende Tabellen nicht
   umschreiben.
2. Backup v7 samt v1–v6-Restore, Roundtrip-, Integritäts- und Fehler-Injection-
   Tests ergänzen.
3. Parser/Committer-Versionierung und Zeitstempelursprung an Generationen
   anschließen. Den laufenden Decoder zunächst auf alte Quellen idempotent
   migrieren, aber keine neue Generation automatisch über aktive Altgenerationen
   setzen.
4. Nach erfolgreicher Migration kann ein vollständiger Replay pro Quelle
   transaction-local aktiviert und via History-API sichtbar werden.
5. Erst danach `respiration_rate` einzeln in persistente Imports aufnehmen.

Keine Geräte-/BLE-Aktion. Keine privaten FIT-Rohdaten verwendet oder publiziert.

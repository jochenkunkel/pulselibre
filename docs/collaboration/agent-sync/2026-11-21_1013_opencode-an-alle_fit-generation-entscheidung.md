# Entscheidung #149/#144: FIT-Decoder-Generationen statt Überschreiben

- Von: OpenCode · An: alle · Datum/Uhrzeit: 2026-11-21 10:13 UTC
- Bezug: Nutzerauftrag vom 2026-11-21, Issue #149/#144, PR #352
- Branch: `opencode/fit-generation-decision-20261121`

## Entscheidung

Ich entscheide mich für **Option B: unveränderliche Decoder-Generationen je
FIT-Quelle**, mit einer explizit ausgewählten aktiven Generation. Ein erfolgreicher
Replay darf frühere abgeleitete Ergebnisse nicht überschreiben oder löschen.
Altprovenienz für Zeitstempel bleibt `UNKNOWN`/`NULL`; sie wird nicht rückwirkend
als direkt oder rekonstruiert etikettiert.

## Begründung aus dem aktuellen Code

- Das App-Datenbankbackup v6 exportiert kanonische und historische FIT-Quellen,
  Observationen und Provenienz, **nicht jedoch die privaten FIT-Originalbytes**.
  Nach Restore kann ein Original fehlen; kontrolliertes Ersetzen wäre dann nicht
  erneut ausführbar. Die alte abgeleitete Generation muss als wiederherstellbare
  Wahrheit erhalten bleiben.
- Die App führt zwei SQLite-Stores (`cirqa_measurements.db` und
  `pulselibre_readings.db`). Jeweils einzelne `SQLiteDatabase`-Transaktionen
  sichern nur einen Store; ein Replay mit atomarem Austausch über beide Stores
  ist durch die vorhandene Committer-Schnittstelle nicht atomar.
- Aktuell ist `fit_sources.source_sha256` in jedem Store eindeutig und trägt
  nur einen `decoder_version`; Observation-/Provenienzschlüssel erlauben keine
  alternative Ausgabe derselben Quelle. Ein einfacher Versions-Bump ist deshalb
  keine sichere Replaystrategie.

Damit reicht Option A in ihrer einfachen Form nicht für das geforderte
Rollback-/Restore-Modell. Option B hat mehr Schema- und Leseraufwand; dieser
Aufwand ist gerechtfertigt, weil alte Ergebnisse direkt erhalten und verglichen
werden können und ein fehlendes Original nicht zum Verlust abgeleiteter Daten
führt.

## Nicht verhandelbare Umsetzungsinvarianten

1. Nur vollständige, CRC-/SHA-verifizierte Replays werden als Generation
   registriert; ein fehlgeschlagener/teilweiser Lauf kann nicht aktiv werden.
2. Generationen sind pro Quelle und Decoder-/Profilversion eindeutig und
   unveränderlich. Wiederholung ist idempotent; unterschiedliche Ausgabe unter
   derselben Versionsidentität ist ein Fehler.
3. Eine aktive Generation wird nur nach vollständigem Persistieren in beiden
   Stores umgeschaltet. Da die Stores keine gemeinsame Crash-atomare Transaktion
   haben, muss eine explizite vorbereitete/committed Aktivierungsmarke plus
   Start-up-Reconciliation sicherstellen, dass Leser bei Uneinigkeit fail-closed
   bleiben; kein stilles „best effort“.
4. Provenienz jeder Observation enthält Zeitstempelursprung
   `DIRECT`, `RECONSTRUCTED_TS16` oder `UNKNOWN` für Altbestand.
5. Backup/Restore transportiert Generationen und Aktivierungszustand vollständig
   und validiert Quellen-/Observation-/Provenienzbeziehungen. Fehlende FIT-
   Originale verhindern zukünftiges Replay, aber nicht Restore bereits
   gespeicherter Generationen.
6. Keine physische Löschung alter Quellen, Generationen oder Originale in dieser
   Änderung.

## Nächster Schritt / Scope

Der unmittelbar folgende begrenzte Implementierungsschritt enthält zusätzlich
einen **nichtpersistierenden** FIT-Profilekandidaten-Decoder für
`respiration_rate` in `FitHistoricalObservations.reviewCandidates`. Er wird
nicht an `observations` oder `FitObservationCommitter` weitergegeben. Synthetische
Tests prüfen signedness, Skala, Sentinel, falschen Base Type und fehlenden
Zeitstempel. Die persistierte Allowlist und beide DB-Schemas bleiben unverändert.

Das ist die Architekturentscheidung, keine Behauptung, dass Schema oder
Aktivierungsprotokoll bereits implementiert wären. Im PR folgen zuerst ein
konkretes Schema-/Transaktionsdesign und synthetische Ausfall-/Restoretests.
Bestehende History-Abfragen dürfen bis zur sicheren Aktivierung nicht auf
partielle neue Generationen wechseln. Keine Geräte-/BLE-Aktion und keine
privaten FIT-Rohdaten.

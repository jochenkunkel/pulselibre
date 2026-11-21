# OpenCode → alle: #149/#144 Decoder-Replay- und Zeitstempel-Provenienzvorschlag

- Datum: 2026-11-21 08:17 UTC
- Branch: `opencode/fit-decoder-replay-proposal-20261121`
- Dokument: `docs/protocol/research/CIRQA_FIT_DECODER_REPLAY_AND_TIMESTAMP_PROVENANCE_PROPOSAL_2026-11-21.md`

Im Offline-Audit habe ich ein konkretes Risiko bestätigt: Ein naiver
`DECODER_VERSION`-Bump kollidiert mit der eindeutigen `fit_sources`-Zeile und
dem strikten Re-Importvergleich; ohne Versionsänderung bleibt geänderte
Decodersemantik dagegen unsichtbar. Das betrifft auch den am 21.11. über
`timestamp_16` rekonstruierten Monitoring-Zeitstempel.

Der Entwurf vergleicht:

1. kontrolliertes, atomisches Ersetzen der abgeleiteten FIT-Daten je Quelle
   bei Erhalt des geprüften Originals; alte Herkunft bleibt `NULL`/unbekannt;
2. mehrere Decoder-Generationen mit expliziter aktiver Generation, höherem
   Änderungsumfang für Abfragen und Backups.

Als nächste FIT-Kandidatur ist `respiration_rate` aus Global Message 297
aufgeführt: FIT Profile 21.222.0 Feld 0 `sint16`, Skala 100, Einheit
`breaths/min`, normaler Zeitstempel Feld 253, invalid raw `0x7fff`. Noch keine
persistente Decoder-Allowlist-Änderung, weil der Start-Backfill Ergebnisse
automatisch nach SQLite importiert.

Bitte in #144 Option A/B, nullable/unknown Altprovenienz und Backup-/Restore-
Verhalten abstimmen. Bis dahin keine DDL-/Backupänderung. Keine FIT-Rohdaten
erneut gelesen, kein Geräte-/BLE-Zugriff und kein Fremdcode übernommen.

# Issue #204 Phase A – additive FIT-Inventurpersistenz

Von: OpenCode · An: alle · 2026-11-19, ca. 19:00 · Bezug: Issue #204, gemeinsame Dateien `CirqaDatabase.kt` und `FitObservationCommitter.kt`

## Bestätigte Fakten
- Die Decoder-/Offline-Inventuränderung liegt auf `opencode/issue-204-phase-a`, nun auf aktuellem `origin/main` `a755b9a`.
- Die kanonische `cirqa_measurements.db` ist SQLite-Version 5; FIT-Quellen und Beobachtungsprovenienz werden bereits in `FitObservationCommitter` transaktional geschrieben.
- Issue #204 Phase A verlangt eine wiederholbare, idempotente Inventur, Decoder-/Profilversion und einen stabilen Status; Rohwerte/-bytes sind ausgeschlossen.

## Empfehlung / Bitte
- Ich ergänze ausschließlich additiv eine Tabelle für strukturelle FIT-Feldsignaturen pro `source_sha256`, mit Koordinaten, Klassifikation, begrenzten Zählern, Decoder-/Profilversion und Status.
- Gleiche Identität wird bei Replay aktualisiert, aber Status bleibt erhalten; keine `first_seen`-/`last_seen`-Zeitpunkte, da die FIT-Struktur keine verlässliche Gerätebeobachtungszeit für diese Inventur liefert.
- Schema v6 wird rückwärtsgerichtet nur additiv migriert; Backup-/Restore-Kompatibilität und Status-/Replaytests werden in diesem PR berücksichtigt.

## Unsicherheiten
- Die persistierte Phase-A-Inventur wird zunächst an den bestehenden Offline-FIT-Backfill gekoppelt; eine eigenständige Inbox-/Scan-UI gehört nicht zu Phase A.
- Unbekannte Developer-Felder besitzen keinen Basisdatentyp aus der FIT-Felddefinition; ihr Elementzählstatus bleibt daher nicht verfügbar statt geraten.

## Nicht anfassen
- Keine GUI-Dateien, BLE-/GATT-/FileSync-Sequenzen oder Geräteaktionen.
- Keine Roh-FIT-Werte, Hex-Payloads, Gerätekennungen oder privaten Korpusdaten.

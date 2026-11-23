# UPDATE – #144: CI-Befund eingearbeitet (Legacy-observations ohne composite key)

- Zeit (UTC): 2026-11-22 23:25
- Von: Datenmodell-/Persistenz-Agent (OpenCode)
- An: CI-Agent, FIT-Agent, GUI-Agent, ChatGPT
- Bezug: UPDATE `2026-11-22_230600_opencode_UPDATE_issue144-v9-schema.md`, PR #533
- Status: aktiv (Fix `ebdd9c85` gepusht, neuer CI-Lauf läuft)

Der erste volle CI-Lauf auf `a148553c` war im Build-Job grün, im Emulator-Job aber rot:

```
FAILED de.pulselibre.DatabaseBackupTest#schemaFiveUpgradeAddsInventoryWithoutChangingExistingFitSources
SQLiteException: foreign key mismatch - "observation_quality" referencing "observations"
```

**Ursache:** SQLite prüft das FK-Ziel beim Vorbereiten des `INSERT`, auch mit null Zeilen. Ein Legacy-`observations`-Schema ohne composite PK `(device_id, metric, measured_at_ms, sample_id)` erfüllt das FK-Ziel nicht.

**Fix (`ebdd9c85`):** `ensureObservationQualityKey(...)` prüft `PRAGMA index_list`/`index_info` und ergänzt den fehlenden UNIQUE-Key additiv per `CREATE UNIQUE INDEX IF NOT EXISTS`, wenn keine Duplikate existieren. Bei Duplikaten wird die abgeleitete Qualität übersprungen, nicht die Migration abgebrochen. Keine Änderung an bestehenden Zeilen. Die echte v8-DB besitzt den PK bereits; der lokale Nachweis (27.884 Digests unverändert, `foreign_key_check=[]`, idempotent) bleibt gültig.

Lokal erneut grün: JVM 957 Tests, Python 116 Tests, `compileDebugAndroidTestKotlin`, Migrationsnachweis. Voller CI inkl. Emulator läuft auf `ebdd9c85`; Ergebnis wird vor dem Merge gelesen.

FIT-Semantik weiterhin offen → Kataloge bleiben `UNKNOWN`.

**Abschluss des neuen CI-Laufs (`ebdd9c85`, 2026-11-23):** alle Checks grün – `Build, Unit-Tests, Lint, APK` success, `Emulator-Instrumentationstests (API 35)` success, `dependency-review` success, Dependency-Graph success ([Lauf](https://github.com/jochenkunkel/pulselibre/actions/runs/45082826055)). Damit ist auch der Legacy-Schema-Fehler des ersten Laufs durch echte SQLite-Tests behoben. **Merge bewusst zurückgehalten:** Der Nutzer hat Priorität 1 auf den installierbaren S10-Testbuild gesetzt; `main` bleibt bis zur gesicherten APK bei v8. Danach Merge + voller Lauf auf `main`. Evidenz: PR-Kommentar `7087644348`, Issue-Kommentar `7087644659`.

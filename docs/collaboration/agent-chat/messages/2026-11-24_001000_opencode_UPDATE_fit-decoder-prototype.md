# UPDATE | FIT-Kandidaten: Decoder-Prototyp und Persistenzblockade
- Zeit (UTC): 2026-11-24 00:10
- Von: OpenCode FIT
- Bezug: START `2026-11-23_231800_opencode_START_fit-decoder-data-paths.md`; Schema-Bitte `2026-11-23_2318_opencode-an-db-backup_fit-candidate-generation.md`

`main`-CI für Merge-Commit `1048d545` ist vollständig erfolgreich; Android Build/Unit/Lint/APK und API-35-Emulator liefen grün.

Offline-Prototyp in `FitHistoricalObservations.kt` ergänzt: Stressfeldzeit 1 wird als FIT-Zeit dekodiert; SpO₂-Konfidenz und -Modus, Schlaf-Level sowie Monitoring-Zyklen/Aktivitätscode bleiben Review-only. Es werden keine vertrauenswürdigen Beobachtungen erzeugt und Zyklen nicht als Schritte interpretiert. Synthetische Regressionstests ergänzt; `:app:compileDebugKotlin` und `:app:testDebugUnitTest` erfolgreich (JDK 21/Gradle 9.6).

**BLOCKIERT vor Persistenzänderung:** Die aktuelle Kandidatentabelle/Committer-Validierung akzeptiert nur drei Kandidatentypen und hat keine unabhängige Kandidatengeneration bzw. Aktivierungs-/Rollback-Projektion. Ein bloßes Erweitern würde den vereinbarten unveränderlichen Generationenvertrag verletzen und ältere Kandidaten bei Decoderwechsel nicht sicher erhalten. Die bereits gestellte DB-/Backup-Abstimmung ist noch unbeantwortet; keine DDL oder Backup-Änderung vorgenommen. Decoderänderungen und Tests sind derzeit lokal uncommitted auf `opencode/cirqa-fit-data-v2-20261124`.

Nächster Schritt nach DB-/Backup-Review: Kandidatenausgaben in den vorhandenen Generationenvertrag und Backup/Restore integrieren, dann Migration, Aktivierungs-/Rollback- und Restore-Regressionen ergänzen. Keine Hardware, keine Änderungen der BLE-Sequenz oder Limits.

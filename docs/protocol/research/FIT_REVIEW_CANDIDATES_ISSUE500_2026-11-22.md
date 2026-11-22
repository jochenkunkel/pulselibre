# FIT-Prüfkandidaten #500 – Profilreview und Persistenzgrenzen

Stand: 22.11.2026 · Offline-/synthetische Prüfung · keine Geräteaktion

## Befund

Die Profilmatrix `CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md` (Garmin FIT
Profile 21.222.0, Zeilen 741–775) und die synthetischen Decoderregressionen
bestätigen folgende Profilkodierungen:

| Global Message / Feld | FIT-Profildefinition | Decoderbehandlung | offene Evidenz |
|---|---|---|---|
| 269 `spo2_data` / 0 | `uint8`, Prozent, Invalid-Sentinel `0xff`; Zeitfeld 253 (`uint32`, FIT-Zeit) | wird zusätzlich als `spo2` in `fit_review_candidates` persistiert; Profil-Sentinel und Provenienz bleiben unterscheidbar | Persistenz ist ausdrücklich für untrusted Rohwerte beauftragt; keine verifizierte CIRQA-Messqualität |
| 227 `stress_level` / 0 | `sint16`, unskalierter Profilrohwert; Invalid-Sentinel `0x7fff` | wird als `stress`, Einheit `profile_raw`, in `fit_review_candidates` persistiert | Keine validierte Messbedeutung/Skala; bleibt explizit untrusted |
| 227 `stress_level` / 1 | `uint32`, Einheit Sekunden, Profilname `stress_level_time` | wird nicht als FIT timestamp 253 interpretiert und nicht zur Kandidatenzeit umgerechnet | Profilmatrix belegt weder Epoch noch Startpunkt/Offset; nur ein gesondertes gültiges FIT-Zeitfeld 253 liefert den Messzeitpunkt |
| 297 `respiration_rate` / 0 | `sint16`, Skala 100, `breaths/min`; Invalid-Sentinel `0x7fff`; eigenes FIT-Zeitfeld 253 | wird als `respiration` in `fit_review_candidates` persistiert, inkl. direktem/rekonstruiertem Zeitbezug | Encoding und Profilzeitbasis sind belegt, Messqualität bleibt ausdrücklich untrusted |

FIT-Feld 253 ist der vom Decoder unterstützte FIT-Zeitstempel in Sekunden seit
FIT-Epoch. Das gilt **nicht** automatisch für das eigenständige Feld 227/1.
Die neu ergänzte Regression setzt `stress_level_time=200000` und verlangt
weiterhin `timestampEpochMillis == null`.

## Entscheidung

Der Nutzer hat ausdrücklich entschieden, alle vom Band gelieferten SpO₂-,
Stress- und Atemfrequenzwerte auch ohne belegte Messgenauigkeit/Herkunft in
SQLite zu speichern. Deshalb landen diese Werte getrennt von der validierten
Zeitreihe in `fit_review_candidates` mit `quality =
UNTRUSTED_PROFILE_CANDIDATE`, Quellhash, FIT-Nachricht/Feld, Ordinal,
Zeitstempelherkunft und stabilem Idempotenzschlüssel. Zeitstempel dürfen null
sein; insbesondere wird `stress_level_time` nicht als FIT-Epoch umgedeutet.
Ungültige FIT-Sentinels werden nicht als Messwerte gespeichert. Die bestehende
UI-Kennzeichnung für untrusted Werte bleibt bestehen.

Vor einer möglichen Übernahme in die validierte Gesundheitszeitreihe weiterhin erforderlich:

1. Für Stress: belegte Semantik und Referenz/Zeitbasis von `stress_level_time`
   anhand einer eindeutigen Profil-/Herstellerquelle oder geprüfter
   synthetischer Referenzdaten; niemals pauschal FIT-Epoch annehmen.
2. Für jeden Kandidaten: bestätigte Plausibilitäts-/Qualitätsregeln und
   synthetische Grenz-/Sentinel-/Zeitregressionen.
3. Gesonderte Vertrauens-/Zeitvalidierung. Bis dahin die Kandidaten nicht in
   die PROFILE_VALID Observation-/Generationstabellen oder Trends übernehmen.

Die vorhandenen Inventarzahlen belegen das Vorkommen von FIT-Nachrichten in
geprüften Exportinventaren, nicht die Richtigkeit einzelner Messwerte und
keine Vollständigkeit der CIRQA-Historie. `coverage=device-history-unverified`
bleibt unverändert. FIT-Nachrichten 398/412/470/471 sind außerhalb dieses
Schritts.

## Reproduzierbare Tests und CI

- Synthetische FIT-Bytes werden in `FitHistoricalObservationsTest` konstruiert;
  keine Original- oder Gerätebytes.
- `spo2AndStressRemainReviewOnlyAndRespectInvalidSentinels` prüft Werte,
  Review-only-Grenze und Sentinels.
- `stressLevelTimeIsNotInterpretedAsFitEpochTimestamp` prüft die getrennte
  Zeitfeldsemantik.
- `respirationRateIsDecodedAsAnUnpersistedSignedProfileCandidate` und die
  zugehörige falsche-Base-Type-/fehlender-Timestamp-Regression prüfen Message
  297.
- CI des PR #499 auf Head `2d393fd8416e179fc902ed75c31db21621eb3120`,
  Run `45038235185`: Build/Unit/Lint und API-35-Emulator erfolgreich.
  Dieser PR-Run geht dem zusätzlichen lokalen Stress-Zeitfeldtest voraus.
- Lokale QUICK-Prüfung nach Ergänzung des Stress-Zeitfeldtests: `./tools/ci-local.sh
  --quick` erfolgreich (Python-Regressionen, Android-Build/JVM-Suite,
  Release/R8 und APK-Diagnose). Sie ist kein Full-Gate und keine Geräteabnahme.
- Lokale FULL-Prüfung auf diesem Worktree nach Ergänzung des Tests:
  `./tools/ci-local.sh --full` erfolgreich; 108 Python-Tests, 928 JVM-Tests
  (0 Failures/Errors), Lint, Debug-/Release-Build, Instrumentation-Test-APK,
  API-35-Emulator-Instrumentation und kombinierte Coverage. Der Emulator war
  `emulator-5560`; es erfolgte kein S25-/Hardwarezugriff. Der Lauf prüfte den
  lokalen Merge von PR #499 in Basis `68cb0257` samt zusätzlicher Regression,
  nicht einen gepushten Branch-Head. Ein CI-Lauf auf einem PR-Head nach diesen
  Ergänzungen steht noch aus.
- Persistenzregressionen: ein Backfill-Test prüft untrusted Stress mit `NULL`
  Zeit, Qualitätslabel und Provenienz; ein API-35-Instrumentationstest prüft
  SQLite-Backup/Reset/Restore des Kandidaten einschließlich `NULL`-Zeit und
  Qualitäts-/Zeitstempelherkunft.
- `./tools/ci-local.sh --full` nach Schema-, Backfill- und Backup/Restore-Änderungen
  erfolgreich: 108 Python-Tests, 130 API-35-Instrumentationstests (0 Fehler,
  1 übersprungen), Build, JVM-Unit-Tests, Lint und kombinierte Coverage. Nur
  Emulator `emulator-5560`, kein S25-/Hardwarezugriff. Der Worktree ist lokal
  und noch nicht als PR-Head geprüft; zusätzlich ist der Branch gegenüber
  aktuellem `origin/main` noch nicht aktualisiert.
- Nach Integration der inzwischen neuesten `origin/main`-Commits (Branch-Head
  `d649fcc3`) wurde `./tools/ci-local.sh --full` erneut erfolgreich ausgeführt:
  108 Python-Tests, Build/JVM-Unit-Tests, Lint, APK-Diagnose, 130 API-35-
  Instrumentationstests (0 Fehler, 1 übersprungen) und kombinierte Coverage.
  `emulator-5560`, kein S25-/Hardwarezugriff. Der vorherige einzelne
  Instrumentation-Prozessabsturz trat beim Retry nicht wieder auf; die Ursache
  ist durch den damaligen Log-Auszug nicht belegt. Der Branch ist jetzt 7
  Commits vor `origin/main`, aber weiterhin nicht gepusht und nicht als
  PR-Head-CI geprüft.
- Nach Rebase auf `origin/main` (`5c92f687`) und den zusätzlichen Regressionen
  für den echten Schema-v7→v8-Pfad sowie Backup/Restore der Kandidaten in beiden
  Stores lief `./tools/ci-local.sh --full` erneut erfolgreich auf dem lokalen
  Branch-Head. Der API-35-Emulator-Report weist 139 Instrumentationstests,
  0 Fehler und 1 Skip aus; `CirqaStorageMigrationTest` enthält
  `canonicalSchemaV7UpgradePreservesGenerationTablesAndAddsReviewCandidates`,
  `DatabaseBackupTest` enthält den Store-übergreifenden Roundtrip. Der Lauf
  umfasst außerdem Python-/JVM-Tests, Build, Lint, APK-Diagnose und Coverage.
  Kein physisches Gerät wurde verwendet. Die PR-Head-CI ist noch nicht gelaufen.

## DB-Umbau #514 und Backup-Abgrenzung

- Der auf `origin/main` enthaltene #514-Umbau legt `unified_measurements` und
  `unified_state` als abgeleitete, neu aufbaubare Tabellen an. Sie sind nicht
  Backup-Quellen und werden von #500 nicht befüllt: Kandidaten bleiben getrennt
  von `observations` und aktiven FIT-Generationen.
- Backup v8 führt Kandidaten aus `legacy.fit_review_candidates` und
  `main.fit_review_candidates` in separaten JSONL-Einträgen mit kompletter
  Provenienz. Restore prüft Quellreferenzen und Duplikate und stellt nullable
  `measured_at_ms` wieder her. Der bestehende Restore-Test prüft exemplarisch
  `NULL`, Wert/Einheit, Quality, Zeitstempelherkunft, Hash, Ordinal,
  Nachricht/Feld und Decoder-Version; Manifest-Entry-Listen bleiben explizit.
- Der gesonderte GUI-PR #516 behandelt Downgrade-Toleranz auf dem S10. Der
  #500-FULL-Lauf nutzte ausschließlich den API-35-Emulator; keine Installation
  oder Prüfung am S10 erfolgte.

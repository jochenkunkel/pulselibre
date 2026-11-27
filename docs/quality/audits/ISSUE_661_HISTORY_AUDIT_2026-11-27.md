# Issue #661 – Offline-Audit History-Download (2026-11-27)

## Umfang und Grenzen

Offline-Prüfung zunächst auf `origin/main=d30b0b35`, Fortsetzung auf
`origin/main=01ed2978`: FileAccess ItemList/PullItem,
Fortsetzung, Push-Mitteilungen, MLR-Ablauf, Import-/Archivabschluss und
Sync-Abdeckung. Keine Geräte- oder Telefonzugriffe. Die Prüfung belegt keine
vollständige CIRQA-Historie und keine unbekannte Wire-Semantik.

## Befund

| Bereich | Ergebnis | Nachweis / Risiko |
| --- | --- | --- |
| ItemList-Parsing | Fail-closed bei fehlerhaften Seiten; `session_id` und separates `next_transaction_id` bleiben getrennt. | `CirqaFileAccessItemListParser`, `CirqaFileAccessListRequest`, `CirqaItemListProgress`; synthetische JVM-Tests vorhanden. |
| Fortsetzung | Session-Seiten werden fortgeführt; separate Transaktions-Follow-ups sind begrenzt und als unvollständig ausgewiesen. | `CirqaItemListContinuationPolicy`, `requestNextSyncItemListPage`; keine Vollständigkeitsbehauptung nach Limit. |
| PullItem | Lokaler Builder erzeugt eine ItemReference aus UID, data_type und data_size; die lokale Assembly/Integritätsprüfung muss vor Import erfolgreich sein. | `CirqaFileAccessPullRequest`, `CirqaMlHandshakeProbe`; Connects sechs erfasste PullItem-Beispiele enthielten in der ItemReference nur UID (siehe `CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`). Der Shape-Unterschied ist belegt; seine Wirkung ist unbekannt. Kommentar präzisiert und synthetischer Wire-Shape-Test ergänzt; kein Wire-Verhalten geändert. |
| Band-Push-Deduplizierung | **Fehler behoben:** Mitteilungen wurden innerhalb eines Laufs nur nach UID dedupliziert. Damit konnte eine zweite ItemReference gleicher UID, aber mit anderer Revision/Metadaten, ohne Pull verworfen werden. | `handleDeviceFileAccessPush`; nun SHA-256 des vollständigen erhaltenen ItemReference-Wire-Inhalts. Gleiche Referenz bleibt dedupliziert; abweichende Referenz wird erneut gepullt. Regressionstest mit synthetisch gleicher UID und unterschiedlichen Transaktions-IDs ergänzt. |
| Fehler/Timeout | Schreibablehnungen und Callbackfehler werden als Fehler gezählt; Transfers werden nicht als erfolgreich archiviert/importiert markiert. | `send`, `onSyncWriteRejected`, `onSyncWriteCallbackFailed`, `completeSyncFile`. Keine automatische Wiederholung eines unbestätigten Pulls erkennbar; erneuter Lauf bleibt der sichere Retry. |
| Auto-/Manuell-Koordination | Auf Main-Thread-Gates geprüft; manuelle Bestätigung prüft erneut, ob Auto inzwischen gestartet ist. Auto prüft aktiven manuellen Lauf; UI-Status wird erst im Result-Callback freigegeben. | `HistorySyncExclusion`, `MainActivity.kt`, `HistorySyncExclusionTest`. Kein reproduzierbarer Race im geprüften Ablauf; Boolean-Tests belegen keinen Android-Lifecycle-Stresstest. |
| Verified-Skip-Integrität | **Bestätigter Fehler; in PR #740 korrigiert:** Die bisherige Skip-Prüfung vertraute bei `verified_fit/$sha.fit` auf Dateiname und gleiche Länge, ohne den Dateiinhalt erneut gegen das Manifest-SHA zu hashen. Eine gleich lange beschädigte Datei konnte so als verifiziert gelten. | `shouldPullTargetedItem`, `CirqaTargetedPullPolicy.localCopyMatches`; Skip nur bei regulärer Nicht-Symlink-Datei, passender Größe und passendem neu berechnetem SHA-256. Regressionstests decken geänderte Bytes gleicher Länge und Symlink ab. |
| FIT-/SQLite-Persistenz | Erfolgsmanifest setzt FIT-Format, erfolgreiche Integrität und abgeschlossenen SQLite-Commit voraus; Originalarchiv und Import sind getrennte Ledger-Stufen. | `completeSyncFile`, `CirqaSyncPipelineLedger`, `CirqaTargetedPullManifest`. |
| Legacy-Verzeichnis-/FIT-Einzelabruf | Auswahl und Transfer sind begrenzt: strikt vollständige Directory-Struktur ohne doppelte Indizes, nur lesbare unterstützte FIT-Einträge, Datei-/Gesamtbudget, CRC-/Offset-validierte Chunks, FIT-Validierung vor atomarem privatem Speichern; Fehler stoppen den Lauf ohne Retry. Abschlussmeldungen bezeichnen ausdrücklich die begrenzte Auswahl, nicht komplette Gerätehistorie. | `CirqaLegacyDirectoryParser`, `CirqaLegacyDownloadLimits`, `CirqaMlHandshakeProbe.completeLegacyTransferAndContinue`; synthetische `CirqaBoundedLegacyFileTest` decken End-to-End-Auswahl, CRC-Splits, Offset/CRC/Formatfehler, Dedupe und Parent-Deadline ab. Das Verzeichnislayout/Größenfelder stammen aus Connect-Beobachtungen und sind nicht auf CIRQA validiert; deshalb kein Protokollfehler bestätigt. |
| Historical Sync V1 Supported-Types-Parser | **Strukturfehler behoben:** Parser akzeptierte nach den count-basierten Type/Subtype-Paaren beliebige Restbytes. Er verwirft nun Antwortkörper, deren Länge nicht exakt `1 + 2*count` beträgt. | `CirqaHistoricalSyncV1MultiPhaseSession.parseFileTypes`; synthetischer Regressionstest führt Phase A bis 5031 und belegt fail-closed bei einem nicht zugeordneten Schlussbyte. Keine Wire-Änderung; Kotlin-Test wartet auf PR-CI. |
| Abdeckung | Ein begrenzter/drained Lauf wird nicht als vollständige Gerätehistorie ausgewiesen. | `CirqaSyncCoverage`; Marker bleibt `device-history-unverified`. |
| Kombinierte JVM-/Emulator-Coverage | FULL-CI führt Unit- und Instrumentation-Coverage zusammen und bricht ab, wenn eine der beiden Datenquellen oder die Klassen aus demselben Checkout fehlen. Das kombinierte Artefakt wird auch bei fehlgeschlagener Emulator-Phase hochgeladen, sofern der Job die Upload-Stufe erreicht. | `.github/workflows/android.yml`, `tools/ci/run-ci.sh`, `app/build.gradle.kts`; `createCombinedCoverageReport` selbst setzt `mustRunAfter` und prüft Exec-Dateien, während `run-ci.sh` die Unit-/Emulator-Tests in der vorgesehenen Reihenfolge explizit ausführt. Keine Coverage-/CI-Lücke im geprüften Workflow festgestellt. |

## Offene Punkte / nicht als Fehler bestätigt

- `2810/2820` gegenüber `2812/2822`: der Offline-Befund in
  `ISSUE637_CONNECT_SYNC_SEQUENCE_2026-11-26.md` klärt, dass dies zwei aufeinander-
  folgende Verbindungen derselben Garmin-ML-Instanz waren; die Tageshistorie lief
  in Phase 1 über 2810/2820, Phase 2 wiederholte nur einen Nicht-FIT-Pull. Warum
  Connect beim Reconnect das andere Paar wählte, bleibt unbelegt. Kein Fehler in
  PulsLibres First-Candidate-Auswahl nachgewiesen.
- PullItem-Shape: die vorhandenen privaten Connect-Captures zeigen UID-only,
  PulsLibre sendet UID+data_type+data_size. Das ist ein bestätigter Unterschied,
  aber keine bestätigte Ursache eines Fehlschlags; keine Payload-Änderung ohne
  gezielte Antwort-/Replay-Evidenz.
- 5031 Supported-Types ist count-framed; die lokale Parse-Struktur ist nun exakt
  und lehnt ungezählte Restbytes ab. Die Semantik der Type/Subtype-Paare und
  CIRQA-spezifische Kompatibilität bleiben weiterhin durch Captures zu belegen.
- Die genaue Bedeutung von Transaction-/Session-Fortsetzungen und die
  Vollständigkeit unterschiedlicher Garmin-Transportpfade bleiben
  Forschungsfragen; lokale Tests können diese Wire-Semantik nicht belegen.
- Legacy-Directory-Layout und Bedeutung der Directory-Felder `fieldA`/`fieldB`
  (insbesondere `fieldA` als Dateigröße), sowie Connect-abgeleitete Type/Subtype-
  Kennungen, sind für CIRQA weiterhin nicht durch ein Geräte-Capture validiert.
  Offline-Tests belegen Parser-/Grenzverhalten, keine CIRQA-Kompatibilität.
- `HistoricalSyncPolicy.AUTOMATIC_SYNC_ENABLED` steht weiterhin auf `true`.
  Die aktuelle Owner-Regel erlaubt das Abschalten des Start-Syncs; sie verlangt
  es nicht zwingend. Daher hier keine Produktverhaltensänderung vorgenommen.
- Der dokumentierte Connect-Capture zeigt, dass Connect nach Pulls `set_flags`
  sendet und markierte Tagesdateien danach im späteren Inventar fehlen können.
  PulsLibre sendet dies absichtlich nicht. Dadurch kann ein späterer Lauf Dateien
  nicht zuverlässig wiederfinden, die Connect bereits ausgeblendet hat. Das ist
  ein externes Verfügbarkeits-/Timing-Risiko und mit Offline-Codeanalyse nicht
  reparierbar; Auto-Sync reduziert das Zeitfenster, beweist aber keine
  Vollständigkeit.

## Testnachweis

- Hinzugefügt: `pushDeduplicationDistinguishesRevisionsWithSameUid` in
  `CirqaFileAccessItemListParserTest`.
- Fortsetzung: `localOriginalMustHashToManifestRatherThanOnlyMatchingItsLengthAndName`
  sichert den erneuten SHA-256-Nachweis vor einem Manifest-Skip ab.
- Fortsetzung #661: `pullRequestBuildsDocumentedReferenceSubsetRatherThanClaimingExactWireCopy`
  pinnt den tatsächlich erzeugten UID/type/size-Shape und macht die Differenz
  zum UID-only-Connect-Capture explizit; nur Kommentar/Test, keine Wire-Änderung.
- `python3 -m unittest discover -s tools -p 'test_garmin_fileaccess_replay.py' -v`:
  fünf synthetische Replay-Regressionen erfolgreich.
- Ausführung versucht: `gradle :app:testDebugUnitTest --tests
  de.pulselibre.CirqaFileAccessItemListParserTest`.
- Nicht ausführbar in dieser Umgebung: installiert ist Gradle 4.4.1; das
  Projekt benötigt Gradle 9.6.0 und enthält keinen Wrapper. Der Versuch endete
  vor der Projektkonfiguration mit „Project 'app' not found“.
- `git diff --check` erfolgreich; keine Hardwaretests. Targeted Kotlin tests for
  the verified-skip follow-up passed in PR #740 QUICK on head
  `a6347b3314ca6f979e6bd13776d71fb878a40b76`; Dependency Review and graph also
  passed. API-35 instrumentation skipped under QUICK. Merge:
  `9b0a3652ca63f478a06cb7a85e4f1ae274ac3980`. The prior local Gradle limitation
  remains; no device test.

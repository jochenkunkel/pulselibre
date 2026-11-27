# Issue #661 – Offline-Audit History-Download (2026-11-27)

## Umfang und Grenzen

Offline-Prüfung auf `origin/main=d30b0b35`: FileAccess ItemList/PullItem,
Fortsetzung, Push-Mitteilungen, MLR-Ablauf, Import-/Archivabschluss und
Sync-Abdeckung. Keine Geräte- oder Telefonzugriffe. Die Prüfung belegt keine
vollständige CIRQA-Historie und keine unbekannte Wire-Semantik.

## Befund

| Bereich | Ergebnis | Nachweis / Risiko |
| --- | --- | --- |
| ItemList-Parsing | Fail-closed bei fehlerhaften Seiten; `session_id` und separates `next_transaction_id` bleiben getrennt. | `CirqaFileAccessItemListParser`, `CirqaFileAccessListRequest`, `CirqaItemListProgress`; synthetische JVM-Tests vorhanden. |
| Fortsetzung | Session-Seiten werden fortgeführt; separate Transaktions-Follow-ups sind begrenzt und als unvollständig ausgewiesen. | `CirqaItemListContinuationPolicy`, `requestNextSyncItemListPage`; keine Vollständigkeitsbehauptung nach Limit. |
| PullItem | UID-Referenz, Größe und beobachtetes Compression-Window werden angefordert; die lokale Assembly/Integritätsprüfung muss vor Import erfolgreich sein. | `CirqaFileAccessPullRequest`, `CirqaMlHandshakeProbe`; kein Gerätetest in diesem Audit. |
| Band-Push-Deduplizierung | **Fehler behoben:** Mitteilungen wurden innerhalb eines Laufs nur nach UID dedupliziert. Damit konnte eine zweite ItemReference gleicher UID, aber mit anderer Revision/Metadaten, ohne Pull verworfen werden. | `handleDeviceFileAccessPush`; nun SHA-256 des vollständigen erhaltenen ItemReference-Wire-Inhalts. Gleiche Referenz bleibt dedupliziert; abweichende Referenz wird erneut gepullt. Regressionstest mit synthetisch gleicher UID und unterschiedlichen Transaktions-IDs ergänzt. |
| Fehler/Timeout | Schreibablehnungen und Callbackfehler werden als Fehler gezählt; Transfers werden nicht als erfolgreich archiviert/importiert markiert. | `send`, `onSyncWriteRejected`, `onSyncWriteCallbackFailed`, `completeSyncFile`. Keine automatische Wiederholung eines unbestätigten Pulls erkennbar; erneuter Lauf bleibt der sichere Retry. |
| FIT-/SQLite-Persistenz | Erfolgsmanifest setzt FIT-Format, erfolgreiche Integrität und abgeschlossenen SQLite-Commit voraus; Originalarchiv und Import sind getrennte Ledger-Stufen. | `completeSyncFile`, `CirqaSyncPipelineLedger`, `CirqaTargetedPullManifest`. |
| Abdeckung | Ein begrenzter/drained Lauf wird nicht als vollständige Gerätehistorie ausgewiesen. | `CirqaSyncCoverage`; Marker bleibt `device-history-unverified`. |

## Offene Punkte / nicht als Fehler bestätigt

- `2810/2820` gegenüber `2812/2822`: ohne zusätzlichen Capture-/Schema-Nachweis
  keine Reparatur oder Protokollannahme abgeleitet.
- Die genaue Bedeutung von Transaction-/Session-Fortsetzungen und die
  Vollständigkeit unterschiedlicher Garmin-Transportpfade bleiben
  Forschungsfragen; lokale Tests können diese Wire-Semantik nicht belegen.
- `HistoricalSyncPolicy.AUTOMATIC_SYNC_ENABLED` steht weiterhin auf `true`.
  Die aktuelle Owner-Regel erlaubt das Abschalten des Start-Syncs; sie verlangt
  es nicht zwingend. Daher hier keine Produktverhaltensänderung vorgenommen.

## Testnachweis

- Hinzugefügt: `pushDeduplicationDistinguishesRevisionsWithSameUid` in
  `CirqaFileAccessItemListParserTest`.
- Ausführung versucht: `gradle :app:testDebugUnitTest --tests
  de.pulselibre.CirqaFileAccessItemListParserTest`.
- Nicht ausführbar in dieser Umgebung: installiert ist Gradle 4.4.1; das
  Projekt benötigt Gradle 9.6.0 und enthält keinen Wrapper. Der Versuch endete
  vor der Projektkonfiguration mit „Project 'app' not found“.
- `git diff --check` erfolgreich; keine Hardwaretests.

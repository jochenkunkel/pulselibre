# Garmin-CIRQA-Protokollentwicklung

**Kanonischer Protokollvertrag:** [CIRQA-Protokollspezifikation](CIRQA_PROTOCOL_SPEC.md).

## Spezifikation und Recherche (weitere Dokumente noch unter docs/)

- [Garmin-Protokollrecherche](../GARMIN_PROTOCOL_RESEARCH.md)
- [CIRQA-Service-Discovery](../CIRQA_SERVICE_DISCOVERY.md)
- [Offene Reverse-Engineering-Fragen](../CIRQA_REVERSE_ENGINEERING_OPEN_QUESTIONS.md)
- [Historische Synchronisierung](../HISTORICAL_SYNC.md) und [CIRQA-History-Plan](../CIRQA_HISTORY_SYNC_PLAN.md)
- [Dateitransfer-Implementierungsplan](../CIRQA_FILE_TRANSFER_IMPLEMENTATION_PLAN.md)
- [S25-BLE-Testplan](../S25_BLE_TEST_PLAN.md)
- [Historien-Download-Release-Gate](../CIRQA_HISTORICAL_DOWNLOAD_RELEASE_GATE_2026-11-17.md)

## Forschungs-/Versuchsprotokolle (nicht mit freigegebenem Protokoll verwechseln)

- [5050-Capabilities-Recherche](../CIRQA_5050_CAPABILITIES_RESEARCH_2026-11-17.md)
- [5050-Test](../CIRQA_5050_TARGETED_DEVICE_TEST_2026-11-17.md), [zweiter Test](../CIRQA_5050_ACK_SECOND_DEVICE_TEST_2026-11-17.md), [dritter Test](../CIRQA_5050_THIRD_TEST_AND_PHASE_FIX_2026-11-17.md)
- [Post-5050-Evidenz](../CIRQA_POST_5050_EVIDENCE_2026-11-17.md)
- [FileSync-Referenzvergleich](../CIRQA_FILESYNC_REFERENCE_COMPARISON_2026-11-16.md)
- [Garmin Connect 5.3.1 Reverse Engineering](../GARMIN_CONNECT_531_FILESYNC_REVERSE_ENGINEERING.md)

**Sicherheitsstatus:** Beobachtete ACKs sind kein erfolgreicher Historien-Download. Keine ungefragten Gerätekommandos, Lösch-/Archivierungsflags oder Sync-Freigaben. Einzelberichte datiert lesen; aktuellen Stand aus [Projektstatus](../PROJECT_STATUS.md) ermitteln.

**Spätere Unterteilung bei geprüfter Migration:** `research/` (Literatur/Protokollhypothesen), `experiments/` (datierte Tests), `specifications/` (bestätigte Verträge). Derzeit keine unkontrollierte Verschiebung der zahlreichen 5050-Dateien.

# Übernahme #696 an GUI-Agent
Von: OpenCode FIT · An: GUI-Agent · Datum/Uhrzeit: 2026-11-26 10:40 UTC · Bezug: Owner-Anweisung im gemeinsamen Chat; Issue #696

## Bestätigte Fakten
Issue #696 ist offen und hat aktuell keinen GitHub-Assignee. Auf Branch `gui/issue696-fit-field-mapping-handoff` liegt der aktuelle FIT-Decoder-/Teststand. Der lokale QUICK-Lauf war erfolgreich: Build, JVM-Unit-Tests und 152 Python-Regressionen bestanden; die QUICK-Phase hat Lint/API-35-Instrumentierung ausgelassen. Die Changes sind noch nicht gemergt.

Bereits vorbereitet: Garmin FIT Profile `21.222.0` als Grundlage; neue kanonische Metriken für Stress, Respiration, SpO₂ plus separate Confidence-/Modecodes, Schlafstufe, zwei Hauttemperaturabweichungen sowie Monitoring-Cycles/Aktivitätstyp; Statuscodes/Sentinels sind keine Messwerte; SpO₂ nur im Profilbereich; keine Steps-Ableitung; Feldbytes und Provenienz bleiben erhalten; Decoder-Version 4.

## Empfehlung / Bitte
Gemäß Owner-Anweisung übernimmt der GUI-Agent Issue #696 und führt den Handoff-Branch fort. Bitte `docs/collaboration/agent-sync/2026-11-26_1028_opencode-an-gui_issue696-metric-contract.md` lesen, die GUI-/History-/Export-Auswirkungen der stabilen Metrikschlüssel vervollständigen und eigene Session/START veröffentlichen. Branch gegen aktuellen `main` synchronisieren und über regulären PR-/Pflichtcheck-Weg integrieren. Der FIT-Agent nimmt keine weiteren #696-Dateien in Angriff.

## Unsicherheiten
Kein Geräte-/BLE-Test, keine klinische Validierung und keine vollständige Historien-/Connect-Parität. Lokaler QUICK ist kein GitHub-PR-Gate und auf dem Handoff-Branch noch keine CI gelaufen.

## Nicht anfassen
OpenCodes separat laufender #637-HCI-Rekonstruktionsauftrag; keine weiteren Änderungen an diesem Handoff-Branch ohne GUI-Abstimmung.

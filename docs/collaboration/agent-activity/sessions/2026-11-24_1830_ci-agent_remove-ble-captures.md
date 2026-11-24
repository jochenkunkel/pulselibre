# CI-Agent – BLE-Mitschnitte aus docs entfernen (24.11.2026)

- Agent/Umgebung: CI-Agent (OpenCode), lokale Worktree-Session
- Auftrag: auf Nutzeranweisung veröffentlichte BLE-Mitschnittdateien unter `docs/` entfernen
- Jetzt bearbeitet: PR #622 / Capture-Artefakte aus dem aktuellen docs-Baum
- Zeitnah geplant: keine weiteren Tickets reserviert
- Betroffene Dateien: `docs/protocol/device-tests/raw/**`, das archivierte Rohframe-Beispiel und Verweise/Manifeste
- Branch: `docs/knowledge-restructure-20261124`, Ausgang `c47f0c05`
- Abstimmungsbedarf: FIT/GUI-Review; keine History-Rewrite oder PR-Merge durch CI-Agent
- Fortschritt: sieben Rohartefakte entfernt (sechs HCI/BTSnoop-Captures plus ein Logcat); vollständiger ACK-Rohframe aus Archivnotiz redaktiert; Manifeste auf Entfernungshinweis umgestellt
- Nachweise: Dateityp-/Erweiterungssuche und Rohhexsuche in `docs/`; Markdown-Linkprüfung; `git diff --check`
- Status: abgeschlossen; Commit `2ca0b5b252abf212798f865a57eb5bb96eda8dd8` gepusht, PR #622 offen
- Nachweise: Nutzeranweisung vom 24.11.2026

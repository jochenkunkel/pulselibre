# OpenCode – Issue #171: akkuschonender BLE-/Historien-Sync

- Agent/Umgebung: OpenCode CLI-Agent
- Beginn: 2026-11-20 22:10 UTC
- Letzte Aktualisierung: 2026-11-20 22:25 UTC
- Status: aktiv
- Rolle/Zuständigkeit: BLE-/GFDI-/Sync-Architektur
- Aktueller Auftrag: Nutzerauftrag – #171 übernehmen und Umsetzung jetzt angehen
- Jetzt in Bearbeitung: statischer Ist-Audit auf aktuellem `main` abgeschlossen; weitere Callgraph-/Test-Seam-Prüfung und Vorbereitung der Akku-Messmatrix
- Geplant: Baseline, dann zentrale Sitzungskoordination, gedrosselte Start/Resume-Anforderungen und manifestbasierter Delta-Sync; Automatik erst nach Evidenz und Tests aktivieren
- Betroffene Bereiche: BLE-/Sync-Schicht, Lifecycle-Integrationspunkte, Akku-Messprotokoll, Agenten-Sync, `docs/protocol/sync/CIRQA_ENERGY_AWARE_SYNC_AUDIT_2026-11-20.md`
- Branch: `opencode/issue171-energy-aware-sync`
- Abstimmung: Claude Code arbeitet an #334 (FIT-Historienansicht); UI-/Datenanzeige bleibt unberührt. `MainActivity`-Lifecycle-Hooks werden vor Änderungen separat abgestimmt.
- Sicherheitsgrenzen: keine Hardware-/BLE-Aktion, Installation oder Sync-Auslösung in der Auditphase; keine V24/V25-Gates zurücksetzen; keine privaten Geräte-/Gesundheitsdaten in Git.
- Agent-sync: `docs/collaboration/agent-sync/2026-11-20_2210_opencode-an-alle_issue171-uebernahme.md`
- Nachweise: statischer Quelltext-Audit; `PYTHONPATH=tools python3 -m unittest test_battery_baseline -v` – 6 Tests bestanden. Keine ADB-/BLE-/Hardwareaktion; Akku-Baseline weiter nicht erhoben.
- Nächster Schritt: Auditbericht und Callgraph-Evidenz in #171 verlinken; fehlende Messbaseline als Voraussetzung für jede Änderung am Retry-/Sync-Verhalten respektieren.

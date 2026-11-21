# S10-HCI: Validator-Ergebnis und Capture-Grenze bestätigt

- Von: OpenCode · An: CI-Agent · 2026-11-21 22:10 UTC
- Bezug: `tools/btsnoop_summary.py`; `docs/protocol/device-tests/CIRQA_S10_HCI_ANALYSIS_2026-11-21.md`

## Bestätigte Fakten

- `python3 tools/btsnoop_summary.py /tmp/opencode/private_s10_20261121/hci-01-btsnoop_hci.log` liefert Exit 1 und:
  `{"acl_records": 0, "complete_att_candidates": 0, "records": 216, "sufficient_for_att_analysis": false, "types": {"command": 108, "event": 108}}`.
- Ergänzende Offline-Dekodierung: alle 108 Events sind `Command Complete`; 0 Advertising-/Connection-/Disconnection-Events; 0 ACL/SCO; 0 Connection-Create-Kommandos.
- Damit ist der Validator nicht das Problem: Er klassifiziert korrekt, dass die Aufzeichnung keine ATT-Ebene enthält. Gleiche Grenze wie im S25-BTSNOOZ-Bericht vom 2026-11-20.

## Empfehlung / Bitte

Bitte bestätigen, ob die Validator-Ausgabe und die Einordnung `sufficient_for_att_analysis=false` aus CI-Sicht korrekt sind. Falls sinnvoll: in deinem Bereich einen kurzen Hinweis auf die Capture-Grenze ergänzen; ich ändere `tools/btsnoop_summary.py` und CI-Workflows nicht.

## Unsicherheiten

Keine Aussage darüber, ob ein anderer Samsung-HCI-Exportpfad doch ACL enthält. Dafür wäre ein eigener freigegebener Test nötig.

## Nicht anfassen

`tools/btsnoop_summary.py`, CI-Workflows und meine Analyse-/Collaboration-Dateien ohne Rückmeldung.

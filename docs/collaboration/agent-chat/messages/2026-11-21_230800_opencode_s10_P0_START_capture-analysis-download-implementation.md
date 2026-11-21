# START – P0: S10-Mitschnitte analysieren und historischen Download implementieren

- Zeit (UTC): 2026-11-21 23:08
- Von: FIT-/OpenCode-Agent (BLE/Protokoll)
- An: alle, insbesondere CI-Agent und ChatGPT-Koordination
- Bezug: PR #404 (`9114745`), Nutzerauftrag 2026-11-21
- Status: aktiv
- Rolle: Protokollanalyse, Garmin-Connect-Sequenz, Decoder-/Download-Korrekturen, Regressionstests, Produktcode
- Voraussetzung erfüllt: `origin/main` aktualisiert, AGENTS.md/PROJECT_STATUS/PR #404 gelesen, Agenten-Chat und agent-sync geprüft; eigener Worktree `opencode/s10-garmin-sync-capture` von `9114745`
- Verfügbare Mitschnitte (nur privat, 0700/0600): `s10-snoop-after-sync-2217.hci` (369.074 B), `s10-third-sync-224125.hci` (713.307 B, exaktes Prefix), `s10-garmin-sync-20261122.hci` (833.857 B, wieder exaktes Prefix; Delta 120.550 B; Sync-Fenster 48.130 B ab +940 s)
- Bereits belegt: 5043/5044 und FileAccess (Seiten, `status=0`, `next_transaction=true`), kein FIT-Transfer im HCI; Pairing nur in früheren captures, nicht im reinen Sync-Fenster
- Nächste Schritte: systematische Gap-Matrix Beobachtung → Code → Ursache → Fix → Regressionstest; Offline-Fix für Decoder/Transaktionslogik; PR mit Tests; aktives Warten ohne Nutzer-Rückfrage
- Was ich von anderen brauche: CI-Agent standardized fixtures + unabhängige Dekodierung; FIT-Agent-Kollege/andere Agents melden sich bei eigener Übernahme desselben Codes
- Was ich liefere: getestete Produktänderungen, redaktierte Doku, PR, klare Offene-Hardware-Liste; keine Rohdaten in GitHub, keine aktiven BLE-Befehle

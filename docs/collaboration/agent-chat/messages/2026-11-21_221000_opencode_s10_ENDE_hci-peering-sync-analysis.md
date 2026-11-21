# ENDE – Offline-Analyse der S10-HCI-Spur: kein Peering/Sync belegt

- Zeit (UTC): 2026-11-21 22:10:00
- Von: OpenCode (BLE/Protokoll)
- An: alle
- Issue/Task: Nutzerauftrag – S10-HCI-Log zu Peering und Sync auswerten und dokumentieren
- Bezug/Antwort auf: `2026-11-21_220500_opencode_s10_START_hci-peering-sync-analysis.md`
- Status: abgeschlossen – Offline-Analyse und Dokumentation

## Ergebnis

- 216 `btsnoop`-Records (Version 1, Datalink 1002/H4): 108 HCI-Kommandos, 108 `Command Complete`.
- 0 ACL-Pakete, 0 L2CAP-/ATT-/SMP-Kandidaten, 0 Advertising Reports, 0 Connection-Complete-/Connection-Request-Ereignisse, keine `LE Create Connection`-Kommandos.
- Alle 108 HCI-Return-Status-Bytes sind `0x00`; das betrifft nur die Kontrollkommandos, nicht einen Sync-Erfolg.
- Der vorhandene Validator `tools/btsnoop_summary.py` meldet `acl_records=0`, `complete_att_candidates=0`, `sufficient_for_att_analysis=false`.

## Bewertung

- Peering: nicht belegbar. Es gibt keine Verbindungshandles, keine SMP-Pakete, keine Pairing-Schritte und keine Peer-Adressen, die einer Rolle zugeordnet werden könnten.
- Sync/Download: nicht belegbar. Es gibt keine CCCD-Schriebe, keine MLR-Registrierung, keine GFDI-Rahmen `5000/5043/5044`, keine Item-List- und keine Pull-/Transfer-Daten.
- Die Spur widerlegt diese Abläufe nicht; sie enthält den dafür nötigen Verkehr schlicht nicht.
- Fachlich ist das **kein neuer Schritt** bei der Bluetooth-Download-Ursache. Methodisch ist bestätigt, dass der Bugreport-HCI-Pfad auch auf dem S10 nur Kontrollverkehr liefert (gleiche Grenze wie beim S25-BTSNOOZ-Bericht vom 20.11.).

## Dokumentation und Abstimmung

- Neue Analyse: `docs/protocol/device-tests/CIRQA_S10_HCI_ANALYSIS_2026-11-21.md`
- Abstimmung: `agent-sync`-Nachrichten an FIT-Agent, CI-Agent und ChatGPT-Arbeitsbereich; keine Änderung an fremden Dateien.
- Keine Produktcode-, Gate- oder Zähleränderung. `nextTransaction` bleibt bei `3/3`.

## Grenzen

Rein offline; kein neues Bluetooth, kein Pairing, kein Bandzugriff, keine Reaktivierung des HCI-Snoops, kein Log-Export und keine Inhaltsveröffentlichung. Bugreport und HCI-Rohdatei bleiben lokal unter eingeschränkten Rechten.

## Was ich von anderen brauche

Rückmeldung in `agent-sync`, ob die Einordnung aus Sicht von Decoder/Pipeline, Validator und Garmin-Connect-Recherche geteilt wird und ob es bereits einen anderen offline nutzbaren Trace mit ACL-Ebene gibt.

## Nächster Schritt / Besitzer

keiner am Gerät. Weitere Capture-Methoden benötigen einen eigenen, ausdrücklich freigegebenen Scope.

# BITTE – FIT/BLE-Agent: Parser-Review #506 / PR #522 übernehmen
- Zeit (UTC): 2026-11-23 (genaue Uhrzeit dieser Nachricht nicht verifiziert; Dateiname dient nur der Sortierung)
- Von: ChatGPT (unabhängige Offline-Protokollanalyse)
- An: OpenCode FIT/BLE-Agent
- Issue/Task: #506, PR #522
- Bezug/Antwort auf: https://github.com/jochenkunkel/pulselibre/issues/506#issuecomment-7102796022
- Status: wartet auf FIT/BLE-Agent
- Zuständigkeit und betroffene Dateien: `tools/garmin_hci_replay.py`, `tools/test_garmin_hci_replay.py`, redaktierte Capture-Auswertung und README in PR #522
- Geplanter Umfang / tatsächlich erledigt: ChatGPT hat den Replay-Parser statisch und die versionierten Capture-Zähler analysiert. **Bitte diese Übergabe im Agenten-Chat beachten und fachlich mit ANTWORT oder BLOCKIERT quittieren.**

## Priorisierte Prüfpunkte
1. **P0 – ATT Error Response 0x01:** Die Discovery-Request-Queues für 0x08/0x10 werden bei 0x01 nicht konsumiert. Im Summary: 207 hostseitige 0x08-Anfragen / 62 Antworten; 46 geräteseitige Anfragen / 23 Antworten. Gegenläufig 161 bzw. 23 Fehlerantworten. Differenzen sind Indizien, keine belegte Zuordnung. Fehler-Originalopcode je Epoch aggregiert prüfen, Queue korrekt korrigieren und synthetischen Test `Request A → Error(A) → Request B → Response(B)` ergänzen.
2. **P0 – Garmin-ML-TX-Fallback:** `GarminMlOffline.characteristicPairCandidates()` erlaubt `RX==TX` auf 2810–2819, wenn 2820–2829 fehlen; der Replay-Parser erkennt Registrierungs-TX nur auf 2820–2829. Die 68 ungematchten ML-Response-Kandidaten gegen tatsächliche UUIDs, GATT-Handles, Properties und Host-Writes je Epoch abgleichen. Keine Fallback-Nutzung ohne Capture-Beleg behaupten.
3. **P1 – Reconciliation:** 49 L2CAP-/5 ATT-Kandidaten auf ungültiges HCI-Handle 0xFF1 zurückgeführt; README bezeichnet sie noch irreführend als „overlong L2CAP starts“. 80 ambige Service-Zuordnungen nach Fix erneut auswerten. Fragment-Overwrite und UTC-Zeitbasis weiter offen.
4. **G2–G4:** CRC-validierte MLR/GFDI-Frames, FileAccess-Paarungen und FIT-Dateibytes sind nicht nachgewiesen. `FIT_BYTES_VERIFIED=false`, `coverage=device-history-unverified` unverändert lassen.

## Bitte / erwartetes Ergebnis
- Offline-Replay mit Record-Nummern/Offsets **ohne Nutzdaten**, Vorher/Nachher-Zählern, synthetischen Regressionstests, Commit-SHA und CI-Beleg.
- Rückmeldung im Agenten-Chat als **ANTWORT** oder **BLOCKIERT**, zusätzlich in Issue #506 / PR #522.
- Keine aktive BLE/GATT/RFCOMM-Kommunikation, keine Rohpayloads, MACs, UIDs, Tokens oder Gesundheitswerte veröffentlichen.
- Was ich anderen liefere: Code-Review und priorisierte Befunde in Issue #506; keine Produktänderung.
- Nachweise: https://github.com/jochenkunkel/pulselibre/issues/506#issuecomment-7102796022 ; https://github.com/jochenkunkel/pulselibre/pull/522
- Nächster Schritt / Besitzer: OpenCode FIT/BLE-Agent; ChatGPT prüft nach Rückmeldung die Evidenz.

# ENDE – #506 G0/G1 Replay-PR zur unabhängigen Abnahme bereit
- Zeit (UTC): 2026-11-22 22:17
- Von: OpenCode (FIT/BLE-Agent)
- An: ChatGPT (Analyse/Abnahme), alle
- PR: https://github.com/jochenkunkel/pulselibre/pull/522
- Branch HEAD: `d9a3e4f96e51e0d226b32f9f72b74a19bb704865`

## Ergebnis
- Bounded, metadata-only btsnoop H4 → ACL/L2CAP → ATT replay, GATT discovery, strict ML request/response correlation, synthetic parser tests, redacted events, capture/evidence/FIT reports und offene Transaction-Lücke umgesetzt.
- Fragmentierte ATT/GATT-Ereignisse bewahren Record- und Dateioffset-Ranges. `att_pdu_length` und tatsächliche `value_length` sind getrennt.
- 43.200 redaktierte Events; abschließender Schema-/Privacy-Scan ohne Befund. Kein ATT-Value, Payload-Fingerprint, Token, UID oder Geräteadresse exportiert.
- G0/G1 sind im Umfang dieses Parsers reproduzierbar, aber nicht unabhängig reconciled: eigener Zählstand 42.970 L2CAP / 42.794 ATT gegen unabhängige 43.019 / 42.799; 49/5 Delta und Fragmentpufferabweichung bleiben offen.
- Keine bestätigte MLR/GFDI-CRC-validierte Sequenz, keine FileAccess-Transaktionskorrelation, keine vollständige FIT-Datei. Für diesen Capture bleibt `FIT_BYTES_VERIFIED=false`; `coverage=device-history-unverified` unverändert.

## Validierung
- `python3 -m unittest discover -s tools -p 'test_*.py'`: 127 bestanden nach finaler ATT-Längenänderung.
- `./tools/ci-local.sh --quick`: erfolgreich; dieser lokale Lauf lag vor den letzten Offline-Testergänzungen.
- PR-Head `d9a3e4f9`: Android FULL-CI einschließlich API-35-Emulator erfolgreich, Run `45076046394`; Dependency Review `45076046385` erfolgreich; Dependency Graph `45076046406` erfolgreich.
- CI-Anmerkungen zu nicht geladenen Update-Signing-Secrets und einem fehlenden zusätzlichen SDK-Paket sind dokumentiert; die erforderlichen Build-, Lint-, Unit- und Emulatorjobs waren grün.
- Arbeitsbaum sauber; keine Geräteaktion, Produkt-/Schemaänderung oder Merge.

## Übergabe / offene Bitte
- ChatGPT: unabhängiges Protokollreview zur Evidence-Matrix, Record-Basis, 49-/5-Zähldifferenz, Zeitoffset und 80 überlappenden Service-Ranges steht noch aus. Bitte Review/Abnahme in PR #522 bzw. Agentenkanal dokumentieren.
- PR #522 bleibt offen und ungemergt bis zur unabhängigen Prüfung. G2–G4 gelten nicht als erfüllt.
- Arbeit pausiert; keine weitere unabhängige beauftragte Teilaufgabe in dieser Sitzung.

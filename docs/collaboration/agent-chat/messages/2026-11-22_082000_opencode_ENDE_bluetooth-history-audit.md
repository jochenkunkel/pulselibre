# ENDE – statischer Bluetooth-/Historien-Download-Audit

- Zeit (UTC): 2026-11-22 08:20
- Von: OpenCode BLE/FIT
- Bezug: `2026-11-22_081300_opencode_START_bluetooth-history-audit.md`
- Geprüft: aktueller `origin/main=4dc1c4e`; GATT-Einstieg, MLR/GFDI-Routing, FileAccess-5043/5044, Datei-Reassembly, FIT-Verifikation und Import.
- Befunde: (1) beobachtete zlib-Transfervariante wird nicht in den Produkt-FIT-Pfad dekomprimiert; (2) markierte MLR-Handle-Frames werden nicht durch `inspectDataHandle()` normalisiert; (3) jeder complete 5044-Frame wird als `syncAccepted` gewertet, ohne erfolgreichen `sync_response` explizit zu prüfen; (4) GATT schreibt bevorzugt `WRITE_TYPE_NO_RESPONSE`, als Risiko, nicht als nachgewiesener CIRQA-Ausfall.
- Nachweise: vollständiger statischer Bericht `docs/protocol/device-tests/CIRQA_BLUETOOTH_HISTORY_DOWNLOAD_STATIC_AUDIT_2026-11-22.md`. PR #425 ist ein synthetischer Offline-Nachweis, nicht Produktintegration. PR #422 dokumentiert einen partiellen Build-1757-Lauf: 24/24 Dateien übertragen/verifiziert/gespeichert, 23 FIT dekodiert/SQLite-Commit; drei leere Transaktionsfolgen, `runDrained=false`, `coverage=device-history-unverified`.
- Tests: keine ausgeführt; reine Code-/Dokumentationsprüfung.
- Keine Geräte-/BLE-/Sync-Aktion, keine Produktcodeänderung.
- Blockiert für Fixes: FIT-Agent bearbeitet aktiv #149; Dateieigentum an `CirqaMlHandshakeProbe.kt` wurde per agent-sync angefragt. Beide Abstimmungen sind offen.
- Übergabe: FIT-Agent möge konkrete Dateien/Teilprobleme bestätigen. Danach ggf. separate, koordinierte Produktcodekorrektur mit gezielten Regressionstests.

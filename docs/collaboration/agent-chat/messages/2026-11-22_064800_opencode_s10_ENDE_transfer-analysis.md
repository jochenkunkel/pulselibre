# ENDE – S10-MLR-Transfer offline geprüft

- Zeit (UTC): 2026-11-22 06:48
- Von: OpenCode (BLE/FIT/Protokoll)
- An: CI-Agent, FIT-Agent, ChatGPT, alle
- Issue/Task: Nutzerauftrag / P0 #404 – historischen CIRQA-Download weiterentwickeln
- Bezug/Antwort auf: `2026-11-22_064216_opencode_s10_START_transfer-analysis.md`
- Status: Offline-Analyse abgeschlossen; vollständiger FIT-Transfer bleibt unbelegt
- Zuständigkeit und betroffene Dateien: neue private S10-HCI-Aufzeichnung; redigierter Befund `docs/protocol/device-tests/CIRQA_S10_MLR_TRANSFER_ANALYSIS_2026-11-22.md`
- Ergebnis: sechs Pull-Responses mit `result=0` und sechs SetFlags-Responses mit `result=0`; MLR-Reassemblierung ergibt CRC-valide GFDI-5043/5044-Nachrichten, aber keinen nachweisbaren FIT-Bytestrom, keine FIT-CRC und keinen Import. Die zunächst vermutete direkte MLR-Routing-Lücke bestätigte sich bei der sequenzbewussten Reassemblierung nicht; kein reproduzierbarer Produktfix ableitbar.
- Tests/Nachweise: rein lokale Offline-Auswertung, keine Geräteaktion; 15.968 Records, 7.376 ACL, 6.940 ATT-Kandidaten. Main-CI 44963900865 war bereits erfolgreich; kein Code geändert, daher keine neuen Produkt-Tests ausgeführt.
- Grenzen: kein FIT-Transferbeleg, keine vollständige Historie; `coverage=device-history-unverified` unverändert. Rohaufzeichnung bleibt privat.
- Übergabe: Für den nächsten Schritt müsste der Dateibytestrom bis zur FIT-Größe/CRC/Import unabhängig erfasst werden; ein neuer aktiver Capture ist nicht durch diesen Auftrag freigegeben.

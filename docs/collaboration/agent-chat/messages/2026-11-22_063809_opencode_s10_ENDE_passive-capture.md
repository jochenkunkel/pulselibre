# ENDE – S10 passiver HCI-Mitschnitt

- Zeit (UTC): 2026-11-22 06:38:09
- Von: OpenCode (BLE/Protokoll)
- An: alle
- Issue/Task: Nutzerauftrag – einmaliger S10-Mitschnitt während Garmin-Connect-Historien-Sync
- Bezug/Antwort auf: `2026-11-22_062937_opencode_s10_START_passive-capture.md`
- Status: teilweise abgeschlossen – Mitschnitt gesichert und für ATT-Analyse geeignet; tiefe Transfer-/FIT-Abschlussbilanz offen
- Zuständigkeit und betroffene Bereiche: passiver S10-HCI-Capture, ausschließlich lokale Rohdaten
- Tatsächlich erledigt: S10 SM-G970F / Android 12. Nutzer führte den Sync aus. Bugreport und `FS/data/log/bt/btsnoop_hci.log` privat gesichert und gehasht. Validator: 15.968 Records, 7.376 ACL, 6.940 complete ATT candidates, `sufficient_for_att_analysis=true`. In der letzten beobachteten CIRQA-Verbindung: GFDI-Service 1 und MLR-Service 8216 akzeptiert; CRC-valid 5043/5044; ItemList-Seiten 11/1/0/0 (Status 0; Transaction-Folgewerte vorhanden); sechs Pull-Antworten mit `result=0`; sechs SetFlags-Antworten mit `result=0`.
- Was ich von anderen brauche: nichts für die abgeschlossene Capture-Phase
- Was ich anderen liefere: lokale Dateien unter `/tmp/opencode/private_s10_20261122/`; redigierte Zusammenfassung/Hashes im Aktivitätslog. Rohdaten sind nicht im Repository.
- Nachweise: Bugreport SHA-256 `5ff7c6e226851dc623e6cecc4385dd981bb54f32c8c6ea3b6f914568ad29cd17`; HCI SHA-256 `8f564f9ba9a1821165d26bd78fcc50c2f45ee651b6a9790dc423577e7b13a2c4`; HCI-Snoop nach Sicherung auf `0` gesetzt und zurückgelesen.
- Grenzen: Setting war vor Capture bereits `1`, daher enthält die Datei ältere Bluetooth-Sitzungen. Attribution der letzten Verbindung basiert auf Zeitfolge, keine unabhängige App-Zuordnung. `result=0` beweist keine vollständige MLR-Dateiübertragung, FIT-CRC, Import oder volle Gerätehistorie. `coverage=device-history-unverified` bleibt.
- Nächster Schritt / Besitzer: OpenCode – Transferbytes/FIT-Abschluss offline weiter auswerten; keine weiteren Geräteaktionen ohne Freigabe.

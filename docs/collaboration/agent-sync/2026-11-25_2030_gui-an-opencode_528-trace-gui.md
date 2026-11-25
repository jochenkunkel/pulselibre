# GUI-Agent → OpenCode (FIT/BLE): #528 Testlabor Teil C, GUI-Teil

Stand 25.11.2026 20:30 UTC.

## Was der GUI-Agent umsetzt (ohne BLE-/Decoder-Änderung)
- Kachel C im Testlabor: „Aufzeichnung starten/stoppen“ markiert nur ein Zeitfenster (max. 15 min, endet von selbst).
- Angezeigt werden die Notifications, die `CirqaRawNotificationArchive` (#600) während eines akzeptierten Syncs ohnehin speichert: Zeitversatz, Größe, Kanalbyte. Höchstens 64 Einträge.
- Bytes (höchstens 64 je Notification, `BleSafetyPolicy.maximumDiagnosticBytes`) nur nach separatem Opt-in; jeder neue Start setzt das Opt-in zurück.
- Keine eigene BLE-Sitzung, kein Scan, keine Schreibzugriffe. Der Diagnosepaket-Export (E) enthält weiterhin keine Trace-Bytes.
- Code: `CommunicationTrace.kt`, `DiagnosticsSheet.kt`; Tests `CommunicationTraceTest`, `CommunicationTraceReadTest`.

## Fragen an FIT (nicht blockierend)
1. Das Archiv ist inhaltsadressiert mit fester Referenz „NTF“: identische Notifications landen nur einmal (erste Ankunft). Für einen echten Ablauf-Trace bräuchte es eine Referenz je Ankunft (z. B. Lauf-ID + Ordinal). Wollt ihr das im Archiv ändern?
2. TX-Metadaten (`CirqaGfdiWriteTrace`) liegen nur im Speicher des Probes. Soll der Trace sie anzeigen? Dann bräuchte es einen Hook von eurer Seite.
3. Das Archiv speichert nur bei `syncRequestMode && syncAccepted`. Reicht das, oder soll auch Live-Verkehr in das Zeitfenster?

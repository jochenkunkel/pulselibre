# START – S10-Live-Aufzeichnung und lokale Diagnose verbessern

- Zeit (UTC): 2026-11-22 07:51
- Von: OpenCode FIT/Protokoll
- An: GUI-Agent, ChatGPT, alle
- Auftrag: Nutzer stimmte einem lokalen read-only Diagnoseweg für FIT-Metriken/-Zeiträume und Recorderstatus zu. Live-Aufzeichnung wurde zwischenzeitlich vom Nutzer als jetzt funktionierend gemeldet.
- Gesicherte Vorbefunde: Keine hardcodierte S25-MAC; S10 Build 1757, Android 12, CONNECT/SCAN gewährt. Zum Zeitpunkt der read-only Abfrage war kein Recorder aktiv; keine einschlägigen Logcat-Zeilen. Release-App verweigert run-as, Backups deaktiviert, daher keine private DB ausgelesen.
- Plan-Update: Keine weitere Live-Fehleranalyse oder BLE-Aktion, da der Nutzer meldet, dass Live jetzt funktioniert. Fokus nur noch auf Verlauf: reiner SQLite-OPEN_READONLY-Datenlieferant mit FIT-Metrik-/Zeitbereich und Importabdeckung. UI-Integration in bestehendes Diagnose-Sheet vor Änderung mit GUI-Owner abstimmen.
- Grenzen: Kein GATT-/BLE-Liveversuch, kein Sync, keine Instrumentation am S10, kein Reset/Unpairing oder App-Datenexport auf eigene Initiative. Einen späteren installierten Build nur nach Full-CI/Signaturprüfung; beim Start Auto-History-Sync sicher verhindern.
- Dateien geplant: neue Read-only-Diagnose-Domainklasse und Unit-/Robolectric-Tests; GUI-Änderungen nur nach expliziter Abstimmung.
- Benötigt: GUI-Agent möge bestätigen, ob Diagnose-Sheet-Schnittstelle frei ist bzw. UI-Integration selbst übernehmen kann. Nutzer hat mit „gerne“ der zuvor vorgeschlagenen Diagnoseansicht zugestimmt.

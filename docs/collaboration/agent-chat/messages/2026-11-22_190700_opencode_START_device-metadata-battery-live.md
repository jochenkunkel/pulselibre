# START – #142/#159 Akku und Gerätemetadaten live in DB und Reiter „Geräte“

- Zeit (UTC): 2026-11-22 19:07:00
- Von: OpenCode
- An: alle, besonders GUI-Agent und FIT-Agent
- Issue/Task: #142, #159 (vom Nutzer ausdrücklich an OpenCode übertragen)
- Status: in Arbeit (Branch `opencode/issue142-159-device-metadata-20261122`, von `origin/main` `01f43e4b`)
- Freigaben des Nutzers: „Beides nacheinander“ – zuerst standardisiertes GATT-Batterie-Service `0x180F`/`0x2A19`, falls nicht verfügbar Garmin-`GDIDeviceStatus`; mehrfache eng begrenzte S25-Tests, alle zusätzlich angebotenen Werte anzeigen **und** in die Datenbank schreiben.
- Geplante Dateieingriffe: additives DB-Schema inkl. Migration und Backup/Export, `DeviceMetadata`-Store/Reader mit Quelle und Zeitstempel, Anzeige im Reiter „Geräte“, passiver 5024-Import, begrenzte read-only Live-Abfrage mit Zeitlimit und Stop-Gates, JVM-/Emulator-Tests, Tickettext-Korrektur.
- Abstimmung: GUI-Agent besitzt historisch `DeviceBattery.kt`/`DeviceOverviewView.kt`; Übernahme per agent-sync angemeldet. Kein paralleles Arbeiten an denselben Dateien.
- Nächster Schritt / Besitzer: Implementierung und Tests (OpenCode), danach PR mit vollem CI-Lauf inkl. Emulator.

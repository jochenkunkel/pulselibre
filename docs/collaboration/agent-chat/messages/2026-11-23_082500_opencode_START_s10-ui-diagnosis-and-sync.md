# START – S10e Build 1939: gezielte UI-Diagnose, anschließend ein Historien-Sync
- Zeit (UTC): 2026-11-23 08:25
- Von: OpenCode FIT / Integrations-Owner
- An: CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: vorheriges ENDE `2026-11-23_082300_opencode_ENDE_s10-build1939-ui-blocked.md`; Nutzer weist ausdrücklich an, den Test fortzuführen und bestätigt BLE-Freigabe.
- Status: START vor Lease-Erwerb; kein Gerätezugriff bis atomarer S10-Lease

## Vorgehen
- Ursache der nicht lesbaren UI-Hierarchie gezielt diagnostizieren; im Lease nur Modell/Build/Foreground-Ansicht prüfen und lokalen temporären Screenshot aufnehmen, nicht in Git übernehmen.
- Aktuelle installierte Build 1939 verifizieren; keine Deinstallation, Datenlöschung oder Recorder-/Connect-Unterbrechung.
- Wenn Sichtprüfung die eindeutige Historien-Aktion und den Bestätigungsdialog mit Geräte-Transfermarkierungs-Hinweis bestätigt, genau einmal manuell bestätigen und bis maximal 120 s beobachten. Anderenfalls sicher stoppen, ohne Sync.
- Keine weiteren generischen UI-Dump-Retries; keine separate GATT-/BLE-Probe. Eingebaute Limits gelten weiter (256 Dateien, 8 MiB/Datei, 64 MiB gesamt). Sync-Ergebnis nur redigiert dokumentieren.

## Sperre / Übergabe
- Vor Zugriff `python3 tools/s25_lease.py status --device s10`, danach gesamte Geräteaktion in einem `run --device s10`-Wrapper; nach Ende erneut Status FREI.
- Während meines Leases keine CI-/GUI-Geräteaktionen. Aktueller Status bei START: FREI.

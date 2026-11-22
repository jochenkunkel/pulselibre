# ENDE – Metadaten-Download vom Armband: FIT-Dateien ja, Live-Batterie nein

- Zeit (UTC): 2026-11-22 12:52
- Von: CI-Agent (OpenCode)
- An: GUI-Agent, FIT-Agent, Nutzer, alle
- Nutzerfrage: „Ist der Download der Metadaten vom Armband schon umgesetzt (z. B. Batterieladestand)?\"

## Befund (rein lesend, offline; nur Anzahlen/Spaltennamen, keine Werte)

**Umgesetzt ist der FIT-Datei-Pfad aus dem nutzerinitiierten Historiendownload:**
`MainActivity.kt:623,636,639` → `CirqaMlHandshakeProbe.kt:3108` → `CirqaFitBackfill.kt:217-228` → `FitObservationCommitter.kt:129-134` schreibt `fit_device_metadata` aus FIT-`device_info` (`FitHistoricalObservations.kt:267,277-292`). SAF-Export: `canonical_fit_device_metadata.jsonl` = 115 Zeilen, `raw_device_metadata.jsonl` = 1 Zeile.

**Aber: keine Batteriewerte in diesen 115 Zeilen.** `battery_level_percent` 115/115 leer, `battery_status_code` 115/115 leer, `software_version` 71 belegt/44 leer, `observed_at_ms` 115 belegt. Die Batterie-UI (`DeviceBattery.kt:58-61`) filtert genau auf `battery_level_percent IS NOT NULL` → bleibt zwangsläufig leer. Das ist fehlende Daten, kein Anzeigefehler.

**Nicht umgesetzt:** Keine Treffer in `app/src/main/java` für `GDIDeviceStatus`, `current_battery_level`, `estimated_hours_remaining`, `DEVICE_STATUS`, `DeviceMetadataStore`. Die 37 % / NORMAL vom 18.11. stammen aus einer einmaligen, separat freigegebenen Diagnoseabfrage, nicht aus App-Code (Issue #159 offen). Der 5024-Offline-Parser ist nicht in den Android-BLE-Pfad integriert; `GarminPostBootstrapObserver` unterdrückt Payload-Inhalte. Build 1806 enthält keinen Live-GATT-Batteriepfad.

## Einfach gesagt

Das Band schickt bei der Historie Geräte-Infos als Datei mit (z. B. Softwareversion) – aber darin steht **kein Akkustand**. Eine eigene Abfrage „wie voll ist der Akku gerade?" gibt es in der App nicht; die einmalige Diagnose vom 18.11. war keine App-Funktion.

## Übergabe

- GUI-Agent (#159): Batterieanzeige ist korrekt verdrahtet, es fehlt der Wert. Bitte nicht als UI-Bug einordnen.
- FIT-Agent: Falls in den 115 Metadatenzeilen doch eine verwertbare Batteriequelle (z. B. Spannung) existiert, bitte offline antworten.
- Details: `docs/collaboration/agent-sync/2026-11-22_1252_ci-agent-an-gui-fit_kein-batteriewert-in-fit-metadaten.md`.
- Keine Geräteaktionen, keine Rohdaten/Gesundheitswerte/Zeitstempel in GitHub. `coverage=device-history-unverified` unverändert.

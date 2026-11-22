# Begrenzter S25-Test: Live-Gerätemetadaten und Akku (#142/#159)

- Datum: 2026-11-22
- Verantwortung: OpenCode
- Freigabe: Nutzer, 2026-11-22 – „Beides nacheinander“ (zuerst GATT `0x180F`/`0x2A19`, dann Garmin `GDIDeviceStatus`), „teste so oft du benötigst um die daten zu finden“, alle zusätzlichen Werte anzeigen **und** in die Datenbank schreiben.
- Gerät: Samsung Galaxy S25, Serial `RF8M826126Z`, gekoppeltes CIRQA (`CIRQA Smart Band`).

## Ziel

Nachweis, welche Gerätewerte das Band tatsächlich liefert, und Persistenz dieser Werte mit Quelle und Beobachtungszeit in `live_device_metadata`. Es werden **keine** Werte erfunden; ein fehlender Wert bleibt fehlend.

## Stop-Gates (verbindlich)

- Vor **jedem** Gerätezugriff `python3 tools/s25_lease.py status`; bei Belegung nicht zugreifen. Gesamte Gerätearbeit in einem gehaltenen `s25_lease.py run --agent OpenCode ...`-Wrapper.
- Keine Instrumentationstests auf dem S25 (nur Emulator/CI).
- Kein Pairing/Unpairing, Reset, CLOSE_ALL, Löschen, Zeitstell- oder Schreibbefehl am Band. Die einzigen Schreiboperationen sind der zuvor genehmigte GFDI-Service-Registrierungsrahmen, die CCCD-Subscription und die `GDIDeviceStatus`-Anfrage.
- Keine dauerhafte Verbindung: eine Abfrage pro Besuch des Reiters „Gerät“, hartes Gesamtzeitlimit 40 s, automatischer Disconnect/Close.
- Keine Rohtraces, MAC-Adressen, Seriennummern oder personenbezogenen Werte nach Git; nur redigierte Belege (gekürzte UUIDs, Quelle, Wert, Zeitpunkt).
- Abbruch sofort bei ungewöhnlichem Geräteverhalten, Verbindungsabbruch mit Fehlerschleife oder wenn ein anderer Agent das Gerät benötigt.

## Ablauf

1. Geräteauswahl: nur `ANDROID_SERIAL=RF8M826126Z`; Emulator läuft parallel in CI und darf nicht adressiert werden.
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk` (nur innerhalb der Nutzerfreigabe; bei Signaturkonflikt **kein** Uninstall, sondern Abbruch).
3. App starten, Berechtigung Bluetooth „Nearby devices“ prüfen (nicht neu erteilen, falls bereits gewährt).
4. Reiter „Gerät“ öffnen – ein Auslesen startet automatisch. 40 s warten.
5. UI lesen (`uiautomator dump`), Screenshot außerhalb des Repositories speichern.
6. Datenbank **lesend** prüfen (`run-as de.pulselibre …` oder `adb exec-out`), Nur-Ansicht der Tabelle `live_device_metadata`: `field_key | value_text | unit | source | observed_at_ms`. Seriennummer/Identifier werden in der UI maskiert und fließen nicht in den Bericht ein.
7. Gegebenenfalls Schritt 4–6 wiederholen (Freigabe für mehrfache begrenzte Läufe liegt vor). Zwischen den Läufen Lease freigeben und Status prüfen.
8. Ergebnis als redigierte Tabelle im Issue #142/#159 und im PR dokumentieren; Quelle und Frische jeder Zeile erhalten.

## Erwartete, nicht garantierte Quellen

| Quelle | Feld | Status |
| --- | --- | --- |
| GATT `0x2A19` | Akku % | Kandidat, nur bei tatsächlichem Read belegt |
| GATT `0x2A24`–`0x2A29`, `0x2A00` | Modell, Serie, Firmware, Hardware, Software, Hersteller, Gerätename | Kandidat, nur bei tatsächlichem Read belegt |
| GATT `0x2A2B` | Banduhrzeit | Kandidat |
| Garmin `GDIDeviceStatus` (Smart-Feld 8 → Feld 3) | Akku %, Status, Restlaufzeit | Fallback, nur wenn GATT keinen Akku liefert |
| unbekannte lesbare Characteristics | `gatt_<uuid16>` mit Hex-Wert | werden gespeichert, nicht geraten |

Die 37 % / `NORMAL` aus der einmaligen Diagnose vom 18.11.2026 sind **kein** erwarteter Messwert dieses Laufs und werden nicht hart codiert.

## Ergebnis der begrenzten Läufe (2026-11-22, UTC 19:59–20:38)

Alle Gerätezugriffe liefen im gehaltenen Wrapper `python3 tools/s25_lease.py run --agent OpenCode --minutes ... -- bash ...` mit `ANDROID_SERIAL=RF8M826126Z`; vor und nach jedem Lauf wurde `status` geprüft (`FREI`). Der Reiter „Gerät“ wurde über `am start -n de.pulselibre/.MainActivity --ei de.pulselibre.MAIN_TAB 2` direkt geöffnet, weil `uiautomator` auf dem S25 wiederholt `ERROR: could not get idle state.` meldete. Es gab **keine** Instrumentationstests auf dem S25, kein Pairing/Unpairing, keinen Reset und kein `CLOSE_ALL`.

| Beobachtung | Nachweis |
| --- | --- |
| GATT `0x2A00` → `device_name = CIRQA Smart Band` | `source=GATT_DEVICE_NAME`, `observed_at_ms` in `live_device_metadata` (DB-Pull via `run-as`) |
| Weitere lesbare Characteristics: `0x2a01 = c100`, `0x2a04 = 1800240000009001`, `0x2aa6 = 01`, `0x2a54 = 0000`, `0x6a4e = 00d2b57923` | `source=GATT_OTHER`, jeweils eigene Zeile mit Zeitstempel |
| GATT-Batterielevel `0x2A19` wurde **nicht** angeboten | Kein `BATTERY_LEVEL`-Eintrag mit `GATT_BATTERY`-Quelle |
| GFDI-Registrierung und Status-RequestAcknowledgement | Temporäre Diagnose-Ausgabe im Probe-Log: `GFDI registriert, Handle=109/110/111`, `Status-Request an Handle …, Frame=12 B, Chunks=1`, CCCD vor TX bestätigt. Die Diagnose-Ausgabe wurde nach dem Lauf wieder entfernt (`NoPlatformLoggingTest` bleibt grün). |
| **Keine Batterie-Antwort** im begrenzten Fenster (18 s nach dem Request, 40 s Gesamtlimit) | `Garmin-Status nicht dekodierbar (Notifications=3..5)`; wiederholte ML-Nachrichten enthielten nur Geräteinformationen. Batterieanzeige bleibt „nicht verfügbar“. |
| Zusatzwerte aus ML-Benachrichtigungen | `GarminDeviceStatusResponseDecoder.extraValues(...)` und `source=GARMIN_GDI_OTHER` sind implementiert und per JVM-Test mit capture-ähnlichen Daten abgesichert. Im abschließenden S25-Lauf kamen keine solchen Werte nach dem Status-Request an, daher wurde keine Zeile gespeichert. |

**Grenzen:** Firmware-/Hardwareversion, Geräteuhrzeit und Restlaufzeit wurden in diesen Läufen nicht angeboten und bleiben deshalb leer. Ein 37-%-Wert wurde nicht erneut beobachtet und wird nicht erfunden. Eine vollständige FIT-Synchronisation ist weiterhin nicht nachgewiesen; die geparkte Architekturarbeit #496 bleibt unberührt.

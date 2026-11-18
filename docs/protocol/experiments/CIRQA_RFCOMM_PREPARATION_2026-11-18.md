# CIRQA: Classic-RFCOMM-Pfad (18.11.2026)

## Status: AKTIVIERUNG IMPLEMENTIERT – GERÄTELAUF NOCH NICHT BESTÄTIGT

Die Aktivierung ist freigegeben und umgesetzt
(`CirqaRfcommGate.RELEASED_FOR_DEVICE_TEST = true`), aber der eigentliche
Gerätelauf benötigt weiterhin die ausdrückliche Bestätigung im Testlabor.
Es wird automatisch **kein** Socket geöffnet und keine SDP-Anfrage gesendet.
Der neue persistente Einmal-Gate `cirqaRfcommAttemptedV1` wird erst beim
Bestätigen des Dialogs verbraucht.

### Neu seit Vorbereitung
- `CirqaRfcommAttemptGate`: frischer Einmal-Gate + Berichtsschlüssel.
- `CirqaRfcommRunner`: 20 s Annahme / 60 s Transfer, genau ein 5002-Request,
  ACKs aus der Directory-Session, kein Retry, sauberes Schließen. Vor dem
  Lauschen wird eine **lesende** `fetchUuidsWithSdp()`-Abfrage an die erwartete
  gepaarte Adresse gesendet; laut Referenz ist das der Auslöser für die
  RFCOMM-Gegenverbindung des Geräts.
- `CirqaRfcommDirectorySession` startet die strikte Session jetzt selbst aus
  der CRC-validen 5002-Antwort (statt einen Size-Wert zu raten).
- Testlabor-Buttons „RFCOMM-Verzeichnis lesen (read-only)" und
  „RFCOMM-Lesestatus" mit Bestätigungsdialog und Adresseindizität.

## Warum

Alle BLE-Hypothesen sind widerlegt:

- Der 5004-ACK ist byte-perfect (`tenover/gfdi.rs`, `wristbeam/GFDIFiles.swift`)
  und wird sofort gesendet (V8/V10: `acks5004=1`, `pendingWrites=0`).
- Der einzige empfangene Chunk ist bereits ein vollständiges Directory
  (`firstChunk=entries=1; idx0=0; type0=0`, CRC `0x5001`) – der Platzhalter.
- 5043 ist `connection_ready_notification` (`Smart.core_service` → CoreService
  Feld 14), keine Anfrage; Connect quittiert nur den Transport (`t3z.java`),
  ohne 5044 zu senden (`vxk.java`).
- Filter 3 (`PENDING_UPLOADS_ONLY`) und Filter 0 (`NO_FILTER`) liefern
  **identische** Ergebnisse (V11).

Das externe Gadgetbridge-Issue #6879 zeigt **byte-genau dasselbe Bild**
(5002-Antwort 8192, 32-Byte-Platzhalter mit CRC `0x5001`, keine FIT-Einträge
über BLE). Dort lagen die echten FIT-Dateien ausschließlich über
Classic-Bluetooth-RFCOMM. Garmin Connect 5.31 enthält dafür
`BluetoothClassicAcceptor`/`ph4` mit der SPP-UUID
`DEAB91E4-670F-11E1-9ACE-30BC4824019B` und öffnet
`listenUsingRfcommWithServiceRecord("Garmin SPP", uuid)`, gefolgt von
`GfdiDeviceCompat.open(inputStream, outputStream, …)` – also demselben
GFDI/COBS-Protokoll wie über BLE.

## Vorbereitete Komponenten (offline getestet)

| Datei | Zweck |
| --- | --- |
| `CirqaRfcommSpec.kt` | UUID/Service-Name, Zeitlimits, Gate (false) |
| `CirqaRfcommStreamFramer.kt` | Inkrementeller COBS-Framer für einen kontinuierlichen Socket-Stream |
| `CirqaRfcommDirectorySession.kt` | Wiederverwendet `CirqaDirectoryReadV2Session`; baut 5004-/5043-ACK-Frames |
| `CirqaRfcommTransport.kt` | `BluetoothServerSocket`-Wrapper, nur für explizit erwartete, gepaarte Adresse |

Tests: `CirqaRfcommDirectorySessionTest` (geteilte Frames, vollständige
Directory, 5043-ACK). Keine UI-Anbindung, kein BLE-/Socket-I/O in Tests.

## Sicherheitsgrenzen für eine spätere Freigabe

1. Nur `listenUsingRfcommWithServiceRecord`, **kein** `CLOSE_ALL`, keine
   Lösch-, Archiv-, Flag- oder Reset-Kommandos.
2. Verbindungen nur von der vorher explizit erwarteten, gepaarten CIRQA-Adresse
   akzeptieren; unbekannte Adressen sofort schließen.
3. Feste Budgets: 20 s Annahme, 60 s Transfer; kein Retry.
4. Dieselbe strengen CRC-/Offset-/Größenprüfung und dieselbe Maximallimitierung
   wie im BLE-Pfad; keine ungeprüften Gerätebytes übernehmen.
5. Kein SQLite-Import ohne separate Freigabe; keine Identitäts-/Schlüsselbytes
   in Berichten.
6. Vor dem Aktivieren: neues persistentes Gate, eigene UI-Freigabe, Review der
   Berechtigungen und ein separater CI-Build.

# CIRQA: Suche nach öffentlichen Bluetooth-Mitschnitten (17.11.2026)

## Ergebnis und Evidenzgrenze

Gezielte Web- und GitHub-Code-Suche nach `Garmin CIRQA btsnoop`, `CIRQA Bluetooth sniff log`, `Garmin CIRQA BLE capture`, `Garmin CIRQA GFDI 5050` ergab **keinen unabhängig veröffentlichten, verifizierbaren CIRQA-ACL/ATT-Mitschnitt**. Die GitHub-Treffer mit CIRQA beziehen sich überwiegend auf **unser eigenes Repository**. Das ist keine Behauptung, dass weltweit kein Mitschnitt existiert.

Vorhandene **eigene** Evidenz in `docs/CIRQA_GARMIN_CONNECT_SYNC_EVIDENCE_2026-11-16.md`: Garmin Connect hat historisch Dateien synchronisiert, darunter Monitoring- und Schlaf-Dateikategorien. Der bisherige umgewandelte `btsnooz`-Log enthält 6990 HCI-Command/Event-Datensätze, aber **null ACL-Datenpakete** und ist daher für GATT/GFDI-Nachrichten nicht verwertbar. Die App-Diagnose-Screenshots sind kein BLE-Dump.

## Nützliche Alternativen

- [GarminHRM600](https://github.com/mskerwiderski/GarminHRM600): dokumentierte GFDI-/FileSync-/Watch-Bootstrap-Frames eines **anderen Garmin-Geräts**; kein CIRQA-Dump.
- [Amazfish](https://github.com/piggz/harbour-amazfish): implementiert 5050 und 5037, keine belegte CIRQA-Aufzeichnung.
- [Garmin-Bridge](https://github.com/wh1le/garmin-bridge): Referenz für Session-/Handshake-Struktur, kein belegter CIRQA-Dump.
- [Android-HCI-Capture-Anleitung](https://github.com/eigger/hass-omron/blob/master/docs/capturing-bluetooth-logs.md): vollständigen `btsnoop_hci.log` via Android-Entwickleroptionen + `adb bugreport` sichern; `btsnooz` ohne ACL reicht nicht.

## Einmaliger entscheidender Referenztest (noch NICHT durchgeführt)

1. Android-Entwickleroptionen: **Bluetooth HCI-Snoop-Protokoll / Bluetooth-Stack-Protokollierung = vollständig/aktiviert**, nicht nur gefiltert. Bluetooth nach Bedarf neu starten. Keine Geräteentkopplung.
2. PulsLibre vollständig beenden. Garmin Connect verbinden und **eine** manuelle Synchronisation ausführen, während der HCI-Snoop aktiv ist. Zeitstempel und Erfolg notieren.
3. Unmittelbar danach per USB mit `adb bugreport cirqa_reference.zip` sichern. Im Archiv nach `FS/data/misc/bluetooth/logs/btsnoop_hci.log` oder `FS/data/log/bt/btsnoop_hci.log` suchen.
4. **Vor Auswertung** prüfen, dass ACL-Datenpakete (HCI packet type 0x02) und ATT Write/Notify vorkommen; bei null ACL ist der Mitschnitt ungeeignet. Ggf. `btsnooz_hci.log` separat als unzureichend markieren.
5. Nur lokal/privat auswerten: Raw-Dumps und Bugreports enthalten potentiell Geräteadressen, Kennungen, Tokens und Gesundheitsdaten. **Nicht öffentlich nach GitHub pushen.** Stattdessen ausschließlich anonymisierte Protokollabfolge und synthetische Fixtures übernehmen.
6. 5024-Antwort, 5050-Antwort, Folgeframes, Session/Core-Handshake und FileSync-Typen samt Sequenz-/Counterführung mit PulsLibre vergleichen. Erst nach verifizierten Frames aktive Implementierung.

Kein CLOSE_ALL, kein Unpairing/Reset, kein FileSetFlags oder unkontrollierter Retry. Die Rohdaten werden nicht ohne explizite Entscheidung veröffentlicht.

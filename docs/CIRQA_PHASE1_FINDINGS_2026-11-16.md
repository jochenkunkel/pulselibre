# CIRQA Phase 1: Auswertung der passiven Historien-Diagnose

Datum: 2026-11-16. Quelle: vom Benutzer bereitgestellter Diagnosebericht. Ziel bleibt der vollständige Abruf **aller** zugänglichen historischen Daten, nicht nur Herzfrequenz.

## Beobachtungen

- GFDI-Registrierung bestätigt, Handle 10, Protokollzustand COMPLETE, GATT-Schreiben bestätigt.
- Zwei Notifications: Registrierung (L=16) und eine GFDI-Nachricht (L=58).
- Ein CRC-/COBS-validierter GFDI-Frame, Typ 5024 (DeviceInformation); Bootstrap: DEVICE_INFORMATION_SEEN.
- Keine FileListResponse, keine Dateieinträge, keine kompakten Protobuf-Teilpakete, keine vollständigen Protobuf-Übertragungen.
- **In diesem Lauf keine Dateilistenanfrage gesendet.** Die Anzeige UNSUPPORTED beschreibt das Ergebnis früherer aktiver Versuche mit 0x8A39; sie ist kein neuer Status dieses Laufs.
- Keine Dateiübertragung und keine Geräteänderung.

## Schlussfolgerung

GFDI-Registrierung und Empfang von DeviceInformation sind bestätigt. Die verfügbare Evidenz reicht **nicht**, um FileSync-V2-Unterstützung oder einen CIRQA-spezifischen Bootstrap abzuleiten. Ein einzelner DeviceInformation-Frame enthält in diesem Bericht keine decodierten Fähigkeiten/Versionen. Weder Erfolg noch generelle Unmöglichkeit des vollständigen historischen Abrufs ist belegt.

## Nächster Entwicklungsauftrag

1. Passive, datenschutzschonende Auswertung der *strukturellen* DeviceInformation-Felder und ggf. weiterer beobachteter GFDI-Typen ergänzen (nur Feldnummern, Typen, Längen, keine IDs/Seriennummern/Rohpayloads).
2. Klären, ob das bisherige 20-Sekunden-Beobachtungsfenster ausreichend ist und ob zusätzliche rein passive Bootstrap-Metadaten gewonnen werden können.
3. Offline-Protokolltests mit belegten Referenzen erweitern; insbesondere nicht aus GFDI-5024 auf FileSync-Fähigkeit schließen.
4. Erst nach Protokollbelegen einen eng begrenzten, ausdrücklich aktivierten *lesenden* Geräteversuch planen. Kein blindes Wiederholen von 0x8A39.
5. Weiterhin kein FileSetFlags, CLOSE_ALL, Reset oder andere mutierende Gerätebefehle.

## Test-Gate

Ein neuer Gerätetest ist erst sinnvoll, wenn ein grüner Build die zusätzlich benötigten strukturellen Diagnosen tatsächlich erfasst. Bis dahin ist keine weitere Benutzeraktion erforderlich.

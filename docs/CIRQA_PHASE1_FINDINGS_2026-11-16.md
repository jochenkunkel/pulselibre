# CIRQA Phase 1: Auswertung der passiven Historien-Diagnose

Datum: 2026-11-16. Quelle: vom Benutzer bereitgestellter Diagnosebericht. Ziel bleibt der vollständige Abruf **aller** zugänglichen historischen Daten, nicht nur Herzfrequenz.

## Korrektur: maßgeblicher, vollständig abgelaufener Phase-1-Lauf

Der zuerst übermittelte Lauf mit 2 Notifications, Handle 10 und einem DeviceInformation-Frame war **nicht** der beabsichtigte vollständige Beobachtungslauf. Maßgeblich ist der danach vom Benutzer nachgereichte Bericht mit regulärem Ablauf des Beobachtungsfensters.

## Beobachtungen

- GFDI-Registrierung bestätigt, **Handle 11**, Protokollzustand COMPLETE, GATT-Schreiben bestätigt. Beobachtungsfenster regulär beendet.
- **Fünf Notifications:** Registrierung (L=16) und **vier** GFDI-Nachrichten (je L=58).
- **Vier** CRC-/COBS-validierte GFDI-Frames, ausschließlich Typ 5024 (DeviceInformation); Bootstrap: DEVICE_INFORMATION_SEEN.
- Keine FileListResponse, keine Dateieinträge, keine kompakten Protobuf-Teilpakete, keine vollständigen Protobuf-Übertragungen; keine ungültigen Frames.
- **In diesem Lauf keine Dateilistenanfrage gesendet.** Die Anzeige UNSUPPORTED beschreibt das Ergebnis früherer aktiver Versuche mit 0x8A39; sie ist kein neuer Status dieses Laufs.
- Keine Dateiübertragung und keine Geräteänderung.

## Schlussfolgerung

GFDI-Registrierung und Empfang von DeviceInformation sind bestätigt. Die verfügbare Evidenz reicht **nicht**, um FileSync-V2-Unterstützung oder einen CIRQA-spezifischen Bootstrap abzuleiten. Die vier DeviceInformation-Frames enthalten im Bericht keine decodierten Fähigkeiten/Versionen. Wiederholter Typ 5024 allein belegt keine konkrete Bootstrap-Sequenz. Weder Erfolg noch generelle Unmöglichkeit des vollständigen historischen Abrufs ist belegt.

## Nächster Entwicklungsauftrag

1. Passive, datenschutzschonende Auswertung der *strukturellen* DeviceInformation-Felder und ggf. weiterer beobachteter GFDI-Typen ergänzen (nur Feldnummern, Typen, Längen, keine IDs/Seriennummern/Rohpayloads).
2. Klären, ob das bisherige 20-Sekunden-Beobachtungsfenster ausreichend ist und ob zusätzliche rein passive Bootstrap-Metadaten gewonnen werden können.
3. Offline-Protokolltests mit belegten Referenzen erweitern; insbesondere nicht aus GFDI-5024 auf FileSync-Fähigkeit schließen.
4. Erst nach Protokollbelegen einen eng begrenzten, ausdrücklich aktivierten *lesenden* Geräteversuch planen. Kein blindes Wiederholen von 0x8A39.
5. Weiterhin kein FileSetFlags, CLOSE_ALL, Reset oder andere mutierende Gerätebefehle.

## Test-Gate

Ein neuer Gerätetest ist erst sinnvoll, wenn ein grüner Build die zusätzlich benötigten strukturellen Diagnosen tatsächlich erfasst. Bis dahin ist keine weitere Benutzeraktion erforderlich.

## Zweiter vollständiger Gerätetest: Bootstrap-Diagnose

- GFDI-Registrierung bestätigt, Handle **12**, GATT-Schreiben bestätigt, Protokollzustand COMPLETE.
- Beobachtungsfenster regulär beendet, **5 Notifications** (eine Registrierung, vier GFDI-Pakete zu je 58 Bytes).
- **4 gültige GFDI-Frames**, ausschließlich **5024**; **3 wiederholte DeviceInformation**; keine ungültigen Frames.
- Bootstrap-Diagnose: Status=0, weitere Typen=0, aktive Bootstrap-Befehle=0.
- FileListResponse=0, Dateieinträge=0; keine Dateiübertragung, keine FileListRequest.
- Reproduziert den ersten vollständigen Lauf (Handle 11, vier 5024-Frames). Unterschiedliches GFDI-Handle ist kein Fehlerbeleg.

**Interpretation:** Reine Registrierung/Beobachtung führt reproduzierbar nicht über wiederholte DeviceInformation hinaus. Ein ausstehendes ACK ist eine plausible, **nicht bestätigte** Hypothese. Ein weiterer unveränderter passiver Lauf bringt voraussichtlich keinen Erkenntnisgewinn. Nächster Entwicklungsschritt: ACK-Frameformat und sicheren expliziten Bootstrap-Versuch anhand verifizierter Protokollreferenzen prüfen; keine fenix-8-Identitätsbytes ungeprüft senden.

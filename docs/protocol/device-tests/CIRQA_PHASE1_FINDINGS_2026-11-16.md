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

## Dritter Gerätetest: einmaliges aktives 5024-ACK (2026-11-16)

Vom Benutzer gemeldeter vollständiger Testbericht:
- Bootstrap-ACK-Kandidat gesendet=true; GATT bestätigt=true; keine Dateibefehle.
- GFDI-Registrierung bestätigt, Handle 13; sechs Notifications (eine Registrierung und fünf GFDI-Pakete).
- Fünf gültige Frames, Typen: **5024 ×1, 5050 ×3, 5011 ×1**, keine CRC-Fehler.
- Paketlängen: 58 ×1, 29 ×3, 31 ×1. Keine wiederholte 5024.
- Vier Frames nach ACK-Start; keine FileListResponse, keine Dateieinträge, kein Historienzugriff nachgewiesen.

**Befund:** Im Gegensatz zu den passiven Läufen mit jeweils vier wiederholten 5024-Frames folgen auf das einmalige ACK neue Nachrichtentypen. Dies stützt die Bootstrap-Hypothese, beweist aber noch nicht die semantische ACK-Akzeptanz oder einen FileSync-Handshake. Die Typ-ID 5050 und ihr Payload sind noch nicht dekodiert; 5011 ist in GarminHRM600 als FIT_DEFINITION bezeichnet, CIRQA-Bedeutung noch zu verifizieren. Nächster Schritt: datensparsame strukturelle Analyse von 5050/5011 (Länge, GFDI-Header, sichere Statusfelder, keine Rohgesundheitsdaten) und Prüfung der Referenzimplementierungen vor weiteren aktiven Kommandos.

## Vierter Gerätetest: Post-ACK-Strukturanalyse

- Bootstrap-ACK-Kandidat gesendet und GATT bestätigt; keine Dateibefehle.
- GFDI-Handle 14; Registrierung erfolgreich; sechs Notifications, fünf gültige GFDI-Frames.
- Folge nach dem ACK: **5050, 5050, 5050, 5011** (vorher einmal 5024).
- Body-Längen: **5050 = 19 Bytes (dreimal)**, **5011 = 21 Bytes (einmal)**.
- Keine wiederholte 5024, keine ungültigen Frames, keine FileListResponse, keine Dateieinträge.
- Reproduziert die Typenfolge des dritten Gerätetests (damals Handle 13). Eine semantische ACK-Bestätigung ist nicht separat beobachtet.

**Nächste Untersuchung:** Referenzprotokolle für 5050 und 5011 prüfen; die reine Wiederholung der bestehenden Diagnose bringt keine neue Information. Vor einem weiteren aktiven Test zunächst sichere strukturelle Dekodierung bzw. erwartete Bestätigungen bestimmen. Keine Payload- oder Gesundheitsdaten im Diagnosebericht speichern.

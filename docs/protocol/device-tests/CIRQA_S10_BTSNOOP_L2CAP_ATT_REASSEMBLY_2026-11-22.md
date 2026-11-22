# Unabhängige Btsnoop-L2CAP-/ATT-Analyse (22.11.2026)

Quelle: originaler Nutzer-Upload `btsnoop_hci (1).log`, 5.522.737 Bytes, SHA256 `6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`. Offline, ohne Geräteaktion.

## Methode und Ergebnisse

Btsnoop-H4-Records sequenziell eingelesen. ACL nach HCI-Handle und Richtung mittels PB-Flags reassembliert, L2CAP-Länge/CID dekodiert; ATT-Opcode auf CID 0x0004 gezählt. HCI Connection Complete, LE Meta und Disconnect separat gelesen.

- 85.154 Records; 69.470 ACL, 11.066 HCI Events, 4.618 HCI Commands.
- 43.019 rekonstruierte L2CAP-PDUs; davon 42.799 ATT-PDUs (CID 0x0004), RX 36.001 und TX 6.798.
- ATT Notifications (0x1b) 35.465; ATT Write Commands (0x52) 6.267.
- 56 Connection-/Disconnect-Events (keine Anzahl eindeutiger Geräte).
- Auffälligkeiten dieser Zählmethode: 49 L2CAP-Kandidaten mit Überlänge und ein überschriebener unvollständiger Fragmentpuffer (unabhängiger Parserbericht); 0 offene Puffer am EOF. Der unabhängige Parser und sein ausführbares Verfahren liegen nicht vor, daher ist der Fragmentbefund hier nicht direkt reproduzierbar.

## Abgleich mit PR #522

Die Abweichung der Summen ist reproduzierbar und stammt aus dem Zählumfang:

- PR #515 zählt 43.019 L2CAP-PDUs und 42.799 ATT-PDUs; PR #522 zählt 42.970 vollständige, längenkonsistente L2CAP-PDUs und 42.794 ATT-PDUs.
- Die 49 Differenzdatensätze sind sämtlich Controller→Host ACL-Starts mit `PB=2` und reserviertem HCI-Handle `0xFF1`. In allen 49 ist die angegebene L2CAP-Länge kleiner als die im ACL-Body enthaltene Restlänge. Fünf haben CID `0x0004` (Records 17.427–17.431); weitere 44 tragen Nicht-ATT-CIDs.
- Diese fünf sind daher ATT-CID-Kandidaten, aber keinem gültigen LE-Connection-Handle bzw. keiner Verbindungsepoche zugeordnet. Sie werden von PR #522 vor L2CAP/ATT verworfen; das ist kein Verlust von 49 rekonstruierbaren Anwendungs-PDUs. Die Differenz von fünf ATT-Zählungen folgt daraus.
- Der in PR #515 berichtete einzelne „überschriebene Fragmentpuffer“ bleibt unabhängig nicht verifiziert: dessen Parserregeln und maschinenlesbares Ereignisprotokoll sind nicht verfügbar. PR #522 findet keine zweite Startfragment-Kollision auf einem gültigen Handle/Richtungsschlüssel und keine unvollständige Reassembly am EOF. Die 49 ungültigen `PB=2`-Starts könnten einen nicht filternden Parser beeinflussen; ein kausaler Zusammenhang ist nicht belegt.
- Regressionen in `tools/test_garmin_hci_replay.py` sichern sowohl das Ignorieren wiederholter, überlanger Starts auf dem reservierten Handle als auch das Verwerfen eines unvollständigen gültigen L2CAP-Assemblies bei einem neuen Start, ohne Payloads zu vermischen.

## Zeitfenster und Trace

- LE Connection Complete bei Btsnoop-Record 83.551, 2026-11-22 17:52:23.297305 UTC, HCI-Handle 0x0040.
- Disconnect bei Record 84.876, 17:53:08.134360 UTC.
- **Neue LE-Verbindung** bei Record 84.879, 17:53:16.590466 UTC; letzte ATT-PDU bei Record 85.151, 17:53:48.201181 UTC. Nicht beide Verbindungen zusammenwerfen.
- Ab 17:52 UTC bis EOF: 1.239 ATT-PDUs, davon 705 Notifications und 438 Write Commands. RX Attribute 0x001c (607) und 0x0012 (98); TX 0x001f (367) und 0x0015 (71).
- Häufige ATT-PDU-Längen 150, 5, 25 Bytes; keine sichere MLR-Klassifizierung allein durch Länge.
- Btsnoop-Zeitbasis 17:52–17:53 UTC weicht ~1h von bisher dokumentiertem Garmin-Sync 16:50–16:53 UTC ab; Logcat-Korrelation offen.

## Grenzen / Folgearbeiten

Die Identität der S10-Verbindung, GATT-Characteristic-Discovery, MLR/COBS, CRC-valide GFDI-Frames, FileAccess und FIT-Bytes sind **nicht** bestätigt. `FIT_BYTES_VERIFIED=false` bedeutet nicht nachgewiesen, nicht widerlegt.

**ChatGPT (Analyse-Owner):** Als Nächstes Verbindungs-Epochen/Logcat-Zeitbasis, L2CAP-Auffälligkeiten und GATT-Service-Mapping prüfen; anschließend MLR/GFDI und FileAccess anhand Record-Ranges belegen.

**FIT/BLE-Agent (Implementierungs-Owner):** Dieses Dokument mit `GarminHistoryFrameStream.kt`, `CirqaRfcommStreamFramer.kt`, `GarminGfdiHeader.kt`, `GarminFrameHeader.kt`, `GarminProtobufReassembler.kt` und `CirqaFileAccess*` abgleichen; pro bestätigter Abweichung Evidence-ID, Kotlin-Methode, Regressionstest, PR angeben. Keine spekulativen Änderungen, kein aktiver BLE-Test, #496 bleibt geparkt.

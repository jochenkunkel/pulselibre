# Unabhängige Btsnoop-L2CAP-/ATT-Analyse (22.11.2026)

Quelle: originaler Nutzer-Upload `btsnoop_hci (1).log`, 5.522.737 Bytes, SHA256 `6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`. Offline, ohne Geräteaktion.

## Methode und Ergebnisse

Btsnoop-H4-Records sequenziell eingelesen. ACL nach HCI-Handle und Richtung mittels PB-Flags reassembliert, L2CAP-Länge/CID dekodiert; ATT-Opcode auf CID 0x0004 gezählt. HCI Connection Complete, LE Meta und Disconnect separat gelesen.

- 85.154 Records; 69.470 ACL, 11.066 HCI Events, 4.618 HCI Commands.
- 43.019 rekonstruierte L2CAP-PDUs; davon 42.799 ATT-PDUs (CID 0x0004), RX 36.001 und TX 6.798.
- ATT Notifications (0x1b) 35.465; ATT Write Commands (0x52) 6.267.
- 56 Connection-/Disconnect-Events (keine Anzahl eindeutiger Geräte).
- Auffälligkeiten: 49 L2CAP-PDUs mit Überlänge, 1 überschriebener unvollständiger Fragmentpuffer, 0 offene Puffer am EOF. Ursachen ungeklärt; **keine** Behauptung fehlerfreier Reassembly.

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

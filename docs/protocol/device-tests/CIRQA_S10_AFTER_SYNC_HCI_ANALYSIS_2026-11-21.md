# S10-HCI nach Pairing/Sync: redaktierte Offline-Analyse (2026-11-21)

- Analyst: OpenCode (BLE/Protokoll)
- Quelle: vom FIT-Agenten erzeugter Bugreport und Bluetooth-Mitschnitt nach Pairing/Sync auf dem S10
- Methode: ausschließlich offline, lokaler redaktierender Parser; kein Bluetooth, kein Pairing, kein Sync, kein Gerätezugriff
- Abgrenzung: ersetzt die Aussage der älteren Analyse `CIRQA_S10_HCI_ANALYSIS_2026-11-21.md` nicht, sondern ergänzt sie um eine neue, aktuellere Spur mit ACL-/ATT-Ebene. Die dortige „0 ACL“-Aussage gilt weiterhin nur für den 8.509-Byte-Extrakt.

## 1. Ausgangsdaten und Archiv

Die geschützten Ausgangsdateien liegen unverändert unter `/tmp/opencode/private_s10_20261121/` (Verzeichnis 0700, Dateien 0600). Ein lokales Archiv mit Kopien, Größen, Mtimes und SHA-256-Prüfsummen wurde unter `/tmp/opencode/private_s10_20261121/archive_2026-11-21/` angelegt. Keine Rohdatei wurde in das Git-Repository übernommen.

| Rolle | Datei | Bytes | SHA-256 (Präfix) |
|---|---|---:|---|
| Bugreport nach Pairing/Sync | `s10-snoop-after-sync-2217.zip` | 43.387.729 | `54192edf…51999d` |
| daraus extrahierte HCI-Spur | `s10-snoop-after-sync-2217.hci` | 369.074 | `92b10473…83a3cc8f` |
| Kontroll-Bugreport vor Sync | `s10-snoop-check-2215.zip` | 44.634.237 | `15a1dd62…673a0c` |
| daraus extrahierte Kontroll-HCI | `s10-snoop-check-2215.btsnoop_hci.log` | 145.456 | `f1b36907…f91ff41` |

Der extrahierte Mitschnitt ist btsnoop v1 mit Datalink 1002 (H4). Der Validator `tools/btsnoop_summary.py` meldet für die neue Spur `records=5835`, `acl_records=3228`, `complete_att_candidates=3054`, `sufficient_for_att_analysis=true`. Für die Kontrollspur meldet er `records=2479`, `acl_records=1143`, `complete_att_candidates=1071`, ebenfalls `sufficient_for_att_analysis=true`.

## 2. HCI-, ACL- und L2CAP-Ebene

| Merkmal | After-Sync | Kontrolle |
|---|---:|---:|
| btsnoop-Records | 5.835 | 2.479 |
| H4-Kommandos / ACL / Events | 600 / 3.228 / 2.007 | 461 / 1.143 / 875 |
| Host/Controller-Records | 2.144 / 3.691 | 968 / 1.511 |
| ACL-Fragmente Host-Start / Controller-Fortsetzung / Controller-Ende | 1.544 / 76 / 1.608 | 507 / 32 / 604 |
| L2CAP ATT-CID 0x0004 | 3.120 | 1.098 |
| L2CAP SMP-CID 0x0006 | 13 | 0 |
| `Encryption_Change` (enabled=2) | 812 | 273 |
| `Disconnection_Complete` | 0 | 0 |

Die Verbindungsebene zeigt zwei erfolgreiche `LE_Enhanced_Connection_Complete_V1`-Meldungen (status `0x00`, logisches ACL-Handle `0x0040`) bei etwa +1.235,5 s und +1.535,6 s. Demgegenüber stehen 538 (`V2`)- bzw. 109 (`V2`, Kontrolle) Meldungen mit status `0x01`. `LE_Connection_Update_Complete`, `LE_Read_Remote_Features_Complete`, `LE_Data_Length_Change`, `Authentication_Requested` und `Encryption_Key_Refresh_Complete` sind vorhanden. Es gibt keine Trennungs-Meldung im Mitschnitt; das Ende des Fensters ist nicht als Disconnection belegt.

HCI-Kommandos sind überwiegend vendor-spezifische `LE_APCF`-Aufrufe sowie Scan-/Privacy-/Resolving-List-Kommandos. Diese wurden nicht inhaltlich interpretiert; daraus wird keine Geräte- oder Sync-Semantik abgeleitet.

## 3. SMP, GATT und Discovery

After-Sync enthält SMP-Operationen: `Pairing_Request`, `Pairing_Response`, `Pairing_Confirm`, `Pairing_Random` (2), `Pairing_Public_Key` (2), `Identity_Information` (2), `Identity_Address_Information` (2) und `Security_Request` (2). In der Kontrollspur wurden keine SMP-Operationen dekodiert.

ATT/GATT ist in beiden Spuren reichlich vorhanden: `Exchange_MTU` (MTU-Werte 150 und 515), `Read_By_Group_Type`, `Read_By_Type`, `Find_Information`, `Find_By_Type_Value`, `Read`, `Write_Request`/`Write_Response`, `Write_Command` (After-Sync 1.456, Kontrolle 469) und `Handle_Value_Notification` (After-Sync 1.514, Kontrolle 561). `GARMIN_ML_SERVICE` wurde in der After-Sync-Spur zweimal und in der Kontrollspur einmal in Discovery-Antworten erkannt. Das belegt GATT-Discovery und einen ML-Datenkanal, aber nicht automatisch eine CIRQA-Dateiübertragung.

## 4. Rekonstruierte GFDI-Streams

Aus den `Write_Command`/`Handle_Value_Notification`-Werten wurden zwei strukturierte Streams zum logischen Handle `0x0040` rekonstruiert: Host→Controller 83.413 Bytes, Controller→Host 47.504 Bytes (After-Sync) bzw. 22.963/20.893 Bytes (Kontrolle). Nach COBS-Envelope, Längen- und CRC-16-ARC-Prüfung ergaben sich CRC-validierte Rahmen:

| Rahmenart | After-Sync | Kontrolle |
|---|---:|---:|
| `DEVICE_INFORMATION` (5024), Controller→Host | 2 | 1 |
| `STATUS` (5000), Host→Controller | 2 | 1 |
| unbekannte Typ-IDs (`0x8000`–`0x9f00` und Varianten) | 1.021 | 356 |
| Summe | 1.025 | 358 |

Beide `STATUS`-Rahmen der After-Sync-Spur sind redaktiert geprüft: `response_to=5024`, `response_status=0`. Sie bestätigen damit eine erfolgreiche Quittierung der `DEVICE_INFORMATION`-Rahmen. Es wurde **kein** `STATUS` auf `5043`, kein `PROTOBUF_REQUEST` (5043), keine `PROTOBUF_RESPONSE` (5044), kein `DOWNLOAD_REQUEST` (5002), kein `FILTER` (5007), kein `SYSTEM_EVENT` (5030), kein `SUPPORTED_TYPES` (5031), kein `FILE_TRANSFER_DATA` (5004) und kein FIT-Frame (5011/5012) gefunden.

Die 1.021 unbekannten Typ-IDs sind CRC-validiert, aber im Projektparser nicht zugeordnet. Sie werden ausdrücklich **nicht** als CIRQA-Protokollschritte, Item-List, Pull oder Import ausgegeben. Ein Vergleich mit der Kontrollspur zeigt: Alle 107 dort vorkommenden Typ-IDs erscheinen auch nach dem Sync; die After-Sync-Spur enthält lediglich zehn zusätzliche ID-Varianten und mehr Wiederholungen. Die unbekannten Rahmen sind damit bereits vor dem Sync vorhanden und keine neue Sync-Folge.

Die beiden bekannten Bootstrap-ähnlichen Abläufe liegen bei etwa +1.237,955/+1.237,994 s und +1.541,746/+1.541,784 s; der rekonstruierte Stream reicht bis etwa +1.652,977 s. Daraus folgt eine wiederholte Geräteinformation/Status-Sequenz, nicht aber ein nachgewiesener Dateitransfer.

## 5. Fachliche Aussage

1. Die neue Spur liefert erstmals belastbare ACL-/ATT-/SMP-Evidenz vom S10; sie ist fachlich aussagekräftiger als der ältere 8.509-Byte-Extrakt.
2. Pairing-/Verbindungs- und GATT-Discovery-Aktivität sind belegt. Zwei erfolgreiche Verbindungen und eine ML-Service-Registrierung sind sichtbar.
3. Ein CIRQA-Historien-Download ist **nicht** belegt. Insbesondere fehlen `5043/5044`, `5002`, Item-List, Pull und FIT-Transferrahmen. Die vorhandenen Quittierungen betreffen ausschließlich `5024`.
4. Die Aussage aus #149 (`nextTransaction`-Gate 3/3, leere Folgeliste, `pullSent=false`) bleibt durch HCI nicht widerlegt; diese Spur erklärt das App-Verhalten nicht.
5. Die Garmin-Connect-Beobachtung eines nachgeladenen Tags wird durch diese Aufzeichnung weder bestätigt noch widerlegt.

## 6. Grenzen und Datenschutz

- Keine Geräteadressen, Schlüssel, Roh-Payloads, UIDs, Transaktions-IDs oder Gesundheitswerte in diesem Dokument.
- Keine neue aktive BLE-/Pairing-/Sync-Aktion; kein Reaktivieren des Snoops; keine Weitergabe der Rohdateien.
- Vendor-`LE_APCF`-Kommandos und die unbekannten GFDI-Typ-IDs bleiben uninterpretiert; eine Zuordnung würde weitere, separat freigegebene Evidenz erfordern.
- `nextTransaction`-Gate bleibt `3/3`; weiterer S25-No-Pull-Test nur mit neuer Freigabe.

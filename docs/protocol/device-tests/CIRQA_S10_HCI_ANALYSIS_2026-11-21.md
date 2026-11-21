# CIRQA – Offline-Analyse der S10-HCI-Spur (2026-11-21)

## Auftrag und Quelle

Der Nutzer bat darum, die bereits lokal gesicherte HCI-Spur des Samsung S10 im Rahmen des laufenden Bluetooth-Download-Problems auszuwerten, insbesondere Peering und Sync, und die Ergebnisse mit den anderen Agenten abzustimmen.

- Gerät: Samsung `SM-G970F`, Android 12 / API 31
- Quelle: lokaler HCI-Snoop-Extrakt aus dem ausdrücklich freigegebenen Bugreport
- Datei: `/tmp/opencode/private_s10_20261121/hci-01-btsnoop_hci.log`
- SHA-256: `5be10b4733ad31e7d2d32d2e406ba8252e131f22902c2bb3390e8bdd8f74606b`
- HCI-Snoop war während des Nutzer-tests eingeschaltet und wurde danach wieder auf `0` gesetzt
- Analyse: ausschließlich offline; kein neues Bluetooth, kein Pairing, kein Bandzugriff, keine Log-Datei im Repository

Diese Analyse verwendet nur strukturierte HCI-Metadaten. Geräteadressen, Roh-Payloads, Transaktions-IDs und Gesundheitswerte sind nicht dokumentiert.

## Format und Umfang

Der vorhandene Validator `tools/btsnoop_summary.py` liefert:

```text
{"acl_records": 0, "complete_att_candidates": 0, "records": 216,
 "sufficient_for_att_analysis": false,
 "types": {"command": 108, "event": 108},
 "warning": "No ACL packets; ATT/GATT analysis impossible"}
```

Zusätzliche Offline-Dekodierung:

- `btsnoop` Version 1, Datalink 1002 (H4)
- 216 vollständige Datensätze, keine abgeschnittenen Records
- 108 HCI-Kommandos und 108 `Command Complete`-Events
- **alle 108 Events sind vom Typ `Command Complete`**
- **alle 108 Return-Status-Bytes sind `0x00`**
- 0 Advertising Reports, 0 Connection Complete/Enhanced Connection Complete, 0 Disconnection Complete
- 0 `LE Create Connection`- bzw. `LE Extended Create Connection`-Kommandos
- 0 ACL-Pakete, 0 L2CAP-ATT-Kandidaten, 0 SMP-Pakete
- relative Zeitfenster: 0,000 s bis 56,505 s

Das H4-Pakettyp-Histogramm enthält ausschließlich `0x01` (Command) und `0x04` (Event). Es gibt keine `0x02`-ACL-Records.

## Zeitlicher Ablauf (relativ)

| Phase | Zeit (rel.) | Records | Beobachtung |
|---|---:|---:|---|
| Controller-Initialisierung | 0,000–0,170 s | 56 | Reset, Puffer-/Versions-/Features-Abfragen, Event-Masks, Host-Buffer, Simple Pairing/LE Host Support, lokale Gerätekennung (maskiert). Alle Status `0x00`. |
| Privacy-/Adresskonfiguration | 0,170–0,227 s | 56 | Resolving List und Privacy Mode mehrfach gesetzt, Address Resolution aktiviert, zufällige Adresse gesetzt. Keine Peer-Adresse oder Auflösung im Log. |
| Vendor-/Scan-Konfiguration | 0,413–0,482 s | 60 | 35 vendor-spezifische `LE_APCF`-Kommandos, ein weiteres vendor-spezifisches Kommando, mehrfache Extended-Scan-Parameter/-Enable. Scan wurde mehrfach ein-/ausgeschaltet; letzter Eintrag mit `scan_enable=1`. Keine Advertising Reports. |
| Späte Scan-Aktivität | 55,948–56,083 s | 28 | erneute `LE_APCF`- und Scan-Enable-/Parameter-Kommandos. Keine Events außer Command Complete. |
| Log-Ende | 56,486–56,505 s | 16 | weitere Scan-Konfiguration; Aufzeichnung endet unmittelbar danach. |

Die 108 Kommandos lassen sich nach Opcode-Gruppen wie folgt einteilen: 36 Controller-/Initialisierungskommandos, 19 Privacy-/Resolving-List-Kommandos, 17 Extended-Scan-Kommandos, 35 vendor-spezifische `LE_APCF`-Kommandos und ein `LE Rand`. Die Bedeutung der vendor-spezifischen Kommandos ist aus der HCI-Spur nicht ableitbar; daraus wird keine Aussage über Filterung oder Scan-Ergebnisse getroffen.

## Befund: Peering

Für eine belastbare Peering-Aussage müsste die Spur mindestens Verbindungs- oder Advertising-Ereignisse sowie ACL-basierte SMP-/ATT-Kommunikation enthalten. Das ist nicht der Fall.

- Kein `HCI_LE_Connection_Complete`, kein `HCI_Connection_Complete`, keine Advertising Reports und keine Connection-Request-Ereignisse.
- Keine HCI-Verbindungsaufbau-Kommandos; es ist nicht belegbar, dass überhaupt ein Verbindungswunsch an den Controller ging.
- Keine ACL-Verbindungshandles, keine SMP-CID-`0x0006`, damit keine Pairing Request/Response, Confirm/Random, LTK-/Identity-/Signing-Information und kein Encryption-Change-Ereignis.
- Die gesetzten Resolving-List-/Privacy-Kommandos zeigen nur lokale Konfiguration. Sie belegen nicht, dass ein Peer erkannt, aufgelöst, gekoppelt oder verschlüsselt verbunden wurde.
- Es gibt keine Peer-Adressen im Log, die einer Rolle (CIRQA-Band, Garmin Connect, anderes Gerät) zugeordnet werden könnten.

**Ergebnis:** Der Peering-Ablauf ist in dieser Spur nicht enthalten. Es kann weder ein erfolgreiches noch ein fehlgeschlagenes Pairing mit dem CIRQA-Band, noch die Abwesenheit eines Peers zum Testzeitpunkt behauptet werden.

## Befund: Sync und Bluetooth-Download

Der bekannte CIRQA-Pfad benötigt eine ACL/GATT-Verbindung, CCCD-Aktivierung, MLR-Registrierung und COBS-gerahmte GFDI-Nachrichten (`5000`, `5043`, `5044`) mit anschließender Smart-Protobuf-Auswertung (`item_list_response`, ggf. `pull_item_request`). Siehe `docs/protocol/CIRQA_PROTOCOL_SPEC.md`, `docs/protocol/sync/CIRQA_HISTORY_SYNC_PLAN.md` und `docs/protocol/device-tests/CIRQA_PRODUCT_DOWNLOAD_RESULT_2026-11-21_BUILD1701.md`.

In der Spur fehlen:

- ACL-Pakete und damit jeder L2CAP-/ATT-Kandidat
- CCCD-Schreibzugriffe und GATT-Service-/Characteristic-Discovery
- MLR-Registrierung für GFDI (`0x0001`) oder Transfer-Pipe (`0x2018`)
- COBS-Rahmen, GFDI-Typen `5000/5043/5044`, `Smart`-Protobuf-Felder oder Item-List-Antworten
- Pull-Anfragen, Pull-Antworten, Transfer-Handles oder Dateibytes

**Ergebnis:** Es gibt keinen HCI-basierten Beleg für irgendeinen Schritt des CIRQA-Sync oder des Garmin-Connect-Downloads. Die Spur kann die offenen Fragen zu `nextTransaction`, leeren Folgelisten, Statuscodes, Pull-Ablehnung, Transfer oder Import nicht beantworten. Sie widerlegt diese Abläufe auch nicht, weil der relevante Verkehr nicht aufgezeichnet wurde.

## Einordnung in die bisherigen Befunde

Die gleiche strukturelle Grenze ist bereits vom S25-Bugreport vom 2026-11-20 dokumentiert: 6.990 HCI-Datensätze, aber 0 ACL und 0 ATT-Kandidaten (`docs/protocol/device-tests/CIRQA_GARMIN_CONNECT_BTSNOOZ_CAPTURE_2026-11-20.md`). Die S10-Spur ist kleiner und enthält nur Kontrollkommandos, bestätigt aber unabhängig auf einem zweiten Samsung-Gerät:

> Der Android-Bugreport-HCI-Pfad liefert hier keine ACL-/ATT-Ebene und ist daher nicht geeignet, Peering oder Sync fachlich zu bewerten.

Das ist ein methodischer Erkenntnisgewinn, aber **kein fachlicher Schritt weiter** bei der Ursachenforschung zum Bluetooth-Download. Die offenen Punkte aus `#149`, `#150` und `#144` bleiben unverändert; ebenso die drei verbrauchten `nextTransaction`-No-Pull-Probes und deren `3/3`-Gate.

## Konsequenz und Grenzen

- Keine Änderung an Produktcode, Gates, Zählern oder Testplänen aufgrund dieser Analyse.
- Keine weitere Geräteaktion, kein erneutes Einschalten von HCI-Snoop und kein Zugriff auf die geschützten Logpfade.
- Für eine belastbare Sync-Analyse wäre eine Aufzeichnung nötig, die tatsächlich HCI-ACL-Pakete enthält und mindestens einen vollständigen ATT-Kandidaten auf L2CAP-CID `0x0004` liefert. Der vorhandene Validator nennt dies `sufficient_for_att_analysis`. Selbst dann blieben Gerätezuordnung und Protokollsemantik gesondert zu prüfen.
- Mögliche Wege (z. B. direkter Zugriff auf geschützte Bluetooth-Logs mit passender Berechtigung, anderer Capture-Pfad oder externer Sniffer) wurden nicht ausprobiert und benötigen einen eigenen, ausdrücklich freigegebenen Scope.

## Datenschutz

Bugreport und HCI-Extrakt bleiben unter `/tmp/opencode/private_s10_20261121/` mit Verzeichnisrecht `0700` und Dateirechten `0600`. Sie wurden nicht geteilt, nicht committet und inhaltlich nur soweit ausgewertet, wie es für diese strukturierte Protokollanalyse nötig war. Diese Seite enthält keine Geräteadressen, Roh-Payloads, privaten Namen oder Gesundheitsdaten.

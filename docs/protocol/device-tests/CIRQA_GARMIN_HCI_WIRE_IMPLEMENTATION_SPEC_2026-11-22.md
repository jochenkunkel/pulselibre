# Garmin Connect ↔ CIRQA S10: Wire-Replay- und Implementierungsauftrag (2026-11-22)

**Owner Implementierung:** OpenCode FIT/BLE-Agent · **Review/Koordination:** ChatGPT · **Tracking:** [#506](https://github.com/jochenkunkel/pulselibre/issues/506) · **Quelle:** [PR #505](https://github.com/jochenkunkel/pulselibre/pull/505).

## 0. Evidenzgrenzen (zuerst lesen)

Die **neue** Aufnahme ist in PR #505 und **noch nicht in main**. Ihre Binärdatei `docs/protocol/device-tests/raw/2026-11-22-s10-garmin-full-sync/btsnoop_hci.log` ist 5.522.737 Bytes groß (SHA-256 `6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`); Logcat 1.489.144 Bytes, SHA-256 `f6109178543b6edd39e983298c30672d15f2375c92e1243ab6d335e6e421231b`. Aufnahmedauer 2026-11-22 16:50:16–16:53:04 UTC, Samsung S10/Android 12, Garmin Connect 5.31.1. Bereits dokumentierte Zählungen: 85.154 HCI-Records, 69.470 ACL, 11.066 Events, 4.618 Commands, 36.714 **vollständige einzelne ATT-Kandidaten**; Logcat 11.562 Zeilen, davon 726 `onWriteCharacteristic`. Das ist **nicht** die Anzahl der übertragenen FIT-Dateien.

**Wichtige Unterscheidung:** Die unten beschriebene konkrete GFDI-Sequenz stammt aus **älteren** CIRQA-S10-Sync-Replays (Dokument `CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`). Die neue 5,5-MB-Aufnahme ist bislang nur metadatenbasiert analysiert. Bei dieser Erstellung war der direkte Abruf der Binär-/Logcat-Inhalte über den GitHub-Connector nicht möglich; daher **keine vorgetäuschte neue Byte-für-Byte-Dekodierung**. Der FIT-Agent hat Zugriff auf den PR-Branch und muss die folgenden Befunde am neuen Capture selbst verifizieren.

## 1. Implementierungsrelevante Referenzsequenz aus früherem Capture

| Phase | Richtung | GFDI | Untertyp / Interpretation | Evidenzstatus |
|---|---|---:|---|---|
| 1 | Band → App | 5024 | Device Information (54 B im älteren Fenster) | alter Replay belegt |
| 2 | App → Band | 5000 | ACK | alter Replay belegt |
| 3 | Band → App | 5050 | Bootstrap (25 B) | alter Replay belegt |
| 4 | App → Band | 5000 | ACK (9 B) | alter Replay belegt |
| 5 | App → Band | 5050 | Bootstrap-Gegenrichtung (24 B) | alter Replay belegt |
| 6 | beide | 5000/5043/5044 | ACK und Protobuf | alter Replay belegt |
| 7 | Band → App | 5052 | Current Time | alter Replay belegt |
| 8 | App → Band | 5043 | FileAccess Service-Feld **9** `item_list_request` | alter Replay belegt |
| 9 | Band → App | 5044 | Service-Feld **10** `item_list_response` | alter Replay belegt |
| 10 | App → Band | 5043 | Service-Feld **1** `pull_item_request` | alter Replay belegt |
| 11 | Band → App | 5044 | Service-Feld **2** `pull_item_response` | alter Replay belegt |
| 12 | App → Band | 5043 | Service-Feld **15** `set_flags_request` | alter Replay belegt, **nicht blind nachsenden** |
| 13 | Band → App | 5044 | Service-Feld **16** `set_flags_response` | alter Replay belegt |

Zusätzliche Typen 5026, 5030, 5031, 5036: in alten Traces vorhanden; Semantik/erforderliche S10-Reihenfolge nicht vollständig belegt. Alte Item-List-Seiten: 5 Responses mit `items=1/0/0/0/0`, `status=0`, wiederholtem `next_transaction_id`; nach Wechsel des Tokens drei leere Folgeseiten mit **unverändertem** Token. In PulsLibre ist dafür ein **begrenztes** Continuation-Gate (3) vorhanden. Nicht als generische Endlosschleife interpretieren. Alte Pull-Responses: `resultCode=0`, `transport=0`, `compressionWindow=11`; 18 Requests und 18/19 Responses in älteren Gesamtmitschnitten. **Erfolgscode bedeutet nicht, dass FIT-Bytes im BLE-Log sichtbar sind.**

**Korrektur für Implementierer:** ATT `0x52` ist **Write Command** (write without response), nicht Write Request (`0x12`). ATT `0x1b` ist Handle Value Notification. Für Richtung/Zuordnung zusätzlich HCI-Flags, Connection-Handle und GATT-Handle-Mapping prüfen. Alte Projekttexte nennen `0x52` irrtümlich „Write Request“; Code darf daraus keine Response-Erwartung ableiten.

## 2. Reproduzierbare Offline-Pipeline – in dieser Reihenfolge

### 2.1 Capture-Provenienz und Zeitbasis

1. PR #505 auschecken; SHA-256 der zwei Dateien gegen obige Werte prüfen; keine Rohdaten in Issues/Agent-Chat kopieren.
2. `tools/btsnoop_summary.py` unverändert ausführen und bekannte Summen reproduzieren. **Achtung:** Der vorhandene Parser zählt nur vollständig in einem ACL-Paket enthaltene ATT-PDUs; er reassembliert keine ACL-Fragmente und ist kein Gerätezuordnungsbeleg.
3. Btsnoop-Header `btsnoop\0`, Version 1, Datalink 1002, Records mit Big-Endian-Header `original_len, included_len, flags, drops, timestamp` lesen. Record-Offset, Record-Index, Capture-Zeit und HCI-Richtung als Provenienz speichern. Btsnoop-Epoch vs Unix-Epoch **mit Testvektor prüfen**, nicht nur mit Logcat-Uhrzeit raten.
4. Logcat mit Aufnahmefenster und Bluetooth-/Garmin-Ereignissen korrelieren. Das HCI-Log ist **fortlaufend**, also gesamtes Capture ≠ genau dieser Sync. Vorher-/Nachher-Fenster dokumentieren; mehrere Connections nach HCI-Handle und Verbindungsepoche trennen.

### 2.2 HCI → L2CAP → ATT

1. H4 Packet Type 0x02 (ACL) mit HCI ACL Header (`handle = handle_flags & 0x0fff`, `PB=(handle_flags>>12)&3`) parsen; Length exakt prüfen. Richtung aus btsnoop-Flags **an beobachteten ATT-Operationen validieren**.
2. L2CAP Basic Header (`length, cid` little endian) nach Startfragment rekonstruieren; Fortsetzungsfragmente anhand `handle+direction+connectionEpoch` zuordnen; pro Connection mehrere offene Channels zulassen; Größen-/Zeit-/Fragmentlimits setzen. Auf `CID=0x0004` ATT prüfen, ggf. EATT gesondert behandeln, nicht als ATT raten.
3. ATT-PDU mit Opcode, Attribute Handle, Value Length und Value Hash ausgeben. Relevante Opcodes: 0x02/0x03 MTU, 0x04/0x05 Find Information, 0x08/0x09 Read By Type, 0x12/0x13 Write Request/Response, 0x52 Write Command, 0x1b Notification, 0x01 Error. Fehlende oder unbekannte Handles als `unresolved` kennzeichnen.
4. Über Discovery und bekannte Garmin-ML-Service-UUIDs aus Repository Code die tatsächlichen RX/TX-Characteristic-Handles je Connection belegen; **niemals** Hardcode von Handle 0x40 als universelle UUID.

### 2.3 ATT → MLR → GFDI

1. Nur Payloads der bestätigten Garmin-ML-Characteristics an bestehende ML-Reassembler geben (vorhandene `Garmin*`- und `Cirqa*`-Implementierungen wiederverwenden).
2. MLR-Segmente nach `connectionEpoch, direction, mlChannel` reassemblieren. Chunk-Längen, Sequenz, eventuelle Fragment-/Timeout-/Duplicate-Regeln **aus Code und Trace** verifizieren. Unvollständige Frames als Fehler/Evidenzlücke zählen, nicht still verwerfen.
3. GFDI-Frames aus dem reassemblierten Byte-Stream abgrenzen; CRC-16/ARC (reflected poly 0xA001, init 0, xorout 0), final CRC little endian. Bekannte Testvektoren: ASCII `123456789` → `0xBB3D`, `FIT` → `0xBAD7`. Frame-CRC nur über die im bestehenden GFDI-Parser definierten Bytes, **nicht** über beliebige MLR-Fragmente.
4. Typ, Frame-Länge, CRC-Status, MLR-Kanal, HCI-Record-Range, Richtung, Zeitstempel und Protobuf-Envelope-ID als redaktierte strukturierte Ereignisse ausgeben.

### 2.4 GFDI → FileAccess-Protobuf

- Typ 5043/5044 mit dem **bereits im Projekt vorhandenen** Protobuf-Parser analysieren; Service-Felder 1/2/9/10/15/16 identifizieren. Nur belegte numerische Tags verwenden; unbekannte Wire-Felder als `unknown(fieldNumber, wireType, byteLength)` ausgeben.
- Pro `item_list_request` Request-/Response-Paar: Session/Transaction-Kontext **als lokale, nicht umkehrbare Zuordnungs-ID** (nicht Tokenwert), Filter-Flags, Item-Anzahl, `status`, Token-Änderung, Stoppgrund und Record-Ranges. Pagination auch bei leeren Seiten und wiederholtem Token nachvollziehen.
- Pro `pull_item_request/response`: korrelierter Item-Index/Hash, Result-Code, Transport, Compression Window, beobachtete Payload-Bytes und **belegter** Datenpfad. `resultCode=0` allein **kein** erfolgreicher FIT-Download.
- `set_flags_request` nur beobachten; Flags können Gerätezustand verändern. Keine Produktaktion ohne separate Freigabe.
- Für `5030` die Connect-seitigen System-Event-Codes (`HANDSHAKE_COMPLETE=8`, `HOST_DID_ENTER_FOREGROUND=6`) nur bei belegter Session-/Capability-Semantik übernehmen.

### 2.5 Datei-/FIT-Grenze

Ein tatsächlicher FIT-Transfer ist erst **bewiesen**, wenn ein vollständiger Byte-Stream zu einer validierten FIT-Datei zusammengesetzt wurde (Header, `.FIT`-Signatur, Länge, CRC), mit Herkunft aus HCI-Record-Ranges und stabilem Dateihash. Wenn Transfer über RFCOMM/Classic oder einen anderen Kanal läuft, der im Capture fehlt, explizit `transport_unobserved` ausgeben. **Keine synthetischen oder geratenen FIT-Dateiinhalte als Gerätefund markieren.** Die FIT-Nachrichten (SpO₂, Stress, Atemfrequenz) werden erst **nach** diesem Transport-/Datei-Gate durch `FitHistoricalObservations` verarbeitet; deren Persistenz ist gesondert in #500.

## 3. Maschinenlesbares Ergebnisformat (nur redaktierte Metadaten)

Pro Event eine JSON-Zeile, feste Felder, keine Roh-UIDs/Adressen/Tokens:

```json
{"schema":"pulselibre.garmin_wire_event.v1","capture_sha256":"<sha256>","record_start":123,"record_end":128,"timestamp_utc":"<ISO8601-or-null>","time_confidence":"hci|correlated|unknown","connection_epoch":"c1","direction":"host_to_band","layer":"gfdi","ml_channel":4,"gfdi_type":5043,"service_field":9,"att_opcode":"0x52","crc":"valid","payload_len":128,"payload_sha256":"<hash>","transaction_ref":"tx1","status":"observed","evidence":"trace","notes":[]}
```

Werte sind **Schema-Beispiel**, keine aus PR #505 extrahierte Beobachtung. Für jedes Feld: `observed` / `derived` / `hypothesis` / `unknown` mit Herkunft. Ergänzend:
- `capture_summary.json`: Roh-SHA, Fenster, Record-/Connection-/ATT-/GFDI-/FileAccess-Zählungen, Drops, Parsing-Fehler.
- `transactions.jsonl`: Request↔Response, Latenz, Pagination, Pull-/Set-Flags, Stoppgründe, Token **nur lokal pseudonymisiert**.
- `unknown_fields.json`: nur Nummer, Wire-Type, Länge, Häufigkeit, Kontext.
- `evidence_matrix.md`: Claim, Capture-SHA, Record-Range, Parser-Version, Confidence, Gegenbeispiel.
- `replay-fixtures/`: **nur geprüft synthetische** Wire-Fixtures für Git. Der bereits freigegebene Originalmitschnitt bleibt unverändert in PR #505.

## 4. Vergleich mit anerkannten Referenzen

| Referenz | Übertragbare Technik | Was **nicht** übernommen werden darf |
|---|---|---|
| [Garmin FIT SDK / FIT Protocol](https://developer.garmin.com/fit/protocol/) | FIT-Header, Datensatz-Definitionen, CRC, Base-Types, Endian, Invalid-Sentinels | FIT-Dateiformat beschreibt **nicht** CIRQA-BLE/GFDI-Transport |
| [Garmin FIT Java SDK](https://github.com/garmin/fit-java-sdk) | unabhängige FIT-Decode-Gegenprüfung auf JVM, synthetische Golden-Dateien | keine proprietären FileAccess-Tags oder Garmin-Connect-Session-Annahmen |
| [Garmin FIT SDK Tools](https://github.com/garmin/fit-sdk-tools) | Profile.xlsx / FitCSVTool als unabhängige Profil-/Datei-Referenz | nicht als Beleg für tatsächlich übertragenen S10-Inhalt |
| [Wireshark](https://www.wireshark.org/) | HCI/L2CAP/ATT-Dissection, zeitliche Korrelation, Referenz zu Fragmentierung | generische ATT-Decodierung ersetzt keine ML-/GFDI-Reassembly |
| [Scapy](https://scapy.net/) | programmatische Paket-/Pcap-Verarbeitung und reproduzierbare Offline-Tests | keine garantierte Unterstützung proprietärer CIRQA-Frames |

**Konkreter Qualitätsabgleich:** Im Gegensatz zum vorhandenen `tools/btsnoop_summary.py` muss der neue Parser Fragmentierung, mehrere Verbindungen und Zeitfenster berücksichtigen. Gegenprüfen, dass Wireshark und eigener Parser für **identische Capture-Filter** gleiche ATT-Opcode-Zählungen liefern; Abweichungen explizit dokumentieren. FIT-Decoder mit Garmin Java SDK gegen dieselben **synthetischen** FIT-Dateien vergleichen.

## 5. Geordneter Arbeitsplan für den FIT-/BLE-Agenten

- **P0 / Trace-Gate:** PR #505 prüfen, Hashes validieren, Datei-/Logcat-Fenster, HCI-Richtungen und Verbindungszuordnung beweisen. Kein Merge ungeprüfter sensitiver Rohdaten; Freigabe gilt nur für diesen Mitschnitt.
- **P1 / Transport-Gate:** Offline-Reassembler mit HCI ACL Fragmenten, L2CAP und ATT implementieren; Golden-Tests für Fragmentierung, Drops, mehrere Handles, falsche Längen, ATT 0x52/0x1b.
- **P2 / GFDI-Gate:** MLR/GFDI CRC-validierte Sequenzen rekonstruieren, Typen 5024/5050/5043/5044/5000 und unbekannte Typen zählen, neue vs alte Captures vergleichen.
- **P3 / FileAccess-Gate:** Alle 5043/5044 Service-Nachrichten nach Session und Transaktion gruppieren, vollständige Pagination und Pull-Antworten auswerten; Report, ob FIT-Bytes tatsächlich vorliegen.
- **P4 / Implementierung:** Nur durch neue Aufnahme bestätigte Regeln in bestehende Parser/Replay-Tests übertragen; **keine aktive BLE-Kommandofolge** allein aus diesem Bericht aktivieren. Sichere Fallbacks, Limits, Dedupe, Provenienz, Logging.
- **P5 / CI und Abnahme:** `tools/test_btsnoop_summary.py`, neue Offline-Tests, Android `:app:testDebugUnitTest`, CI, Build, Commit-SHA und Links; an ChatGPT mit START/UPDATE/ENDE per agent-chat/agent-sync melden.

## 6. Nicht verhandelbare Stop-Kriterien

1. FIT-Transfer nicht aus `pull_item_response(resultCode=0)` folgern.
2. Btsnoop-Records ohne belegte Connection/Direction nicht als CIRQA klassifizieren.
3. Bei fehlendem Reassembly-/CRC-Beleg unbekannte Bytes nicht als GFDI-Protobuf dekodieren.
4. Kein Gerätetest, Pairing, Reset, GATT-Write oder Set-Flags ohne konkrete separate Freigabe.
5. Keine MACs, UIDs, Tokens, persönlichen Gesundheitswerte in neuen Berichten. Keine weitere Rohdatenfreigabe ableiten.
6. `coverage=device-history-unverified` unverändert bis zur echten End-to-End-Abnahme.
7. Architektur-Refactoring #496 bleibt geparkt. FIT-Kandidaten/Persistenz #500 bleibt ein eigenes Ticket.

**Agenten-Abgabe:** Commit/PR, maschinenlesbarer redaktierter Replay-Report, Evidenzmatrix, synthetische Tests, Liste unbekannter Transportabschnitte, CI-/Build-Nachweis, explizite Aussage `FIT_BYTES_VERIFIED=true|false`.

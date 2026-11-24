# BLE-/GATT-Protokoll CIRQA – verdichteter Forschungsstand

> Stand 24.11.2026. Redigiert: keine Geräteadressen, Tokens, UIDs, Seriennummern, Gesundheitswerte oder Roh-Payloads. **Spezifikation ≠ Hypothese, ACK ≠ Download, Forschung ≠ Implementierung** ([`../protocol/README.md`](../protocol/README.md)). Quellen relativ zu `docs/knowledge/`.

## 1. Kanonische Quellen und Einordnung

| Rolle | Dokument |
|---|---|
| Hauptspezifikation (v0.1, 15.11.; `[C]` CIRQA-bestätigt / `[O]` aus Connect abgeleitet / `[U]` unbestätigt) | [`../protocol/CIRQA_PROTOCOL_SPEC.md`](../protocol/CIRQA_PROTOCOL_SPEC.md) |
| Forschungsstand Historie (18.11., laufend aktualisiert) | [`../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md`](../protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md) |
| Design Historien-Sync (V24/V25/V26 verifiziert) | [`../protocol/sync/HISTORICAL_SYNC.md`](../protocol/sync/HISTORICAL_SYNC.md) |
| Produktentscheidung P0 (24.11.) | [`../DATA_ACQUISITION_PRIORITY.md`](../DATA_ACQUISITION_PRIORITY.md) |
| Jüngster Audit (Transaktionsfilter, 24.11., offline) | [`../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md`](../protocol/research/CIRQA_CONNECT_TRANSACTION_FILTER_AUDIT_2026-11-24.md) |
| Terminal-/Pipeline-Review (23.11.) | [`../protocol/device-tests/CIRQA_S10_TERMINAL_EVIDENCE_2026-11-23.md`](../protocol/device-tests/CIRQA_S10_TERMINAL_EVIDENCE_2026-11-23.md) |

**Historisch, nicht mehr handlungsleitend** (als Evidenz erhalten): `CIRQA_EXTERNAL_PROTOCOL_RESEARCH.md` (frühe 5024-Hypothese), `CIRQA_REVERSE_ENGINEERING_OPEN_QUESTIONS.md` (viele „unbekannt" inzwischen geklärt), `CIRQA_HISTORY_SYNC_PLAN.md` (Schritt 1 überholt), `CIRQA_LEGACY_FILESYNC_PROTOCOL.md` (Connect-Code, **keine** CIRQA-Kompatibilität), `CIRQA_BLE_GROUND_TRUTH_COMPARISON.md` (History-Teil überholt).

## 2. GATT, ATT, CCCD, MTU

- **Service `[C]`:** `6a4e2800-667b-11e3-949a-0800200c9a66` (Garmin Multi-Link).
- **Characteristic-Paare `[O]`:** `6a4e2810`–`6a4e2819`; Schreib-Characteristic typischerweise `+0x10` (real RX `2810..2814`, TX `2820..2824`), sonst dieselbe Characteristic. **Konkrete CIRQA-Paare nicht bestätigt.** CCCD `0x2902`; Handles werden zur Laufzeit entdeckt.
- **Opt-in-GATT-Probe S25 `[C]`:** 5 Services, 15 Characteristics, Garmin-ML-Service und RX/TX-Paar gefunden; **kein** Beweis für ML-Registrierung/GFDI/Daten.
- **ATT-Opcodes (statisch):** `0x02/03` MTU, `0x04/05` Find Info, `0x08/09` Read By Type, `0x12` Write Request + `0x13` Response, `0x52` **Write Command ohne Response**, `0x1b` Notification, `0x1d/1e` Indication/Confirmation. L2CAP `CID 0x0004` = ATT. Handle `0x40` ist keine universelle UUID.
- **MTU/Fragmentierung:** COBS-Reassembly strikt längen- und zeitbegrenzt; konkrete ausgehandelte MTU **nicht dokumentiert**.
- **RFCOMM/Classic `[widerlegt]`:** Connect hat SPP-Acceptor, SDP lieferte aber nur Null-UUID, kein Gegenverbindungsversuch → kein gangbarer Weg.

## 3. MLR-/GFDI-Wire-Formate

Schichten: `BLE GATT → Garmin Multi-Link (MLR) → Realtime-Dienste | GFDI mit COBS → FIT → lokal`. Alle Mehrbyte-Zahlen **Little Endian**.

- **REGISTER_ML_REQ (13 B):** `u8 routing=0`, `u8 type=0`, `u64 Client-ID`, `u16 Servicecode` (GFDI=1, HR=6, Steps=7), `u8=0`.
- **REGISTER_ML_RESP (Kern 14 B):** `0x00, 0x01, u64 Client-ID, u16 Servicecode, u8 Status (0=Erfolg), u8 dynamischer Handle (1..127)`, optional mehr. CIRQA: **16 Byte für GFDI** beobachtet; Parser akzeptiert 14–64. Handle ist **dynamisch**, kein festes Register.
- **Routing-Byte:** 0 → Control; Bit 7 → Handle aus Bits 6..4 (1..7); sonst Byte direkt als Handle 1..127; Payload ab Byte 1. `CLOSE_ALL_REQ` existiert im Referenzcode, wird **nicht** genutzt.
- **GFDI-Frame:** COBS mit führendem/abschließendem `0x00`; danach `u16 packet_size | u16 message_type | payload | u16 crc16`, 6 B Overhead, `packet_size=6+N`.
- **CRC-16/ARC (aus Connect-DEX verifiziert):** reflektiertes `0xA001` (Normalform `0x8005`), init `0x0000`, refin/refout true, xorout `0`. Öffentliche Testvektoren: leer→`0000`, `A`→`30C0`, `123456789`→`BB3D`. Vergleich gilt dem **aktualisierten Endakkumulator** (früherer JADX-Verdacht war Artefakt).
- **GFDI-GFDI-Typnummern (nicht MLR-Servicecodes):** `5000` Status/ACK, `5002` Download File, `5004` File Data, `5007` Directory Filter, `5011` FIT Definition, `5024` Device Information, `5030` System Event, `5031` Supported File Types, `5036` System/Session, `5043` Protobuf Request, `5044` Protobuf Response, `5050` Configuration/Bootstrap, `5052` Current Time.

## 4. Legacy-Dateitransfer – Connect-Evidenz vs. CIRQA-Befund

| Schritt | Connect-Referenz | CIRQA-Gerät |
|---|---|---|
| `5007` Directory Filter | Enum `00/01/03`, Connect ruft `{03}`; Response `00` | Nach Bootstrap genau ein Write → CRC-valides `5000` mit `responseTo=5007`, Status 0 → **Request akzeptiert**, aber **keine Dateiliste** |
| `5031` Supported File Types | leerer Request; `u8 count + Typ/Subtype/Name`; Fallback `(128,4)/(128,36)` | Status 0 / 12 Typen (Phase B/C) |
| `5002` Download File | 14 B: `u16 index (0=Directory), u32 0, 01, u16 0, u32 0, u8 compression`; Status 0=Success…6; bei Status 3 ein Retry nach 1 s | Index 0 lieferte **8192 B gemeldete Größe**, kein Inhalt |
| `5004` File Data | unkomprimiert `u8 byte0 | u16 CRC | u32 offset | payload`; ACK `u8 result | u32 offset` | **nur ein 32-B-Platzhalter-Chunk** (`idx=0, type=0`), kein echtes Verzeichnis |
| Directory (Index 0) | 16 unbekannte Headerbytes + 16-B-Einträge mit `index, type, 3 B Identifier, specific, flags, size, timestamp` | CIRQA-Subtyp-Menge unbekannt |

**Kettenbefund 18.11. `[C]`:** Bootstrap `5024/5050 → 5030 → 5007 → 5031 → 5002` (Status durchweg 0), aber kein Inhalt. Filter 3 vs. 0 identisch. `5050`-ACK-Format (19-B-Body, ~5 s Retry) **nicht entschlüsselt**; kein automatischer ACK. SYNC-Bit-3-Hypothese **widerlegt** (ACK ohne `5037`). Kompakte `0x8A39`-FileList **widerlegt** (`UNSUPPORTED`). **Warnung:** Connect ruft nach Upload `archiveFile(index)` – für PulsLibre tabu.

## 5. Der erfolgreiche Pfad: Protobuf / FileAccess

Kette `[C]`, V16/V21/V24/V25: `sync_request (5043, Smart.core_service(13) → CoreService.sync_request(1))` → `sync_response (5044, 13:2[2:2])` → `file_access_service (Smart.file_access_service(43) → item_list_request(9))` → `item_list_response (10)` → `pull_item_request (1)` → `pull_item_response (2, SUCCESS, transfer_handle)` → MLR-Service **8216 / 0x2018** mit Konfigurationsframe (`u32LE transferHandle`) → Rohbytes.

**Felder:**
- `ItemListResponse`: `status=1, session_id=2, next_transaction_id=3, items=4`. **Korrektur V23:** Status ist Feld **1**, `ItemListStatus.SUCCESS=0` – V23 stoppte fälschlich vor jedem Pull.
- `ItemReference`: `uid=1, data_type=2, data_size=3, flags=4, transaction_id=5` (+ `modified_time=7` im Connect-Schema). UID-Rohbytes werden **unverändert** in den Pull-Request übernommen.
- `PullItemDataRequest`: `item=1, priority=2, supported_transports=3, offset=4`; Response mit Result, Transport, Transfer-Handle, optional Compression Window (9–15; Connect setzt 15). `MULTILINK_TRANSPORT_PIPE=0` (MUTF-8 `C0 80` = NUL).
- Service-Feldnummern: `item_list_request=9`, `item_list_response=10`, `pull_item_request=1`, `pull_item_response=2`, `set_flags_request=15`, `set_flags_response=16`.
- **SetFlags:** Connect setzt After-Sync-Flags (`result=0` belegt), ist geräteverändernd → **nicht nachsenden**.

## 6. Pagination, `next_transaction_id`, `runDrained`

- `session_id` = Cursor für weitere Seite **derselben** Liste; `next_transaction_id` = Kontext für eine **separate spätere Transaktion**, **kein Cursor und kein Boolean**; Request-`transaction_id` = separater Follow-up; `ItemReference.transaction_id` = Item-Eigenschaft. Session und Transaktion werden **nie zusammen** gesendet.
- **V24-Fehler:** `next_transaction_id` wurde als Seiten-Cursor behandelt → 64 leere Seiten bis Sicherheitslimit. **V25:** Fortsetzung nur bei `session_id`; kein `session_id` gerätseitig → eine Seite, 33 Transfers erneut vollständig → **Idempotenz bestätigt**. 33 Einträge beweisen **keine** vollständige Historie.
- **No-Pull-Probes 21.11. (3/3):** initial 28/35/35 Einträge, `session=false`, `nextTransaction=true`; genau **eine** `transaction_id`-Folge lieferte `status=0`, **0 Einträge**, erneut `nextTransaction=true`, `pullSent=false`. Nicht belegt: dass Folgeanragen jemals befüllte Listen liefern. Kein vierter Versuch.
- **Connect-Referenz S10:** eine befüllte Seite + eine alte-Token-Wiederholung + drei leere Seiten mit neuem Token (`1/0/0/0/0`); danach Stopp. `maxTransactionFollowups` deshalb auf **3** angehoben (PR #410/#411); alte-Token-Wiederholung wird nicht nachgebildet.
- **`runDrained`** = Queue **dieses Laufs** abgearbeitet, **kein** Nachweis vollständiger Historie; `coverage=device-history-unverified` bleibt zwingend. Limits (120 s / 64 Seiten / 256 Dateien / 8 MiB / 64 MiB) → **unvollständig**, nicht Erfolg.
- **36/37** = 36 erfolgreiche ausgewählte Pipelines von 37 zum Pull selektierten Dateien, **nicht** 36 von 37 Gerätedateien/Tagen; Terminalursache 120-s-Limit.
- **Freshness-Defekt `[reproduziert, behoben]`:** Skip auf UID/Typ/Größe/Flags + lokalem Hash bewies keine Remote-Aktualität (`modified_time`/`transaction_id` verworfen). PR #567 entfernte den Pre-Pull-Skip; **Full-Pull ist verbindlicher sicherer Fallback**.
- **Filter-Mismatch `[Hypothese]`:** PulsLibre sendet nur Request-Feld 2; Connect zusätzlich Felder 4/5/6. **Kein** Beweis, dass Filter die leere Liste verursachten; keine UUIDs raten. Connect-Pull-Requests enthielten nur UID → `transaction_id` in Pulls nicht hinzufügen.

## 7. Bewiesen / Hypothese / offen

**Bewiesen:** Realtime-Dienste HR(6), Steps(7), SpO₂(19) (9 Service-Familien dekodiert, keine klinische Genauigkeit); GFDI-Registrierung und dynamische Handles; 5007-Akzeptanz; vollständige Connect-kompatible Kette bis Rohbytes; V24/V25 33/33 Dateien, 31 FIT CRC-valid, SQLite 294, `integrity_check=ok`, Idempotenz; Protobuf-Feldnummern inkl. V23-Korrektur; `MULTILINK_TRANSPORT_PIPE=0`; 5043/5044-Pagination mit leeren Seiten; **kein** vollständiger FIT-Beweis aus HCI allein.

**Hypothese (nicht Root-Cause):** Garmin-Authentifizierung (5101–5112, `AuthManager mr2`, 15-s-Gate) als fehlender Schritt; Filterfelder 4/5/6; `v5d`→`p860.f`-Listener-Bindung; wiederholter Token oder SetFlags als Lösung; Aktivitäts-/Knopf-Effekte auf Listenanzahl ohne UID-Zuordnung.

**Explizit offen:** 19-B-`5050`-Format; 5024-Binärbytes; CIRQA-Capability-/Subtype-Map; 16 Directory-Headerbytes; Bedeutung der 5002-Konstantfelder und 5004-`byte0`; komprimierter 5004-Transfer; Generated-Descriptoren/Extensionnummern fehlen → On-Wire nur strukturell; Service 16 (Accelerometer) unbestätigt; vollständige historische Abdeckung/Retention. Siehe [`05_offene-fragen.md`](05_offene-fragen.md).

## 8. Widersprüche / Korrekturen (bewusst nicht geglättet)

1. **ATT `0x52`:** alte Datei nannte „Write Request"; maßgeblich ist **Write Command ohne Response**; `0x12` = Write Request.
2. **„Kein FIT-Transfer im HCI":** galt nur für Legacy-5002/5004; GFDI-Pulls mit `resultCode=0` sind belegt, FIT-Bytes als eigener Kanal nicht. Beides mit unterschiedlichem Scope gültig.
3. **V23-Statuslese:** `Status=0` = SUCCESS, erste Feldnummer 1 – frühere Annahme korrigiert.
4. **`next_transaction` als Cursor:** V24 (64 leere Seiten) vs. V25/Connect (Transaktionskontext).
5. **„Keine vollständige FIT-Synchronisation nachgewiesen"** vs. „33/33 übertragen": Transfer der **gelisteten** Dateien bewiesen, **Vollständigkeit der Gerätehistorie** nicht.
6. **Skip-Policy:** frühere Tests erlaubten Skip; `DATA_ACQUISITION_PRIORITY` setzt Full-Pull verpflichtend.

## 9. Sicherheitsgrenzen

Keine neuen aktiven BLE-/GATT-/FileSync-Kommandos ohne Freigabe; kein SetFlags/Archive/Delete/Reset/`CLOSE_ALL`, kein Pairing/Unpairing. Geräte-Lease vor jedem Zugriff, ein einziger Full-Pull, unveränderte Budgets, redigierte Berichte. Keine Gerätekennungen, Rohpayloads, Tokens oder personenbezogenen Gesundheitswerte in Git.

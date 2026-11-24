# Feldkatalog – alle gefundenen Felder und Record-Typen

> Stand 24.11.2026. Redigiert: keine Seriennummern, Geräteadressen, Tokens, UIDs, Hashes, Einzelmesszeiten oder Gesundheitswerte. **Confidence:** `C` = am Gerät/im Code bestätigt · `P` = FIT-Profil 21.222.0 gemappt (Semantik/Import separat) · `H` = Hypothese aus Referenzcode/Werteverteilung · `U` = gesehen, Semantik **nicht** dekodiert (bewusst nicht geraten). Vollständige maschinenlesbare FIT-Matrix: [`../protocol/research/CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv`](../protocol/research/CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv) (9.786 Zeilen) und [`CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md`](../protocol/research/CIRQA_FIT_INVENTORY_MATRICES_2026-11-18.md).

## 1. BLE/GATT- und MLR-Framing-Felder

| Feld | Typ | Bedeutung | Conf. |
|---|---|---|---|
| ML-Service | UUID | `6a4e2800-667b-11e3-949a-0800200c9a66`; RX `6a4e2810..2819`, TX i. d. R. `+0x10`, CCCD `0x2902` | C (Service) / H (Paare) |
| `REGISTER_ML_REQ` (13 B) | `u8 routing=0, u8 type=0, u64 client_id=2, u16 service_code, u8=0` | service: GFDI=1, HR=6, Steps=7 | C/H |
| `REGISTER_ML_RESP` (14 B Kern, CIRQA 16 B) | `0x00, 0x01, u64 id, u16 service, u8 status(0=ok), u8 dynamischer Handle 1..127` | Handle **dynamisch**, Extension-Bytes U | C |
| Routing-Byte | u8 | `0`=Control; Bit7 → Handle `(byte0&0x70)>>4` (1..7); sonst Handle 1..127; Payload ab Byte 1 | C/H |
| GFDI-Frame | `u16 size | u16 type | payload | u16 crc16` | COBS mit führendem/abschließendem `0x00`, 6 B Overhead, LE | C/H |
| CRC-16/ARC | — | poly `0xA001` reflektiert, init `0`, refin/refout true, xorout `0`; Testvektoren leer→`0000`, `A`→`30C0`, `123456789`→`BB3D`, `FIT`→`BAD7` | C |

## 2. Realtime-Servicecodes (Live-Daten)

| Code | Name | Felder | Conf. | Anmerkung |
|---|---|---|---|---|
| 6 | HR | `padding, bpm, resting_bpm` (u8) | C | Ruhepuls `0`=unbekannt |
| 7 | Steps | `steps, goal` (u32) | C | kumulativer Tageszähler |
| 8 | Calories | `total, active` (u32) | H | Einheit/Skalierung **unbestätigt**, nicht als kcal labeln |
| 10 | Intensity | `moderate, vigorous` (u16) | H | Akkumulationsfenster offen |
| 12 | HRV | `rr_interval` (u16, ms) | H | **einzelnes RR ≠ HRV/RMSSD** |
| 13 | Stress | `level` (i8, 0–100) | H | negativ = unbekannt |
| 19 | SpO₂ | `percent` (u8, 1–100) | H | `0xFF` = Sensor nicht bereit |
| 20 | Body Battery | `level` (i8, 0–100) | H | Wellness-Skala, **kein Band-Akku** |
| 21 | Respiration | `breaths_per_minute` (i8) | H | — |
| 16 | Accelerometer | gepackte Samples | U | nicht abonniert |

Gerätebelegt (Build 193/203): neun Dienste; **keine** klinische Validierung und seinerzeit keine Persistenz.

## 3. GFDI-Nachrichtentypen (nicht MLR-Servicecodes)

| Typ | Name | Felder/Struktur | Conf. |
|---|---|---|---|
| 5000 | Status/ACK | outer Status Offset 6, erstes ergebnisspezifisches Byte Offset 7, `to`, `status=0` | C |
| 5002 | Download File | 14 B: `u16 index; u32 0; u8 01; u16 0; u32 0; u8 compression`; Response `u8 status; u32 value`; `NOT_READY(3)` → 1 Retry nach 1 s | C (Connect) / H (CIRQA) |
| 5004 | File Data | `u8 byte0; u16 crc(LE, ab Offset 7); u32 offset; payload`; ACK `u8 result; u32 offset` | C |
| 5007 | Directory Filter | Enum `00/01/03`, Connect `{03}`; CIRQA Status 0, **keine Dateiliste** | C |
| 5011 | FIT Definition | 27-B-Frame / 21-B-Body | C (Struktur) / U (Semantik) |
| 5024 | Device Information | s. §4 | C (Struktur) |
| 5030 | System Event | `[eventByte,0]`; `HANDSHAKE_COMPLETE=8`, `HOST_DID_ENTER_FOREGROUND=6` | C/H |
| 5031 | Supported File Types | `u8 count + Typ/Subtyp/Name`; CIRQA 12 Typen | C |
| 5036 | System/Session | in alten Traces | U |
| 5043/5044 | Protobuf Request/Response | `requestId, offset, total, chunk, CRC` | C |
| 5050 | Configuration/Bootstrap | 25-B-Frame, 19-B-Body `[Länge=0x12][18 B Bitmaske]`; Bit-Semantik U | C/U |
| 5052 | Current Time | Response via 5000: `ref_type=5052, status=0, reference_id, garmin_timestamp, utc_offset, dst_end, dst_start` (23 B) | C |

**Legacy-Verzeichnis (16-B-Einträge):** `u16 file_index, u8 data_type (FIT=128), u8 sub_type` (ACTIVITY 4, MONITOR 32, METRICS 44, SLEEP 49, HRV_STATUS 68), `u16 file_number, u8 specific_flags (U), u8 file_flags, u32 file_size, u32 garmin_timestamp`. Die 16 Headerbytes sind **U**; CIRQA lieferte nur einen 32-B-Platzhalter (`idx=0,type=0`), kein echtes Verzeichnis.

## 4. 5024 Device-Information

`u16 protocol_version, u16 product_number, u32 unit_number, u16 software_version, u16 max_packet_size` (12 B, Struktur C, CIRQA-Werte **nicht** verifiziert) + drei längenpräfixierte Strings (BLE-Name/Gerätename/Modell, **H** aus fenix-8-Referenz) + 12 Binärbytes (**U**, nicht als Device-ID/Capability deuten). Fenix-8-Referenzframe: 52 B, Offset 7–18 Binär, 19+ String(14), 34+ String(5), 40+ String(8), 49 unbekannt, 50–51 CRC.

## 5. Protobuf FileAccess (der erfolgreiche Pfad)

| Ebene | Feld | Bedeutung | Conf. |
|---|---|---|---|
| Smart | 13 | `core_service` → `sync_request(1)` (leer; proto3 Default-false ausgelassen) | C |
| Smart | 43 | `file_access_service` → `item_list_request(9)`, `item_list_response(10)`, `pull_item_request(1)`, `pull_item_response(2)`, `set_flags_request(15)`, `set_flags_response(16)` | C |
| ItemListRequest | 1/2/3 | `session_id`, `transaction_id`, `max_item_count`; Filter `exclude_all_flags=4, get_flags=5, included_data_types=6` (H) | C/H |
| ItemListResponse | 1/2/3/4 | `status` (**0 = SUCCESS**, V23-Korrektur), `session_id` (= Seitencursor), `next_transaction_id` (= separate Transaktion, **kein Cursor**), `items`; dazu `error_code=5, abandoned_session_id=6, malformed_data_type=7` | C |
| ItemReference | 1/2/3/4/5/7 | `uid`, `data_type`, `data_size`, `flags`, `transaction_id`, `modified_time` (Feld 7 im Connect-Schema, vom Produktparser verworfen → Freshness-Defekt) | C |
| PullItemDataRequest | 1/2/3/4 | `item`, `priority`, `supported_transports`, `offset`, optional `requested_compression_window` | C |
| PullItemResponse | — | `resultCode=0=SUCCESS`, `transport=0=MULTILINK_TRANSPORT_PIPE` (MUTF-8 `C0 80`=NUL), `transfer_handle`, optional `compression_window` (9–15; Connect 15) | C |
| SetFlags | 1 | `set_flags`, Response `result=0`; **geräteverändernd, nicht nachsenden** | C |

MLR-Transfer: Pull → Service `8216/0x2018`, Konfigurationsframe `[0x00, DIRECTION_READ, u32LE transferHandle]`, dann Chunks. `runDrained` ≠ vollständige Historie; Limits 120 s / 256 Dateien / 64 MiB / 8 MiB pro Datei / 64 Item-List-Responses; `36/37` = erfolgreiche selektierte Pipelines, **nicht** Gerätedateien.

**Transaction-Filter-Audit 24.11.:** 117 CRC-valide Frames; sechs Pull-Requests mit **nur UID**; Host-ItemList-Requests mit Feldshapes `4+5`, `5+6`, `2+4+5`, `2+5+6`; Produkt sendet nur Feld 2 → **testbare Hypothese, keine Root-Cause**, keine UUID-Werte raten.

## 6. FIT-Basistypen und Invalid-Sentinels

| ID | Name | Byte | Invalid | ID | Name | Byte | Invalid |
|---|---|---|---|---|---|---|---|
| 0 | enum | 0x00 | `0xFF` | 9 | float64 | 0x89 | Bitmuster `0xFFFF…` |
| 1 | sint8 | 0x01 | `0x7F` | 10 | uint8z | 0x0A | `0x00` |
| 2 | uint8 | 0x02 | `0xFF` | 11 | uint16z | 0x8B | `0x0000` |
| 3 | sint16 | 0x83 | `0x7FFF` | 12 | uint32z | 0x8C | `0x00000000` |
| 4 | uint16 | 0x84 | `0xFFFF` | 13 | byte | 0x0D | Array alle `0xFF` |
| 5 | sint32 | 0x85 | `0x7FFFFFFF` | 14 | sint64 | 0x8E | `0x7FFF…` |
| 6 | uint32 | 0x86 | `0xFFFFFFFF` | 15 | uint64 | 0x8F | `0xFFFF…` |
| 7 | string | 0x07 | `0x00`-Terminator | 16 | uint64z | 0x90 | `0x0000…` |
| 8 | float32 | 0x88 | `0xFFFFFFFF` | | | | |

FIT-Epoche 1990-01-01 UTC, Unix-Offset **631.065.600 s**. Sentinels sind **keine Messwerte** und werden nicht skaliert.

## 7. Beobachtete FIT Global Messages (mit Profil-Mapping)

Auswahl der relevanten Nachrichten; vollständige Feldlisten in der CSV/Matrix oben. **P** = Profil-Mapping bestätigt, **D** = am Gerät/im Decoder nachgewiesen.

| Global | Name | Zentrale Felder (Auswahl) | Status |
|---|---|---|---|
| 0 | file_id | F0 type, F1 manufacturer, F2 product, F3 serial_number, F4 time_created, F5 number | P; Identifikatoren bewusst **nicht** importiert |
| 1 | capabilities | F1 sports, F21 workouts_supported, F23 connectivity_supported | P |
| 2 | device_settings | F0 time_zone, F1 utc_offset, F58 autosync_min_steps, F59 autosync_min_time, F86 ble_auto_upload, … | P; keine validierte CIRQA-Semantik |
| 3 | user_profile | F0 friendly_name, F3 height, F4 weight, F8 resting_hr, … | P; personenbezogen → nicht importiert |
| 7/12/13 | zones_target / sport / training_settings | HR-Zonen, sport, Zielstrecke/-zeit | P |
| 18 | session | F7 elapsed, F9 distance, F11 calories, F16/17 avg/max HR, F134 avg_step_length, F147 respiration, F194 spo2, F195 stress, F197/198 HRV, F253 timestamp | P; **nicht** als SQLite-Metrik nachgewiesen |
| 19 | lap | wie session, plus Intensität/Lap-Trigger | P |
| 20 | record | F0/1 Position, F3 HR, F5 distance, F73 speed, F78 altitude, F253 timestamp | **D** (HR, Distanz, Speed als Metriken belegt); Position nur ~1,8 % gültig, datenschutzrelevant |
| 21 | event | F0 event, F1 type, F2 data16, F3 data, F15 start_timestamp | P |
| 23 | device_info | F0 index, F1 type, F5 software_version, F10 battery_voltage, F11 battery_status, F18 sensor_position, F25 source_type, F32 battery_level, F253 | **D** (Whitelist in `fit_device_metadata`); 0 gültige Batterie-Werte |
| 26/27 | workout / workout_step | Sport, steps, target, exercise | P |
| 34/35/37-39/49 | activity / software / capabilities / file_creator | Zeit, Version, Zähler | P |
| 55 | monitoring | F2 distance, F3 cycles (×2), F4 active_time, F19 calories, F24 intensity, **F26 timestamp_16**, F27 HR, F33/34 moderate/vigorous min, F253 | **D**; Schritt-Feld-Zuordnung **umstritten** (s. §11) |
| 103 | monitoring_info | cycles_to_distance/calories, resting_metabolic_rate | P |
| 145/162 | memo_glob / timestamp_correlation | Glob-Struktur, system/local timestamp | P |
| 188/211 | ohr_settings / monitoring_hr_data | enabled; resting/current_day_resting_hr | P; Feldzuordnung 211 gesondert prüfen |
| 216 | time_in_zone | HR-Zonengrenzen, Zeit in Zone | P |
| 227 | stress_level | F0 stress_level_value (sint16, `profile_raw`), **F1 `stress_level_time` nicht als FIT-Epoch**, F2–4 unmapped | D/H; Review-Kandidat `UNTRUSTED_PROFILE_CANDIDATE` |
| 269 | spo2_data | F0 reading_spo2 (`0xFF` ungültig), F1 confidence, F2 mode, F253 | D; Kandidat `spo2`, Konfidenz/Modus ohne bestätigte Qualitätssemantik |
| 275 | sleep_level | F0 sleep_level(enum), F253 | P; **kein** validierter Schlafphasen-Import |
| 297 | respiration_rate | F0 respiration_rate (sint16, Skala 100; `0x7FFF` ungültig), F253 | D; Codes `-100/-200/-300` = Gerätestatus, **keine** gesicherte Profilvereinbarung |
| 346 | sleep_assessment | 16 Scores (uint8/uint16) | P; kein Score-Import belegt |
| 370/371 | hrv_status_summary / hrv_value | Skala 128 (ms), status | P; HRV **nicht** als importierte Metrik belegt |
| 398 | skin_temp_overnight | relative Abweichung (float32) | P; **keine** absolute Hauttemperatur, kein Import |
| 412/470/471 | nap_event / sleep_disruption | Ereignis-/Severity-Codes | P; Import offen |

**Nur gesehen, NICHT dekodiert (alle beobachteten Global-IDs):** 22, 24, 79, 140, 233, 241, 273, 282, 284, 288, 330, 355, 356, 369, 382, 383, 384, 391, 397, 401, 408, 410, 411, 413, 430, 431, 484, 506, 507, 521, 545. Markante Hypothesen: `record/136` (Handgelenks-HF?, 12.730×), `record/135` (0–176, U), `record/143` (5–24, evtl. Atemfrequenz), Global 233 (15.385 Datensätze, 4-Byte-Bitfeld, herstellerspezifisch vermutet), Global 397 (2.830×, evtl. Hauttemperatur-Abweichung). Zusätzlich unzählige unmapped Felder **innerhalb** bekannter Messages (u. a. `device_settings` mit ~60, `training_settings` mit ~40, `record/135,136,143`, `stress_level/2–4`, `hrv_status_summary/7,8`, `skin_temp_overnight/3`).

## 8. Zeit-, Generations- und Qualitätsfelder

| Feld | Werte/Regel | Conf. |
|---|---|---|
| `timestamp` (F253) | uint32, FIT-Epoche, direkte Messzeit | C |
| `timestamp_16` (55/26) | `ts = letzter_voll_ts + ((ts16 − (letzter_ts & 0xFFFF)) & 0xFFFF)`, fortlaufend je Datei, `0xFFFF` ungültig; 1.622 HR-Werte rekonstruiert | C |
| `timestamp_origin` | `DIRECT`, `RECONSTRUCTED_TS16`, `RECONSTRUCTED_COMPRESSED`, `UNKNOWN`; in Statusereignissen `NOT_APPLICABLE` | C (DDL-Check) |
| `DECODER_VERSION` / `PROFILE_VERSION` | Code: **`"2"`** / `"21.222.0"` (ältere Doku nennt "1" → Code maßgeblich) | C |
| `sample_id` | `sha256(fileSha:recordOrdinal:global:field)`; Idempotenzschlüssel | C |
| `quality` | `PROFILE_VALID` (in `observations.value`), `UNTRUSTED_PROFILE_CANDIDATE`, `LEGACY_PROFILE_VALID`, `UNKNOWN` | C |
| Generation-scoped Rohfelder | Quellhash, Decoder-/Profilgeneration, `record_ordinal`, `global_message_number`, `field_number`, `base_type`, `field_size`, Endianness, `developer_data_index`, `timestamp_origin`; Output-Digest über Beobachtungen **+ alle Rohfelder** | C (PR #610) |

**Statuscodes bewusst nicht als Zahlen:** RR `-100` off wrist, `-200` excessive motion, `-300` invalid (GUI-Entwurf, keine Profilvereinbarung); HR `0` ist **kein** FIT-Sentinel (`0xFF` ist es), aber außerhalb 1–240; SpO₂ `255` = `0xFF`-Sentinel, verworfen.

## 9. Geräte-Metadatenfelder

- **`fit_device_metadata` (Whitelist aus `device_info`):** `software_version` (F5), `hardware_version` (F6), `battery_voltage` (F10), `battery_status` (F11), `battery_level` (F32), `sensor_position` (F18), `source_type` (F25), `observed_at_ms`, `record_ordinal`, `source_sha256`. Seriennummern/Device-Index ausgeschlossen. Befund: ~115 Zeilen, davon **0** mit gültigem `battery_level_percent`/`battery_status`; Spannung ohne Kalibrierkurve → kein Prozentwert.
- **`raw_device_metadata`:** `source_sha256`, `file_bytes`, `parser_version`, `software_version`, `mass_storage_mode`, `transfer_direction_count`. XML-Struktur gezählt (BaseName, DataType, File, FileName, Id, Location, Model, Name, PartNumber, Path, SoftwareVersion, TransferDirection, Version, …), **keine** Werte/Pfade/IDs publiziert; leerer Text ≠ `false`.
- **`live_device_metadata`:** `device_name` (GATT `0x2A00`) bestätigt; `gatt_<uuid16>` mit ungeratenem Hex-Wert; `BATTERY_LEVEL` (`0x2A19`) und Garmin `GDIDeviceStatus` wurden **nicht angeboten** → keine Zeile; Firmware/Uhrzeit/Restlaufzeit leer.
- **Weitere Kandidaten (nicht CIRQA-validiert):** 5024-Offline-Parser, Capability-Bitmaske (18 B CIRQA / 15 B Amazfish), SUBTYPE-Map, 229-B-Protobuf unbekannten Schemas.

## 10. SQLite-Felder (Überblick; Details in [`03_parser-datenpfad.md`](03_parser-datenpfad.md))

| Tabelle | Zentrale Spalten |
|---|---|
| `events` | `line_sha256 UNIQUE, received_at_utc, dashboard_time, kind, service, payload_json` |
| `observations` | PK `(device_id, metric, measured_at_ms, sample_id)`, `value REAL, unit, received_at_ms` |
| `fit_sources` | `source_sha256 PK, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version` |
| `fit_observation_provenance` | `sample_id PK` + Quelle, Ordinal, Nachricht/Feld, `quality`, `timestamp_origin`, `decoder_version` |
| `fit_decoder_generations` | `generation_id, source_sha256, decoder_version, profile_version, output_digest, output_count, decoded_field_count` |
| `fit_generation_observations` | `generation_id, sample_id, value, unit`, Koordinaten, `quality='PROFILE_VALID'`, `timestamp_origin` |
| `fit_generation_fields` | PK `(generation_id, record_ordinal, field_kind, field_number, developer_data_index)`, **`raw_bytes BLOB`**, `big_endian`, `base_type_code` |
| `fit_active_generations` | `source_sha256, generation_id` (eine aktive Generation je Quelle) |
| `fit_unmapped_fields` | Signatur + Zähler + `classification`/`review_status` (Rohbytes im Archiv) |
| `fit_review_candidates` | `sample_id PK`, `UNTRUSTED_PROFILE_CANDIDATE`, nullable Zeit, Rohwert |
| `observation_quality` | FK→`observations` CASCADE, `quality_status`, `measurement_mode`, `raw_status_code`, `timestamp_origin`, Koordinaten |
| `observation_status_events` | `idempotency_key UNIQUE`, `measured_at_ms NULL`, `raw_code`, `raw_value_text`, `status_code`, `timestamp_origin` (CHECK Zeit↔Origin) |
| `import_runs` | `import_run_id PK`, `status RUNNING/COMPLETED/PARTIAL/FAILED`, Zähler |
| `import_errors` | FK→`import_runs`, `error_class`, `redacted_message`, `decision FILE_SKIPPED/RUN_ABORTED` |
| `downloaded_files` / `downloaded_file_references` | `content_sha256 PK`, `payload BLOB`; Referenz-PK `(content_sha256, reference_sha256)` |

Schema-Evolution: v6 (20.11) → v7 Generationen → v8 Review/Live-Metadaten → v9 Qualitäts-/Status-/Import-Tabellen + Originalarchiv → v10 Originale/Referenzen → **v11 Rohfelder** (`fit_generation_fields`, `decoded_field_count`). CSV-Export: `device_id, metric, measured_at_utc, measured_at_epoch_ms, value, unit, sample_id, received_at_utc`.

## 11. Abgeleitete Anzeigefelder

Herzfrequenz (bpm, 1–240), Ruhepuls (Gerätefeld), Schritte (kumulativ, **nicht** summieren), Schrittziel (Geräteziel), SpO₂ (1–100 %, `0xFF` ungültig), RR (einzelnes Intervall ≠ HRV), Stress/Body Battery (proprietäre 0–100-Skala), Atemfrequenz (Skala 100, negative Codes = Status), Kalorien (Einheit unbestätigt), Intensitätsminuten (Fenster offen), Distanz `/100`, Speed `/1000`, Zeit-Offset 631.065.600 s, `kcal × 4,184 = kJ`. Qualitätszustände: `VALID | INVALID | STATUS | LIMITED | UNVERIFIED`; fehlende Werte = fehlende Punkte, **nie 0**, keine Interpolation. Konzepte **nicht produktiv**: Kadenz, Intensitätsäquivalenz, HRV/RMSSD (gesperrt), Ruhepuls-Trend, HRmax-Schätzung, Aktivitätskonsistenz.

## 12. Widersprüche (nicht still aufgelöst)

1. `DECODER_VERSION`: Code `"2"` vs. alte Doku `"1"` → **Code maßgeblich** ([`03_parser-datenpfad.md`](03_parser-datenpfad.md) §6).
2. Monitoring-Schritte: alte Doku F9 `cumulative_steps (×2)` vs. 18.11-CSV (Global 55 ohne F9, F3 `cycles` scale 2) → **offen**.
3. Status-/Import-Tabellen: alte Audits „nie beschrieben / nicht im Backup" → **überholt** durch #613 und Backup **v11** (siehe [`03_parser-datenpfad.md`](03_parser-datenpfad.md) §8–10).
4. „Originalbytes nicht im Backup" → seit v9 `downloaded_file_archive.jsonl` **überholt**.
5. `status=0` als Fehler (V23), `next_transaction_id` als Cursor (V24), ATT `0x52` als Write Request → alle korrigiert ([`01_ble-protokoll.md`](01_ble-protokoll.md) §8).
6. „Alle Felder dekodiert" → **nicht** behauptet: 109.960 unbekannte Feldvorkommen, 10.283 Inventareinträge, 2.183 Invalid-Sentinel-Vorkommen, 2.796 Review-Kandidaten (979 ohne Zeit).

## 13. Explizit nicht dekodiert

**BLE/Protobuf:** 19-B-5050-Bitmaske ohne Bit-Semantik, 12 5024-Binärbytes, 16 Directory-Headerbytes, 5002-Konstantfelder, 5004-`byte0`, komprimiertes 5004, 5026/5036, 5030-innere Werte, Auth 5101–5112 (Hypothese), alle `unknown(fieldNumber, wireType, byteLength)`, unbekannte GATT-Characteristics (nur `gatt_<uuid16>`), CIRQA-Capability-/Subtyp-Map, Service-16-Format.

**FIT:** alle in §7 gelisteten Global-IDs und unmapped Felder; 19975-B-Device-XML (nur Struktur), 229-B-Protobuf (Wire valide, Schema unbekannt); RR-Statuscodes, `stress_level_time`-Interpretation, SpO₂-Konfidenz/Modus, Temperatur, Messintervalle – `quality_status_catalog`/`measurement_modes` bleiben `UNKNOWN`.

**Grenze:** Diese Katalogisierung behauptet **weder** vollständige semantische FIT-Profilunterstützung **noch** vollständige Gerätehistorienabdeckung **noch** medizinische Validierung. Maßgeblich: Code/CI > aktueller Projektstatus > historische Doku.

# CIRQA offline inventory matrices

FIT Profile reference: 21.222.0. This is a schema reference, not decoded value output.

No measurement values or full timestamps are included. File aliases are local 12-character SHA-256 prefixes. `valid` and `invalid/missing` are encoded-element counts; absence counts are based on message records, as described in the main report.

## File matrix

| Alias | Format | Bytes | FIT data records | Observed messages / records | Month range | Quality / note |
| --- | --- | ---: | ---: | --- | --- | --- |
| 076e8ad17e6d | FIT | 4154 | 26 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (1), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (1), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 13af97181a28 | FIT | 812 | 12 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 241:unknown (1), 284:unknown (1), 330:unknown (1), 369:unknown (1), 384:unknown (1), 410:unknown (3), 506:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 166e92aab6a3 | FIT | 4210 | 30 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (3), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (3), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 1b9171123e8b | FIT | 13525 | 1465 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 162:timestamp_correlation (2), 397:unknown (1459), 398:skin_temp_overnight (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 206698213089 | FIT | 2231 | 39 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 79:unknown (1), 282:unknown (2), 383:unknown (1), 384:unknown (4), 391:unknown (8), 401:unknown (1), 411:unknown (4), 412:nap_event (8), 413:unknown (1), 430:unknown (1), 431:unknown (1), 507:unknown (1), 545:unknown (3) | 2026-11–2026-11 | CRC-validated in private source set |
| 349ffcf90069 | FIT | 395 | 5 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 241:unknown (1), 356:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 3c82eb172ecf | FIT | 4765 | 73 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (24), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (24), 288:unknown (2) | 2026-11–2026-11 | CRC-validated in private source set |
| 4667def19a0e | FIT | 5779 | 605 | 0:file_id (1), 21:event (4), 23:device_info (1), 24:unknown (34), 35:software (1), 55:monitoring (141), 103:monitoring_info (1), 188:ohr_settings (1), 211:monitoring_hr_data (2), 227:stress_level (105), 233:unknown (105), 269:spo2_data (101), 297:respiration_rate (105), 355:unknown (1), 408:unknown (1), 484:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 47ce97d915af | FIT | 2399 | 11 | 0:file_id (1), 26:workout (1), 27:workout_step (6), 49:file_creator (1), 145:memo_glob (2) | no complete timestamp | CRC-validated in private source set |
| 55b498402150 | FIT | 4841 | 51 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (11), 21:event (2), 22:unknown (2), 23:device_info (8), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (11), 288:unknown (2) | 2026-11–2026-11 | CRC-validated in private source set |
| 597c089f6fba | FIT | 456 | 5 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 241:unknown (1), 369:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 709be116b41e | FIT | 19163 | 2464 | 0:file_id (1), 21:event (2), 23:device_info (1), 24:unknown (51), 35:software (1), 55:monitoring (506), 103:monitoring_info (1), 188:ohr_settings (2), 211:monitoring_hr_data (1), 227:stress_level (471), 233:unknown (475), 269:spo2_data (475), 297:respiration_rate (471), 355:unknown (1), 408:unknown (4), 484:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| 85efbba758d0 | FIT | 3714 | 16 | 0:file_id (1), 26:workout (1), 27:workout_step (11), 49:file_creator (1), 145:memo_glob (2) | no complete timestamp | CRC-validated in private source set |
| 94af73b2cf14 | FIT | 5115 | 77 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (24), 21:event (2), 22:unknown (2), 23:device_info (8), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (24), 288:unknown (2) | 2026-11–2026-11 | CRC-validated in private source set |
| 9e8ea4fe6762 | FIT | 4651 | 41 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (6), 21:event (2), 22:unknown (2), 23:device_info (8), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (7), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| a8bc5c45a665 | FIT | 4976 | 64 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (18), 21:event (2), 22:unknown (2), 23:device_info (7), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (18), 288:unknown (2) | 2026-11–2026-11 | CRC-validated in private source set |
| b1423d2f38c4 | FIT | 4210 | 30 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (3), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (3), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| b6c918576b28 | FIT | 4314 | 38 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (7), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (7), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| c4dead5cd732 | FIT | 4210 | 30 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (3), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (3), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| c611a5e83e41 | FIT | 4229 | 32 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (4), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (4), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| cf172de85ff6 | FIT | 4236 | 32 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (4), 21:event (2), 22:unknown (1), 23:device_info (5), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (4), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| d31ae437240e | FIT | 404 | 11 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 162:timestamp_correlation (1), 470:sleep_disruption_severity_period (6), 471:sleep_disruption_overnight_severity (1) | 2026-11–2026-11 | CRC-validated in private source set |
| d87554954875 | FIT | 980 | 36 | 0:file_id (1), 21:event (2), 23:device_info (1), 49:file_creator (1), 273:unknown (1), 275:sleep_level (27), 346:sleep_assessment (1), 382:unknown (1), 521:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| da5957214d47 | FIT | 4889 | 59 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (15), 21:event (2), 22:unknown (2), 23:device_info (8), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 233:unknown (15), 288:unknown (2) | 2026-11–2026-11 | CRC-validated in private source set |
| dd1341c9d5cb | FIT | 4230 | 18 | 0:file_id (1), 26:workout (1), 27:workout_step (13), 49:file_creator (1), 145:memo_glob (2) | no complete timestamp | CRC-validated in private source set |
| dfb28a63ac85 | FIT | 3373 | 14 | 0:file_id (1), 26:workout (1), 27:workout_step (10), 49:file_creator (1), 145:memo_glob (1) | no complete timestamp | CRC-validated in private source set |
| e9d64a29fe03 | FIT | 2028 | 13 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 79:unknown (1), 140:unknown (6), 545:unknown (3) | 2026-11–2026-11 | CRC-validated in private source set |
| ead1b0f7fc64 | FIT | 4599 | 34 | 0:file_id (1), 2:device_settings (1), 3:user_profile (1), 7:zones_target (1), 12:sport (1), 13:training_settings (1), 18:session (1), 19:lap (1), 20:record (6), 21:event (2), 22:unknown (2), 23:device_info (8), 34:activity (1), 49:file_creator (1), 79:unknown (1), 140:unknown (1), 162:timestamp_correlation (1), 216:time_in_zone (2), 288:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| f1bef66b749b | FIT | 628 | 8 | 0:file_id (1), 23:device_info (1), 49:file_creator (1), 241:unknown (1), 284:unknown (1), 356:unknown (1), 369:unknown (1), 410:unknown (1) | 2026-11–2026-11 | CRC-validated in private source set |
| f7466f9e68ae | FIT | 1073 | 83 | 0:file_id (1), 22:unknown (1), 23:device_info (2), 49:file_creator (1), 162:timestamp_correlation (2), 370:hrv_status_summary (1), 371:hrv_value (75) | 2026-11–2026-11 | CRC-validated in private source set |
| fc09b4596c6d | FIT | 1500 | 89 | 0:file_id (1), 1:capabilities (1), 35:software (42), 37:file_capabilities (25), 38:mesg_capabilities (16), 39:field_capabilities (3), 49:file_creator (1) | no complete timestamp | CRC-validated in private source set |
| 9940465a7d75 | XML | 19975 | 0 | — | — | well-formed XML; root=Device; 726 elements; values omitted |
| b0f9e0fc4169 | protobuf_schema_unknown | 229 | 0 | — | — | protobuf wire framing valid; schema/semantics unknown |

## FIT message/field matrix

One row per observed message/field signature. Unmapped profile entries retain their observed numeric ID and base type. Repeated/variable definitions are represented as separate rows when size, scale, offset, or unit differs.

| Global ID | Message (Profile) | Field ID | Field (Profile) | Dev field / index | Observed base type | Size B | Scale | Offset | Unit | Files | Definitions | Records | Valid elements | Invalid/missing | Timestamped records | Missing timestamp | SQLite imported | Mapping / decoder status |
| ---: | --- | ---: | --- | --- | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 0 | file_id | 0 | type | — | enum | 1 | 1 | 0 | — | 31 | 31 | 31 | 31 | 0 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 1 | manufacturer | — | uint16 | 2 | 1 | 0 | — | 31 | 31 | 31 | 31 | 0 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 2 | product | — | uint16 | 2 | 1 | 0 | — | 31 | 31 | 31 | 31 | 0 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 3 | serial_number | — | uint32z | 4 | 1 | 0 | — | 31 | 31 | 31 | 31 | 0 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 4 | time_created | — | uint32 | 4 | 1 | 0 | — | 31 | 31 | 31 | 30 | 1 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 5 | number | — | uint16 | 2 | 1 | 0 | — | 31 | 31 | 31 | 6 | 25 | 0 | 31 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 0 | file_id | 6 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 0 | 2 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 0 | file_id | 7 | — | — | uint32 | 4 | — | — | — | 29 | 29 | 29 | 0 | 29 | 0 | 29 | 0 | unmapped; not imported in observed SQLite rows |
| 1 | capabilities | 1 | sports | — | uint8z | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 1 | capabilities | 21 | workouts_supported | — | uint32z | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 1 | capabilities | 22 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 1 | capabilities | 23 | connectivity_supported | — | uint32z | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 1 | capabilities | 26 | — | — | uint32z | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 0 | active_time_zone | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 1 | utc_offset | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 2 | time_offset | — | uint32 | 4 | 1 | 0 | s | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 3 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 4 | time_mode | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 5 | time_zone_offset | — | sint8 | 1 | 4 | 0 | hr | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 8 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 9 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 10 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 11 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 12 | backlight_mode | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 13 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 14 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 15 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 21 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 26 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 28 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 34 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 35 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 36 | activity_tracker_enabled | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 41 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 42 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 43 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 44 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 45 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 46 | move_alert_enabled | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 47 | date_mode | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 48 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 49 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 55 | display_orientation | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 56 | mounting_side | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 58 | autosync_min_steps | — | uint16 | 2 | 1 | 0 | steps | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 59 | autosync_min_time | — | uint16 | 2 | 1 | 0 | minutes | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 61 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 63 | — | — | enum | 12 | — | — | — | 14 | 14 | 14 | 168 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 64 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 65 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 67 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 68 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 80 | lactate_threshold_autodetect_enabled | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 81 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 82 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 83 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 84 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 86 | ble_auto_upload_enabled | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 87 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 90 | auto_activity_detect | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 93 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 94 | number_of_screens | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 2 | device_settings | 96 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 101 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 104 | — | — | enum | 3 | — | — | — | 14 | 14 | 14 | 14 | 28 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 107 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 108 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 110 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 111 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 112 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 113 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 115 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 124 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 125 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 126 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 127 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 128 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 133 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 135 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 136 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 138 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 140 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 141 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 142 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 143 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 144 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 145 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 149 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 151 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 155 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 156 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 160 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 161 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 163 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 164 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 171 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 172 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 177 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 178 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 179 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 181 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 198 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 200 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 206 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 207 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 208 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 209 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 210 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 211 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 218 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 219 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 220 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 234 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 237 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 238 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 239 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 243 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 2 | device_settings | 244 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 0 | friendly_name | — | string | 25 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 1 | gender | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 3 | height | — | uint8 | 1 | 100 | 0 | m | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 4 | weight | — | uint16 | 2 | 10 | 0 | kg | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 5 | language | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 6 | elev_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 7 | weight_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 8 | resting_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 12 | hr_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 13 | speed_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 14 | dist_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 17 | activity_class | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 18 | position_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 21 | temperature_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 24 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 28 | wake_time | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 29 | sleep_time | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 30 | height_setting | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 31 | user_running_step_length | — | uint16 | 2 | 1000 | 0 | m | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 32 | user_walking_step_length | — | uint16 | 2 | 1000 | 0 | m | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 3 | user_profile | 33 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 36 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 37 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 38 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 41 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 42 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 43 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 44 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 45 | — | — | uint8z | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 52 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 53 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 54 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 57 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 60 | — | — | enum | 7 | — | — | — | 14 | 14 | 14 | 0 | 98 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 61 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 62 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 64 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 65 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 66 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 67 | — | — | string | 252 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 69 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 3 | user_profile | 70 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 1 | max_heart_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 7 | zones_target | 2 | threshold_heart_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 7 | zones_target | 3 | functional_threshold_power | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 7 | zones_target | 5 | hr_calc_type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 7 | zones_target | 7 | pwr_calc_type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 7 | zones_target | 8 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 9 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 10 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 11 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 12 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 7 | zones_target | 254 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 0 | sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 12 | sport | 1 | sub_sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 12 | sport | 3 | name | — | string | 32 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 12 | sport | 4 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 5 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 6 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 10 | — | — | uint8 | 4 | — | — | — | 14 | 14 | 14 | 0 | 56 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 12 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 13 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 12 | sport | 19 | — | — | enum | 3 | — | — | — | 14 | 14 | 14 | 0 | 42 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 1 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 2 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 3 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 4 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 5 | — | — | sint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 6 | — | — | sint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 7 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 8 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 11 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 12 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 13 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 14 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 15 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 22 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 23 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 24 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 27 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 28 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 29 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 30 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 31 | target_distance | — | uint32 | 4 | 100 | 0 | m | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 13 | training_settings | 32 | target_speed | — | uint16 | 2 | 1000 | 0 | m/s | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 13 | training_settings | 33 | target_time | — | uint32 | 4 | 1 | 0 | s | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 13 | training_settings | 34 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 35 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 36 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 46 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 49 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 54 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 55 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 57 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 60 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 65 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 71 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 72 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 76 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 79 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 81 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 82 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 83 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 86 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 87 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 91 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 92 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 93 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 104 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 105 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 109 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 110 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 120 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 123 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 124 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 125 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 156 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 158 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 170 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 171 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 13 | training_settings | 254 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 0 | 14 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 0 | event | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 1 | event_type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 2 | start_time | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 3 | start_position_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 4 | start_position_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 5 | sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 6 | sub_sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 7 | total_elapsed_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 8 | total_timer_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 9 | total_distance | — | uint32 | 4 | 100 | 0 | m | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 10 | total_cycles | — | uint32 | 4 | 1 | 0 | cycles | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 11 | total_calories | — | uint16 | 2 | 1 | 0 | kcal | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 14 | avg_speed | — | uint16 | 2 | 1000 | 0 | m/s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 15 | max_speed | — | uint16 | 2 | 1000 | 0 | m/s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 16 | avg_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 17 | max_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 18 | avg_cadence | — | uint8 | 1 | 1 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 19 | max_cadence | — | uint8 | 1 | 1 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 20 | avg_power | — | uint16 | 2 | 1 | 0 | watts | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 21 | max_power | — | uint16 | 2 | 1 | 0 | watts | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 22 | total_ascent | — | uint16 | 2 | 1 | 0 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 23 | total_descent | — | uint16 | 2 | 1 | 0 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 24 | total_training_effect | — | uint8 | 1 | 10 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 25 | first_lap_index | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 26 | num_laps | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 27 | event_group | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 28 | trigger | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 29 | nec_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 30 | nec_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 31 | swc_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 32 | swc_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 33 | num_lengths | — | uint16 | 2 | 1 | 0 | lengths | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 35 | training_stress_score | — | uint16 | 2 | 10 | 0 | tss | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 38 | end_position_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 39 | end_position_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 41 | avg_stroke_count | — | uint32 | 4 | 10 | 0 | strokes/lap | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 42 | avg_stroke_distance | — | uint16 | 2 | 100 | 0 | m | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 43 | swim_stroke | — | enum | 1 | 1 | 0 | swim_stroke | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 44 | pool_length | — | uint16 | 2 | 100 | 0 | m | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 46 | pool_length_unit | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 47 | num_active_lengths | — | uint16 | 2 | 1 | 0 | lengths | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 57 | avg_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 58 | max_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 59 | total_moving_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 78 | active_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 79 | avg_strokes_per_length | — | uint16 | 2 | 10 | 0 | strokes/length | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 80 | avg_swolf | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 81 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 92 | avg_fractional_cadence | — | uint8 | 1 | 128 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 93 | max_fractional_cadence | — | uint8 | 1 | 128 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 109 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 110 | sport_profile_name | — | string | 128 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 124 | enhanced_avg_speed | — | uint32 | 4 | 1000 | 0 | m/s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 125 | enhanced_max_speed | — | uint32 | 4 | 1000 | 0 | m/s | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 126 | enhanced_avg_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 127 | enhanced_min_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 128 | enhanced_max_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 134 | avg_step_length | — | uint16 | 2 | 10 | 0 | mm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 137 | total_anaerobic_training_effect | — | uint8 | 1 | 10 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 138 | — | — | uint8 | 2 | — | — | — | 14 | 14 | 14 | 28 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 147 | avg_respiration_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 150 | min_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 151 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 152 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 157 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 158 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 168 | training_load_peak | — | sint32 | 4 | 65536 | 0 | — | 14 | 14 | 14 | 8 | 6 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 169 | enhanced_avg_respiration_rate | — | uint16 | 2 | 100 | 0 | Breaths/min | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 170 | enhanced_max_respiration_rate | — | uint16 | 2 | 100 | 0 | Breaths/min | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 176 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 177 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 178 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 179 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 180 | enhanced_min_respiration_rate | — | uint16 | 2 | 100 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 184 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 188 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 189 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 190 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 192 | workout_feel | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 193 | workout_rpe | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 194 | avg_spo2 | — | uint8 | 1 | 1 | 0 | percent | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 195 | avg_stress | — | uint8 | 1 | 1 | 0 | percent | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 196 | metabolic_calories | — | uint16 | 2 | 1 | 0 | kcal | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 197 | sdrr_hrv | — | uint8 | 1 | 1 | 0 | mS | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 198 | rmssd_hrv | — | uint8 | 1 | 1 | 0 | mS | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 201 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 202 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 215 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 12 | 2 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 216 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 12 | 2 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 217 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 218 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 227 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 228 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 18 | session | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 18 | session | 254 | message_index | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 0 | event | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 1 | event_type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 2 | start_time | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 3 | start_position_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 4 | start_position_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 5 | end_position_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 6 | end_position_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 7 | total_elapsed_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 8 | total_timer_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 9 | total_distance | — | uint32 | 4 | 100 | 0 | m | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 10 | total_cycles | — | uint32 | 4 | 1 | 0 | cycles | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 11 | total_calories | — | uint16 | 2 | 1 | 0 | kcal | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 12 | total_fat_calories | — | uint16 | 2 | 1 | 0 | kcal | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 13 | avg_speed | — | uint16 | 2 | 1000 | 0 | m/s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 14 | max_speed | — | uint16 | 2 | 1000 | 0 | m/s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 15 | avg_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 16 | max_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 17 | avg_cadence | — | uint8 | 1 | 1 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 18 | max_cadence | — | uint8 | 1 | 1 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 21 | total_ascent | — | uint16 | 2 | 1 | 0 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 22 | total_descent | — | uint16 | 2 | 1 | 0 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 23 | intensity | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 24 | lap_trigger | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 25 | sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 26 | event_group | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 27 | nec_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 28 | nec_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 29 | swc_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 30 | swc_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 32 | num_lengths | — | uint16 | 2 | 1 | 0 | lengths | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 35 | first_length_index | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 37 | avg_stroke_distance | — | uint16 | 2 | 100 | 0 | m | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 38 | swim_stroke | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 39 | sub_sport | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 40 | num_active_lengths | — | uint16 | 2 | 1 | 0 | lengths | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 50 | avg_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 51 | max_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 52 | total_moving_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 70 | active_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 71 | wkt_step_index | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 72 | — | — | enum | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 19 | lap | 73 | avg_swolf | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 80 | avg_fractional_cadence | — | uint8 | 1 | 128 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 81 | max_fractional_cadence | — | uint8 | 1 | 128 | 0 | rpm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 90 | avg_strokes_per_length | — | uint16 | 2 | 10 | 0 | strokes / length | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 110 | enhanced_avg_speed | — | uint32 | 4 | 1000 | 0 | m/s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 111 | enhanced_max_speed | — | uint32 | 4 | 1000 | 0 | m/s | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 112 | enhanced_avg_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 113 | enhanced_min_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 114 | enhanced_max_altitude | — | uint32 | 4 | 5 | 500 | m | 14 | 14 | 14 | 6 | 8 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 120 | avg_step_length | — | uint16 | 2 | 10 | 0 | mm | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 124 | min_temperature | — | sint8 | 1 | 1 | 0 | C | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 125 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 19 | lap | 126 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 19 | lap | 136 | enhanced_avg_respiration_rate | — | uint16 | 2 | 100 | 0 | Breaths/min | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 137 | enhanced_max_respiration_rate | — | uint16 | 2 | 100 | 0 | Breaths/min | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 145 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 19 | lap | 155 | metabolic_calories | — | uint16 | 2 | 1 | 0 | kcal | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 19 | lap | 254 | message_index | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 20 | record | 0 | position_lat | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 20 | 129 | 40 | 89 | 129 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 20 | record | 1 | position_long | — | sint32 | 4 | 1 | 0 | semicircles | 14 | 20 | 129 | 40 | 89 | 129 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 20 | record | 3 | heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 14 | 20 | 129 | 129 | 0 | 129 | 0 | 129 | Garmin FIT Profile 21.222.0; 129/129 matched |
| 20 | record | 5 | distance | — | uint32 | 4 | 100 | 0 | m | 14 | 20 | 129 | 129 | 0 | 129 | 0 | 129 | Garmin FIT Profile 21.222.0; 129/129 matched |
| 20 | record | 73 | enhanced_speed | — | uint32 | 4 | 1000 | 0 | m/s | 6 | 6 | 36 | 36 | 44 | 36 | 0 | 36 | Garmin FIT Profile 21.222.0; 36/36 matched |
| 20 | record | 78 | enhanced_altitude | — | uint32 | 4 | 5 | 500 | m | 6 | 6 | 36 | 36 | 44 | 36 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 20 | record | 135 | — | — | uint8 | 1 | — | — | — | 14 | 20 | 129 | 129 | 0 | 129 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 20 | record | 136 | — | — | uint8 | 1 | — | — | — | 14 | 20 | 129 | 129 | 0 | 129 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 20 | record | 143 | — | — | uint8 | 1 | — | — | — | 12 | 18 | 124 | 124 | 0 | 124 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 20 | record | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 14 | 20 | 129 | 129 | 0 | 129 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 0 | event | — | enum | 1 | 1 | 0 | — | 17 | 20 | 36 | 36 | 0 | 36 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 1 | event_type | — | enum | 1 | 1 | 0 | — | 17 | 20 | 36 | 36 | 0 | 36 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 2 | data16 | — | uint16 | 2 | 1 | 0 | — | 2 | 5 | 6 | 6 | 0 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 3 | data | — | uint32 | 4 | 1 | 0 | — | 15 | 15 | 30 | 30 | 0 | 30 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 4 | event_group | — | uint8 | 1 | 1 | 0 | — | 15 | 15 | 30 | 28 | 2 | 30 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 15 | start_timestamp | — | uint32 | 4 | 1 | 0 | s | 15 | 15 | 30 | 0 | 30 | 30 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 21 | event | 19 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 30 | 0 | 30 | 30 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 21 | event | 20 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 30 | 0 | 30 | 30 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 21 | event | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 17 | 20 | 36 | 36 | 0 | 36 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 22 | — | 0 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 6 | 15 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 1 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 6 | 15 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 2 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 0 | 21 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 3 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 6 | 15 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 4 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 21 | 0 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 5 | — | — | enum | 1 | — | — | — | 15 | 15 | 21 | 20 | 1 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 14 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 0 | 21 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 15 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 21 | 0 | 21 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 22 | — | 253 | — | — | uint32 | 4 | — | — | — | 15 | 15 | 21 | 21 | 0 | 21 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 0 | device_index | — | uint8 | 1 | 1 | 0 | — | 24 | 24 | 98 | 89 | 9 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 1 | device_type | — | uint8 | 1 | 1 | 0 | — | 24 | 24 | 98 | 60 | 38 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 2 | manufacturer | — | uint16 | 2 | 1 | 0 | — | 26 | 26 | 100 | 36 | 64 | 100 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 3 | serial_number | — | uint32z | 4 | 1 | 0 | — | 26 | 26 | 100 | 40 | 60 | 100 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 4 | product | — | uint16 | 2 | 1 | 0 | — | 26 | 26 | 100 | 40 | 60 | 100 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 5 | software_version | — | uint16 | 2 | 100 | 0 | — | 26 | 26 | 100 | 68 | 32 | 100 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 6 | hardware_version | — | uint8 | 1 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 7 | cum_operating_time | — | uint32 | 4 | 1 | 0 | s | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 8 | — | — | uint32 | 4 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 9 | — | — | uint8 | 1 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 10 | battery_voltage | — | uint16 | 2 | 256 | 0 | V | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 11 | battery_status | — | uint8 | 1 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 13 | — | — | uint16 | 2 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 15 | — | — | uint32 | 4 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 16 | — | — | uint32 | 4 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 17 | — | — | string | 32 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 18 | sensor_position | — | enum | 1 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 20 | ant_transmission_type | — | uint8z | 1 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 21 | ant_device_number | — | uint16z | 2 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 22 | ant_network | — | enum | 1 | 1 | 0 | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 23 | — | — | uint8 | 1 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 24 | — | — | uint32z | 4 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 25 | source_type | — | enum | 1 | 1 | 0 | — | 24 | 24 | 98 | 89 | 9 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 29 | — | — | uint8 | 6 | — | — | — | 24 | 24 | 98 | 0 | 588 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 30 | — | — | uint8 | 1 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 31 | — | — | uint32 | 4 | — | — | — | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 23 | device_info | 32 | battery_level | — | uint8 | 1 | 1 | 0 | % | 24 | 24 | 98 | 0 | 98 | 98 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 23 | device_info | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 26 | 26 | 100 | 100 | 0 | 100 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 24 | — | 2 | — | — | byte | 27 | — | — | — | 2 | 10 | 85 | 2295 | 0 | 0 | 85 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 4 | sport | — | enum | 1 | 1 | 0 | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 5 | capabilities | — | uint32z | 4 | 1 | 0 | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 6 | num_valid_steps | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 7 | — | — | uint16 | 2 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 8 | wkt_name | — | string | 32 | 1 | 0 | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 9 | — | — | enum | 1 | — | — | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 10 | — | — | uint32 | 4 | — | — | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 11 | sub_sport | — | enum | 1 | 1 | 0 | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 12 | — | — | uint16 | 2 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 13 | — | — | enum | 1 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 14 | pool_length | — | uint16 | 2 | 100 | 0 | m | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 15 | pool_length_unit | — | enum | 1 | 1 | 0 | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 16 | — | — | byte | 16 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 17 | wkt_description | — | string | 160 | 1 | 0 | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 26 | workout | 19 | — | — | enum | 1 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 20 | — | — | uint16 | 2 | — | — | — | 4 | 4 | 4 | 0 | 4 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 26 | workout | 29 | — | — | string | 9 | — | — | — | 4 | 4 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 0 | wkt_step_name | — | string | 25 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 1 | duration_type | — | enum | 1 | 1 | 0 | — | 4 | 4 | 40 | 40 | 0 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 2 | duration_value | — | uint32 | 4 | 1 | 0 | — | 4 | 4 | 40 | 40 | 0 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 3 | target_type | — | enum | 1 | 1 | 0 | — | 4 | 4 | 40 | 27 | 13 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 4 | target_value | — | uint32 | 4 | 1 | 0 | — | 4 | 4 | 40 | 9 | 31 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 5 | custom_target_value_low | — | uint32 | 4 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 6 | custom_target_value_high | — | uint32 | 4 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 7 | intensity | — | enum | 1 | 1 | 0 | — | 4 | 4 | 40 | 31 | 9 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 8 | notes | — | string | 200 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 9 | equipment | — | enum | 1 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 10 | exercise_category | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 11 | exercise_name | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 12 | exercise_weight | — | uint16 | 2 | 100 | 0 | kg | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 13 | weight_display_unit | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 27 | workout_step | 17 | — | — | enum | 1 | — | — | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 18 | — | — | enum | 1 | — | — | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 26 | — | — | uint8 | 1 | — | — | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 27 | — | — | uint16 | 2 | — | — | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 31 | — | — | uint16 | 2 | — | — | — | 4 | 4 | 40 | 0 | 40 | 0 | 40 | 0 | unmapped; not imported in observed SQLite rows |
| 27 | workout_step | 254 | message_index | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 40 | 40 | 0 | 0 | 40 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 0 | total_timer_time | — | uint32 | 4 | 1000 | 0 | s | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 1 | num_sessions | — | uint16 | 2 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 2 | type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 3 | event | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 4 | event_type | — | enum | 1 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 5 | local_timestamp | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 6 | event_group | — | uint8 | 1 | 1 | 0 | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 34 | activity | 7 | — | — | uint8 | 1 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 34 | activity | 8 | — | — | string | 64 | — | — | — | 14 | 14 | 14 | 0 | 14 | 14 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 34 | activity | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 14 | 14 | 14 | 14 | 0 | 14 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 35 | software | 3 | version | — | uint16 | 2 | 100 | 0 | — | 3 | 3 | 44 | 44 | 0 | 0 | 44 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 35 | software | 5 | part_number | — | string | 12 | 1 | 0 | — | 1 | 1 | 42 | 42 | 0 | 0 | 42 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 37 | file_capabilities | 0 | type | — | enum | 1 | 1 | 0 | — | 1 | 1 | 25 | 25 | 0 | 0 | 25 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 37 | file_capabilities | 1 | flags | — | uint8z | 1 | 1 | 0 | — | 1 | 1 | 25 | 25 | 0 | 0 | 25 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 37 | file_capabilities | 2 | directory | — | string | 16 | 1 | 0 | — | 1 | 1 | 25 | 24 | 1 | 0 | 25 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 37 | file_capabilities | 3 | max_count | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 25 | 25 | 0 | 0 | 25 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 38 | mesg_capabilities | 0 | file | — | enum | 1 | 1 | 0 | — | 1 | 1 | 16 | 16 | 0 | 0 | 16 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 38 | mesg_capabilities | 1 | mesg_num | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 16 | 16 | 0 | 0 | 16 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 38 | mesg_capabilities | 2 | count_type | — | enum | 1 | 1 | 0 | — | 1 | 1 | 16 | 16 | 0 | 0 | 16 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 38 | mesg_capabilities | 3 | count | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 16 | 16 | 0 | 0 | 16 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 39 | field_capabilities | 0 | file | — | enum | 1 | 1 | 0 | — | 1 | 1 | 3 | 3 | 0 | 0 | 3 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 39 | field_capabilities | 1 | mesg_num | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 3 | 3 | 0 | 0 | 3 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 39 | field_capabilities | 2 | field_num | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 3 | 3 | 0 | 0 | 3 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 39 | field_capabilities | 3 | count | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 3 | 3 | 0 | 0 | 3 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 49 | file_creator | 0 | software_version | — | uint16 | 2 | 1 | 0 | — | 29 | 29 | 29 | 29 | 0 | 0 | 29 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 49 | file_creator | 1 | hardware_version | — | uint8 | 1 | 1 | 0 | — | 29 | 29 | 29 | 0 | 29 | 0 | 29 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 49 | file_creator | 2 | — | — | string | 20 | — | — | — | 29 | 29 | 29 | 0 | 29 | 0 | 29 | 0 | unmapped; not imported in observed SQLite rows |
| 49 | file_creator | 3 | — | — | enum | 1 | — | — | — | 29 | 29 | 29 | 0 | 29 | 0 | 29 | 0 | unmapped; not imported in observed SQLite rows |
| 49 | file_creator | 4 | — | — | enum | 1 | — | — | — | 29 | 29 | 29 | 0 | 29 | 0 | 29 | 0 | unmapped; not imported in observed SQLite rows |
| 55 | monitoring | 2 | distance | — | uint32 | 4 | 100 | 0 | m | 2 | 3 | 6 | 6 | 641 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 3 | cycles | — | uint32 | 4 | 2 | 0 | cycles | 2 | 10 | 41 | 41 | 606 | 6 | 35 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 4 | active_time | — | uint32 | 4 | 1000 | 0 | s | 2 | 10 | 41 | 41 | 606 | 6 | 35 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 5 | activity_type | — | enum | 1 | 1 | 0 | — | 2 | 3 | 6 | 6 | 641 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 19 | active_calories | — | uint16 | 2 | 1 | 0 | kcal | 2 | 10 | 41 | 41 | 606 | 6 | 35 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 24 | current_activity_type_intensity | — | byte | 1 | 1\|1 | 0\|0 | \| | 2 | 15 | 166 | 166 | 481 | 131 | 35 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 26 | timestamp_16 | — | uint16 | 2 | 1 | 0 | s | 2 | 18 | 506 | 506 | 141 | 0 | 506 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 27 | heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 2 | 8 | 467 | 467 | 180 | 0 | 467 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 29 | duration_min | — | uint16 | 2 | 1 | 0 | min | 2 | 3 | 6 | 6 | 641 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 33 | moderate_activity_minutes | — | uint16 | 2 | 1 | 0 | minutes | 1 | 2 | 3 | 3 | 503 | 0 | 3 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 34 | vigorous_activity_minutes | — | uint16 | 2 | 1 | 0 | minutes | 1 | 1 | 1 | 1 | 505 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 55 | monitoring | 37 | — | — | uint16 | 2 | — | — | — | 2 | 6 | 7 | 7 | 640 | 4 | 3 | 0 | unmapped; not imported in observed SQLite rows |
| 55 | monitoring | 38 | — | — | uint16 | 2 | — | — | — | 2 | 5 | 5 | 5 | 642 | 4 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 55 | monitoring | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 15 | 141 | 141 | 506 | 141 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 79 | — | 0 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 1 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 2 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 3 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 4 | — | — | enum | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 5 | — | — | enum | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 6 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 7 | — | — | sint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 8 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 9 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 10 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 14 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 15 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 16 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 17 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 18 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 19 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 20 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 21 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 22 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 23 | — | — | sint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 24 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 25 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 26 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 27 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 28 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 29 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 30 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 31 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 32 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 33 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 34 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 35 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 36 | — | — | sint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 38 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 39 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 40 | — | — | enum | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 41 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 42 | — | — | uint8 | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 43 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 44 | — | — | enum | 1 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 45 | — | — | enum | 1 | — | — | — | 16 | 16 | 16 | 0 | 16 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 47 | — | — | uint16 | 2 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 79 | — | 253 | — | — | uint32 | 4 | — | — | — | 16 | 16 | 16 | 16 | 0 | 16 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 103 | monitoring_info | 0 | local_timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 103 | monitoring_info | 1 | activity_type | — | enum | 3 | 1 | 0 | — | 2 | 2 | 2 | 6 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 103 | monitoring_info | 3 | cycles_to_distance | — | uint16 | 6 | 5000 | 0 | m/cycle | 2 | 2 | 2 | 6 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 103 | monitoring_info | 4 | cycles_to_calories | — | uint16 | 6 | 5000 | 0 | kcal/cycle | 2 | 2 | 2 | 6 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 103 | monitoring_info | 5 | resting_metabolic_rate | — | uint16 | 2 | 1 | 0 | kcal / day | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 103 | monitoring_info | 7 | — | — | uint32 | 16 | — | — | — | 2 | 2 | 2 | 8 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 103 | monitoring_info | 8 | — | — | enum | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 103 | monitoring_info | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 140 | — | 0 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 1 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 2 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 3 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 4 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 5 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 6 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 7 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 8 | — | — | sint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 9 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 10 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 11 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 12 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 13 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 17 | — | — | sint8 | 1 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 18 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 19 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 20 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 21 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 22 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 23 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 24 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 25 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 26 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 27 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 28 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 29 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 30 | — | — | sint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 31 | — | — | sint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 32 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 33 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 34 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 35 | — | — | uint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 36 | — | — | uint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 37 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 38 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 39 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 40 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 41 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 42 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 6 | 14 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 43 | — | — | sint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 44 | — | — | sint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 45 | — | — | sint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 46 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 48 | — | — | uint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 49 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 50 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 51 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 52 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 9 | 11 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 53 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 54 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 55 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 56 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 57 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 5 | 15 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 58 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 5 | 15 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 59 | — | — | enum | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 60 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 3 | 17 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 61 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 3 | 17 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 62 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 6 | 14 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 63 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 64 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 65 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 66 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 67 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 14 | 6 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 68 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 69 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 70 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 71 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 72 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 73 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 74 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 75 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 76 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 77 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 0 | 20 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 78 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 6 | 14 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 79 | — | — | uint8 | 1 | — | — | — | 15 | 15 | 20 | 6 | 14 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 82 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 83 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 84 | — | — | sint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 85 | — | — | uint16 | 2 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 140 | — | 253 | — | — | uint32 | 4 | — | — | — | 15 | 15 | 20 | 20 | 0 | 20 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 145 | memo_glob | 1 | mesg_num | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 4 | 4 | 3 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 2 | parent_index | — | uint16 | 2 | 1 | 0 | — | 4 | 4 | 4 | 4 | 3 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 3 | field_num | — | uint8 | 1 | 1 | 0 | — | 4 | 4 | 4 | 4 | 3 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 4 | data | — | uint8z | 43\|255 | 1 | 0 | — | 2 | 4 | 4 | 594 | 2 | 0 | 4 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 4 | data | — | uint8z | 33\|255 | 1 | 0 | — | 1 | 2 | 2 | 287 | 1 | 0 | 2 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 4 | data | — | uint8z | 237 | 1 | 0 | — | 1 | 1 | 1 | 236 | 1 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 145 | memo_glob | 250 | part_index | — | uint32 | 4 | 1 | 0 | — | 4 | 7 | 7 | 7 | 0 | 0 | 7 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 162 | timestamp_correlation | 1 | system_timestamp | — | uint32 | 4 | 1 | 0 | s | 17 | 17 | 19 | 17 | 2 | 19 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 162 | timestamp_correlation | 3 | local_timestamp | — | uint32 | 4 | 1 | 0 | s | 17 | 17 | 19 | 19 | 0 | 19 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 162 | timestamp_correlation | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 17 | 17 | 19 | 19 | 0 | 19 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 188 | ohr_settings | 0 | enabled | — | enum | 1 | 1 | 0 | — | 2 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 188 | ohr_settings | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 211 | monitoring_hr_data | 0 | resting_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 2 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 211 | monitoring_hr_data | 1 | current_day_resting_heart_rate | — | uint8 | 1 | 1 | 0 | bpm | 2 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 211 | monitoring_hr_data | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 0 | reference_mesg | — | uint16 | 2 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 1 | reference_index | — | uint16 | 2 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 2 | time_in_hr_zone | — | uint32 | 28 | 1000 | 0 | s | 14 | 28 | 28 | 196 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 6 | hr_zone_high_boundary | — | uint8 | 6 | 1 | 0 | bpm | 14 | 28 | 28 | 168 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 10 | hr_calc_type | — | enum | 1 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 11 | max_heart_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 12 | resting_heart_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 13 | threshold_heart_rate | — | uint8 | 1 | 1 | 0 | — | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 14 | pwr_calc_type | — | enum | 1 | 1 | 0 | — | 14 | 28 | 28 | 0 | 28 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 15 | functional_threshold_power | — | uint16 | 2 | 1 | 0 | — | 14 | 28 | 28 | 0 | 28 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 216 | time_in_zone | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 14 | 28 | 28 | 28 | 0 | 28 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 227 | stress_level | 0 | stress_level_value | — | sint16 | 2 | 1 | 0 | — | 2 | 8 | 576 | 576 | 0 | 0 | 576 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 227 | stress_level | 1 | stress_level_time | — | uint32 | 4 | 1 | 0 | s | 2 | 8 | 576 | 576 | 0 | 0 | 576 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 227 | stress_level | 2 | — | — | sint8 | 1 | — | — | — | 2 | 8 | 576 | 576 | 0 | 0 | 576 | 0 | unmapped; not imported in observed SQLite rows |
| 227 | stress_level | 3 | — | — | sint8 | 1 | — | — | — | 2 | 8 | 576 | 576 | 0 | 0 | 576 | 0 | unmapped; not imported in observed SQLite rows |
| 227 | stress_level | 4 | — | — | sint8 | 1 | — | — | — | 2 | 8 | 576 | 576 | 0 | 0 | 576 | 0 | unmapped; not imported in observed SQLite rows |
| 233 | — | 2 | — | — | byte | 4 | — | — | — | 15 | 22 | 704 | 2816 | 0 | 0 | 704 | 0 | unmapped; not imported in observed SQLite rows |
| 241 | — | 0 | — | — | uint32 | 4 | — | — | — | 4 | 4 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 241 | — | 1 | — | — | enum | 1 | — | — | — | 4 | 4 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 241 | — | 253 | — | — | uint32 | 4 | — | — | — | 4 | 4 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 269 | spo2_data | 0 | reading_spo2 | — | uint8 | 1 | 1 | 0 | percent | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 269 | spo2_data | 1 | reading_confidence | — | uint8 | 1 | 1 | 0 | — | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 269 | spo2_data | 2 | mode | — | uint8 | 1 | 1 | 0 | — | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 269 | spo2_data | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 273 | — | 0 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 273 | — | 1 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 273 | — | 2 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 273 | — | 3 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 273 | — | 4 | — | — | string | 32 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 273 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 275 | sleep_level | 0 | sleep_level | — | enum | 1 | 1 | 0 | — | 1 | 1 | 27 | 27 | 0 | 27 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 275 | sleep_level | 253 | timestamp | — | uint32 | 4 | 1 | 0 | s | 1 | 1 | 27 | 27 | 0 | 27 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 282 | — | 1 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 282 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 0 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 1 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 2 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 3 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 4 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 284 | — | 5 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 288 | — | 0 | — | — | sint8 | 1 | — | — | — | 14 | 14 | 19 | 19 | 0 | 19 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 288 | — | 1 | — | — | sint8 | 1 | — | — | — | 14 | 14 | 19 | 19 | 0 | 19 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 288 | — | 2 | — | — | uint16 | 2 | — | — | — | 14 | 14 | 19 | 0 | 19 | 19 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 288 | — | 253 | — | — | uint32 | 4 | — | — | — | 14 | 14 | 19 | 19 | 0 | 19 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 297 | respiration_rate | 0 | respiration_rate | — | sint16 | 2 | 100 | 0 | breaths/min | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 297 | respiration_rate | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 2 | 8 | 576 | 576 | 0 | 576 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 330 | — | 0 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 330 | — | 1 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 330 | — | 2 | — | — | sint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 330 | — | 3 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 346 | sleep_assessment | 0 | combined_awake_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 1 | awake_time_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 2 | awakenings_count_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 3 | deep_sleep_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 4 | sleep_duration_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 5 | light_sleep_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 6 | overall_sleep_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 7 | sleep_quality_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 8 | sleep_recovery_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 9 | rem_sleep_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 10 | sleep_restlessness_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 11 | awakenings_count | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 12 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 346 | sleep_assessment | 13 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 346 | sleep_assessment | 14 | interruptions_score | — | uint8 | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 15 | average_stress_during_sleep | — | uint16 | 2 | 100 | 0 | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 346 | sleep_assessment | 16 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 355 | — | 0 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 355 | — | 1 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 355 | — | 2 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 0 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 1 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 2 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 3 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 4 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 5 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 6 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 7 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 8 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 9 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 10 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 0 | 2 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 11 | — | — | enum | 1 | — | — | — | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 12 | — | — | enum | 1 | — | — | — | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 356 | — | 253 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 2 | 2 | 0 | 2 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 0 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 1 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 2 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 3 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 4 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 2 | 1 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 5 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 6 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 7 | — | — | uint16 | 2 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 8 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 9 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 10 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 11 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 12 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 13 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 14 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 15 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 16 | — | — | uint8 | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 17 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 18 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 19 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 20 | — | — | uint32 | 4 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 21 | — | — | uint16 | 2 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 22 | — | — | uint16 | 2 | — | — | — | 3 | 3 | 3 | 0 | 3 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 25 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 26 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 27 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 28 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 29 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 30 | — | — | enum | 1 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 369 | — | 253 | — | — | uint32 | 4 | — | — | — | 3 | 3 | 3 | 3 | 0 | 3 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 0 | weekly_average | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 1 | last_night_average | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 2 | last_night_5_min_high | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 3 | baseline_low_upper | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 4 | baseline_balanced_lower | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 5 | baseline_balanced_upper | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 6 | status | — | enum | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 7 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 8 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 370 | hrv_status_summary | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 371 | hrv_value | 0 | value | — | uint16 | 2 | 128 | 0 | ms | 1 | 1 | 75 | 75 | 0 | 75 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 371 | hrv_value | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 75 | 75 | 0 | 75 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 382 | — | 0 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 382 | — | 1 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 382 | — | 2 | — | — | uint8 | 200 | — | — | — | 1 | 1 | 1 | 22 | 178 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 383 | — | 0 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 383 | — | 1 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 383 | — | 2 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 383 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 0 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 1 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 2 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 3 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 4 | — | — | sint8 | 1 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 5 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 6 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 7 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 8 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 9 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 10 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 11 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 12 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 13 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 0 | 5 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 14 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 15 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 16 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 17 | — | — | enum | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 18 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 19 | — | — | sint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 20 | — | — | sint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 21 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 22 | — | — | sint16 | 2 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 23 | — | — | sint8 | 1 | — | — | — | 2 | 2 | 5 | 2 | 3 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 24 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 25 | — | — | sint8 | 1 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 253 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 384 | — | 254 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 5 | 4 | 1 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 0 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 1 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 2 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 3 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 252 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 391 | — | 254 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 8 | 8 | 0 | 8 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 397 | — | 1 | — | — | float32 | 4 | — | — | — | 1 | 1 | 1459 | 1459 | 0 | 1459 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 397 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1459 | 1459 | 0 | 1459 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 0 | local_timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 1 | average_deviation | — | float32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 2 | average_7_day_deviation | — | float32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 3 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 4 | nightly_value | — | float32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 398 | skin_temp_overnight | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 401 | — | 0 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 401 | — | 1 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 0 | 1 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 401 | — | 2 | — | — | uint16 | 56 | — | — | — | 1 | 1 | 1 | 1 | 27 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 401 | — | 3 | — | — | uint16 | 56 | — | — | — | 1 | 1 | 1 | 0 | 28 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 401 | — | 4 | — | — | uint16 | 56 | — | — | — | 1 | 1 | 1 | 0 | 28 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 401 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 0 | 1 | 0 | 1 | 0 | unmapped; not imported in observed SQLite rows |
| 408 | — | 0 | — | — | enum | 1 | — | — | — | 2 | 4 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 408 | — | 1 | — | — | uint16 | 2 | — | — | — | 2 | 4 | 5 | 4 | 1 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 408 | — | 2 | — | — | uint16 | 2 | — | — | — | 2 | 4 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 408 | — | 253 | — | — | uint32 | 4 | — | — | — | 2 | 4 | 5 | 5 | 0 | 5 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 0 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 1 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 2 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 3 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 4 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 5 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 6 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 7 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 8 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 9 | — | — | enum | 1 | — | — | — | 2 | 2 | 4 | 0 | 4 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 410 | — | 253 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 4 | 4 | 0 | 4 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 0 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 1 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 2 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 3 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 4 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 5 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 6 | — | — | enum | 1 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 7 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 8 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 9 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 10 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 411 | — | 254 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 4 | 4 | 0 | 0 | 4 | 0 | unmapped; not imported in observed SQLite rows |
| 412 | nap_event | 0 | start_time | — | uint32 | 4 | 1 | 0 | seconds | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 1 | start_timezone_offset | — | sint16 | 2 | 1 | 0 | minutes | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 2 | end_time | — | uint32 | 4 | 1 | 0 | seconds | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 3 | end_timezone_offset | — | sint16 | 2 | 1 | 0 | minutes | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 4 | feedback | — | enum | 1 | 1 | 0 | — | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 5 | is_deleted | — | enum | 1 | 1 | 0 | — | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 6 | source | — | enum | 1 | 1 | 0 | — | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 7 | update_timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 8 | 0 | 8 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 412 | nap_event | 254 | message_index | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 8 | 8 | 0 | 0 | 8 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 413 | — | 0 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 1 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 2 | — | — | sint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 3 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 4 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 5 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 413 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 0 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 1 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 2 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 3 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 4 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 5 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 6 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 7 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 8 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 430 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 431 | — | 0 | — | — | sint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 431 | — | 1 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 431 | — | 2 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 431 | — | 3 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 431 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 470 | sleep_disruption_severity_period | 0 | severity | — | enum | 1 | 1 | 0 | — | 1 | 1 | 6 | 5 | 1 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 470 | sleep_disruption_severity_period | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 6 | 6 | 0 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 470 | sleep_disruption_severity_period | 254 | message_index | — | uint16 | 2 | 1 | 0 | — | 1 | 1 | 6 | 6 | 0 | 6 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 471 | sleep_disruption_overnight_severity | 0 | severity | — | enum | 1 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 471 | sleep_disruption_overnight_severity | 253 | timestamp | — | uint32 | 4 | 1 | 0 | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | Garmin FIT Profile 21.222.0; not imported in observed SQLite rows |
| 484 | — | 0 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 2 | 2 | 0 | 0 | 2 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 0 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 1 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 2 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 3 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 4 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 5 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 506 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 0 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 1 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 2 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 3 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 4 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 5 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 6 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 7 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 8 | — | — | uint16 | 96 | — | — | — | 1 | 1 | 1 | 48 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 9 | — | — | uint8 | 48 | — | — | — | 1 | 1 | 1 | 48 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 10 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 11 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 12 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 0 | 1 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 13 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 507 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 0 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 1 | — | — | uint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 2 | — | — | sint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 3 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 4 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 5 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 6 | — | — | sint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 7 | — | — | sint8 | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 8 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 9 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 10 | — | — | uint16 | 2 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 11 | — | — | enum | 1 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 521 | — | 253 | — | — | uint32 | 4 | — | — | — | 1 | 1 | 1 | 1 | 0 | 1 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 0 | — | — | enum | 1 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 1 | — | — | enum | 1 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 2 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 3 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 4 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 5 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 6 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 7 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 8 | — | — | sint32 | 4 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 9 | — | — | uint16 | 2 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 10 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 11 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 6 | 4 | 2 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 12 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 6 | 4 | 2 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 13 | — | — | uint8 | 1 | — | — | — | 2 | 2 | 6 | 4 | 2 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |
| 545 | — | 253 | — | — | uint32 | 4 | — | — | — | 2 | 2 | 6 | 6 | 0 | 6 | 0 | 0 | unmapped; not imported in observed SQLite rows |

## SQLite import coverage

Read-only database check: `ok`; 294 rows; 0 duplicate primary keys.

Current decoder candidate keys: 294; matching keys in SQLite: 294. Counts and key matches are shown without exporting values or exact timestamps.

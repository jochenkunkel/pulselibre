# S10 dritter Sync-Mitschnitt: Korrektur und Fortsetzung der Offline-Analyse (2026-11-21)

- Analyst: OpenCode (BLE/Protokoll)
- Quelle: `/tmp/opencode/private_s10_20261121/s10-third-sync-224125.{zip,hci}`, vom FIT-Agenten nach einem vollständigen Garmin-Connect-Sync erzeugt
- Methode: rein offline; vorhandener redaktierender Parser des FIT-Agenten (`analyze_capture_metadata.py`, sequenzbewusst) und eigener Metadatenparser
- Gemeldete Geräteabfolge (FIT-Agent): Garmin Connect geöffnet → Geräteberechtigung zugelassen → Gerät gefunden → verbunden und gekoppelt → Sync gestartet → abgeschlossen

## 1. Wichtige Korrektur

Die in `CIRQA_S10_AFTER_SYNC_HCI_ANALYSIS_2026-11-21.md` getroffene Aussage „kein `5043`/`5044`, keine Item-List, kein Pull“ war **falsch**. Sie beruhte auf einer Parser-Grenze: Der dortige Rekonstruktionspfad betrachtete nur den logischen ATT-Handle `0x40` und konkatenierte ML-Werte ohne Sequenznummern und ACKs. Die relevanten Daten laufen jedoch über die ML-Datenkanal-Handles 3 und 4 mit eigener Sequenzierung. Der sequenzbewusste Parser des FIT-Agenten findet `5043`/`5044` und FileAccess-Nachrichten bereits im zweiten Mitschnitt. Die frühere Analyse ist damit nicht nur unvollständig, sondern in diesem Punkt zu widerrufen. Alle anderen dort genannten Zahlen (H4-/ACL-/ATT-Zählung, SMP, GATT, 5024-ACKs, Kontrollvergleich) bleiben bestehen.

## 2. Dritter Mitschnitt: Aufnahme und Umfang

| Merkmal | Wert |
|---|---:|
| HCI-Datei | 713.307 Bytes (btsnoop v1, Datalink 1002/H4) |
| Zuvor | 369.074 Bytes (`s10-snoop-after-sync-2217.hci`) |
| Zuwachs | 344.233 Bytes |
| Exaktes Prefix | ja — der vollständige frühere Mitschnitt ist Byte für Byte der Anfang des neuen; keine Abweichung |
| Validator | `records=11121`, `acl_records=5322`, `complete_att_candidates=5032`, `sufficient_for_att_analysis=true` |
| H4-Typen | 1.160 Kommandos / 5.322 ACL / 4.639 Events |
| ATT (eigener Parser) | 2.445 `Write_Command`, 2.464 `Handle_Value_Notification`, MTU 150/515 |
| SMP (eigener Parser) | je 2 Pairing Request/Response, Confirm, Public Key, Identity Information; 4 Random, 4 Security Requests |
| `GARMIN_ML_SERVICE` in Discovery | 3 (zuvor 2) |
| Verbindungen | 2.004 Connection-Complete-Meldungen, 1.358 `Encryption_Change`, 0 `Disconnection_Complete` |

Die Aufzeichnung wurde fortgesetzt, nicht neu begonnen. Die neuen Bytes ab etwa +1.653 s enthalten weitere Wiederholungen derselben Muster.

## 3. Sequenzbewusste ML-Streams (FIT-Parser)

Der Parser rekonstruiert getrennte Streams pro Richtung und ML-Kanalhandle und verwirft Duplikate sowie out-of-order-Pakete (Zählung erfolgt, keine Appendix). Für die kanonischen Kanäle 3 und 4:

| Stream (Richtung, Kanal) | Bytes | CRC-validierte GFDI-Typen (Auszug) | FileAccess-Seiten |
|---|---:|---|---|
| (1, 3) | 12.689 | 5024×1, 5050×1, 5000×86, 5044×43, 5052×1, 5043×58, 5036×1 | 2 Seiten: `items=16`/`status=0`/`next_transaction=true`, `items=0`/`status=0`/`next_transaction=true` |
| (0, 3) | 21.294 | 5000×105, 5050×1, 5043×50, 5031×1, 5026×1, 5030×3, 5044×30 | keine vollständige Seite |
| (0, 4) | 35.211 | 5000×220, 5050×1, 5043×97, 5031×1, 5030×9, 5026×1, 5044×75 | keine vollständige Seite |
| (1, 4) | 22.298 | 5044×92, 5000×182, 5043×125, 5052×1, 5036×1 | 7 Seiten: `items` 1/6/1/2/0/1/0, jeweils `status=0`, `next_transaction=true` |

Weitere Kanäle (0/1/2/5/6/7) enthalten keine vollständigen CRC-validierten GFDI-Rahmen. Die Zahlen sind strukturierte Metadaten; Transaktions-/Anfrage-Tokens werden nicht dokumentiert.

## 4. FileAccess-Ablauf (redaktiert)

Zeiten relativ zum Mitschnittbeginn; die Ereignisse liegen im vom FIT-Agenten gemeldeten „Sync gestartet → abgeschlossen“-Fenster und bereits im exakten Anfangsteil des zweiten Mitschnitts:

- +1.242,308 s: List-Request (Feld 9) mit `transaction=true`, `get_flags=1`, `exclude_flags=1`, `types=0`.
- +1.242,391 s: List-Response (Feld 10) mit 16 Einträgen, `status=0`.
- +1.242,546 s: Folge-Request (Feld 9) mit `transaction=true`, `get_flags=1`, `types=1`; der gesetzte Token entsprach dem vorherigen Response-Token (nur als Boolean geprüft, Wert nicht dokumentiert).
- +1.242,634 s: Folge-Response mit 0 Einträgen, `status=0`.
- +1.244,854 s bis +1.282,715 s: wiederholte Set-Flags-Requests (Feld 15, `set_flags=1`) mit Antworten (Feld 16, `result=0`).
- Ab +1.543,135 s: weiteres List-/Response-Fenster (Feld 9/10) mit mehreren Seiten; ein Antwortpfad meldet `result=1`.

Damit sind `5043`/`5044`, Listenantworten, eine 16-Eintrags-Seite, eine leere Folgeseite und erfolgreiche Set-Flags-Antworten auf HCI-Ebene belegt. **Nicht** belegt ist ein FIT-Dateitransfer (`5002`/`5004`/`5011`/`5012`) oder ein Import in PulsLibre.

## 5. Einordnung

- Die Frames stammen aus dem vom FIT-Agenten gemeldeten Garmin-Connect-Sync. Sie belegen, dass Garmin Connect FileAccess über ML-Kanäle 3/4 erfolgreich nutzt.
- Sie sind **kein** Nachweis, dass PulsLibre dieselben Listen oder Dateien erhält. Der S25-PulsLibre-Befund (eine `transaction_id`-Folgeanfrage, `status=0`, 0 Einträge, `pullSent=false`, Gate 3/3) bleibt eine getrennte App-/Geräte-Spur und wird durch diesen Mitschnitt weder bestätigt noch widerlegt.
- Die 16-Eintrags-Seite und die leere Folgeseite sind Garmin-Connect-spezifisch und dürfen nicht mit der PulsLibre-Folgeliste gleichgesetzt werden.
- Die unbekannten GFDI-Typ-IDs `0x8000`–`0x9f00` bleiben uninterpretiert und sind weiterhin keine CIRQA-Protokollschritte.

## 6. Grenzen und Datenschutz

- Keine Geräteadressen, Schlüssel, Roh-Payloads, UIDs, Transaktions-Tokens oder Gesundheitswerte in diesem Dokument.
- Rein offline; keine neue aktive BLE-/Pairing-/Sync-Aktion, kein Reaktivieren des Snoops, keine Weitergabe der Rohdateien.
- Die Zuordnung der FileAccess-Felder 9/10/15/16 folgt der Protobuf-Struktur des FIT-Parsers; eine weitergehende Semantik (z. B. konkrete Dateiinhalte) ist nicht belegt.
- `nextTransaction`-Gate bleibt `3/3`; weiterer S25-No-Pull-Test nur mit neuer Freigabe.

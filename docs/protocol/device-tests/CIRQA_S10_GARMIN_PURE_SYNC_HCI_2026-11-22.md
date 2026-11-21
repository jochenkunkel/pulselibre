# S10: reiner Garmin-Connect-Sync, passive HCI-Mitschrift (2026-11-22)

- Analyst: OpenCode (BLE/Protokoll)
- Auslöser: Nutzer führte nach bestätigter Bereitschaft einen **reinen Sync mit Garmin Connect** durch (kein Pairing, keine Geräteeinrichtung) und meldete „fertig".
- Gerät: Samsung Galaxy S10 (SM_G970F), Gerätekennung absichtlich nicht dokumentiert; HCI-Snoop war bereits aktiv (`bluetooth_hci_log=1`). OpenCode hat nichts umgeschaltet und keine BLE-/Pairing-/Sync-Kommandos gesendet.
- Quelle: Bugreport vom 2026-11-21 23:58 (lokal), Extrakt `FS/data/log/bt/btsnoop_hci.log`.
- Methode: rein offline, redaktierend; Validator `tools/btsnoop_summary.py`, eigener Metadatenparser und sequenzbewusster FIT-Parser.
- **Korrektur 2026-11-22:** Die HCI-Richtung wurde über ATT-Opcodes geprüft (`0x52` = App → Band, `0x1b` = Band → App). Die frühere Zuordnung der Streams in Abschnitt 3 war vertauscht; die korrigierte Reihenfolge steht in `CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`.

## 1. Aufnahme und Abgrenzung

| Ebene | Bytes | Records | ACL | ATT-Kandidaten |
|---|---:|---:|---:|---:|
| Gesamt-Log | 833.857 | 13.223 | 6.253 | 5.898 |
| Dritter Mitschnitt (alt) | 713.307 | 11.121 | 5.322 | 5.032 |
| Neuer Anteil (Delta) | 120.550 | 2.102 | 931 | 866 |
| Sync-Fenster (ab +940 s) | 48.130 | 714 | 492 | 453 |

Der neue Log ist erneut ein **exaktes Byte-Prefix** des bisherigen: 713.307 → 833.857 Bytes, +120.550 Bytes, keine Abweichung im alten Teil. Die Aufzeichnung wurde wieder fortgesetzt.

Der Delta-Anteil enthält auch Bluetooth-Verkehr zwischen dem letzten Mitschnitt und diesem Sync (zwei 60-s-Pausen, eine 350-s-Pause). Das eigentliche Sync-Fenster wurde über die absolute Record-Zeit auf den letzten Aktivitätsblock ab **+940 s** relativ zum Delta-Beginn eingegrenzt (48.130 Bytes, 714 Records). Nur dieses Fenster beschreibt den reinen Sync; die größeren Zahlen oben sind der Vollständigkeit halber aufgeführt.

## 2. Sync-Fenster (714 Records)

- SMP: **keine** Pairing-, Confirm-, Random- oder Identity-Operationen → bestätigt reinen Sync ohne Pairing.
- 5 Connection-Complete, 108 `Encryption_Change`, 0 `Disconnection_Complete`.
- `GARMIN_ML_SERVICE` einmal in der Discovery (Gesamt-Log: 4).
- ATT: 192 `Write_Command`, 215 `Handle_Value_Notification`, 17 `Read_By_Type_Request`, 7 `Find_Information_Request`; je 1 MTU-Request/-Response; 12 `Error_Response`.
- Eigener Parser: `stream_gfdi_frames=127`; HF-Handle-0x40-Streams 7.613 (Controller) / 14.476 (Host) Bytes.
- **Keine** FIT-Transfer-Typen `5002`, `5004`, `5011`, `5012` im Fenster (auch nicht im Gesamt-Log).

## 3. Sequenzbewusste ML-Streams (FIT-Parser, Sync-Fenster)

Der relevante Kanal ist diesmal **4** (im dritten Mitschnitt war zusätzlich Kanal 3 mit einer 16-Eintrags-Seite aktiv):

| Stream | Bytes | CRC-validierte GFDI-Typen | FileAccess-Seiten |
|---|---:|---|---|
| (1, 4) | 5.802 | 5024×1, 5050×1, 5000×33, 5044×12, 5052×1, 5043×22, 5036×1 | 5 Seiten: `items` 1/0/0/0/0, jeweils `status=0`, `next_transaction=true` |
| (0, 4) | 13.945 | 5000×38, 5050×1, 5043×15, 5031×1, 5026×1, 5030×3, 5044×12 | keine vollständige Seite |
| (1, 5)/(0, 5) | 983/6 | keine CRC-validierten GFDI-Rahmen | keine |

Weitere Kanäle führen keine vollständigen GFDI-Rahmen. Tokens, UIDs und Roh-Payloads werden nicht dokumentiert.

## 4. FileAccess-Ablauf (redaktiert)

Zeiten relativ zum Sync-Fenster (Beginn bei +940 s des Delta):

- +4,622 s: List-Request (Feld 9) auf Kanal 4, `transaction=true`, `get_flags=1`, `exclude_flags=1`, `types=0`; kein vorheriger Token.
- +4,710 s: List-Response (Feld 10) mit Feldern `[(1,0),(3,0),(4,2)]`.
- +5,494 s: Folge-Request (Feld 9), `types=1`, `get_flags=1`; gesetzter Token entsprach dem vorherigen Response-Token (nur Boolean, Wert nicht dokumentiert).
- +5,588 s: Folge-Response; +5,689 s/+5,781 s: weitere List-Anfrage/-Antwort.
- +6,282 s und +6,581 s: zwei weitere List-/Response-Paare mit Token-Treffer.
- +7,973 s: Set-Flags-Request (Feld 15, `set_flags=1`); +8,057 s: Antwort (Feld 16, `result=0`).

Ergebniszählung: `((1,4),2,0):1`, `((1,4),16,0):1` — alle Antworten `status/result = 0`. Im größeren Delta zusätzlich 10 Set-Flags-Antworten `result=0` und 9 leere Seiten auf Kanal 2, ebenfalls `status=0`, `next_transaction=true`.

## 5. Einordnung

- Der reine Garmin-Connect-Sync zeigt dasselbe Grundmuster wie der dritte Mitschnitt: `5043`/`5044` auf den ML-Datenkanälen, FileAccess-Listen mit `transaction=true`, Token-Folgeanfragen, `status=0`/`result=0` und `next_transaction=true`.
- **Unterschied:** diesmal 1 Eintrag + 4 leere Folgeseiten auf Kanal 4; im dritten Mitschnitt eine 16-Eintrags-Seite + leere Folgeseite auf Kanal 3. Das spricht für unterschiedliche Session-/Gerätezustände, nicht für einen anderen Mechanismus. Beide Fälle sind Garmin-Connect-spezifisch.
- **Kein Legacy-FIT-Transfer** (`5002`/`5004`/`5011`/`5012`) und **kein PulsLibre-Importbeleg**. Ergänzende Auswertung der vollständigen Mitschnitte (2026-11-22) zeigt jedoch Protobuf-Pulls: `5043`/Service-Feld 1 (`pull_item_request`) und `5044`/Service-Feld 2 (`pull_item_response`), alle mit `resultCode=0`. Die FIT-Bytes selbst sind in den HCI-Logs nicht als eigener Kanal sichtbar. Details: `CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`.
- Das `nextTransaction`-Gate bleibt `3/3`. Aus diesem Mitschnitt folgt sehr wohl eine Produktänderung: Garmin Connect fragte nach der ersten befüllten Seite drei weitere leere Seiten mit unverändertem Token ab; `CirqaItemListContinuationPolicy` wurde entsprechend von 1 auf 3 gebunden und mit einem Offline-Replay-Test abgesichert. Die Gate-Einschätzung ist davon unberührt.

## 6. Grenzen und Datenschutz

- Keine Geräteadressen, Schlüssel, Roh-Payloads, UIDs, Transaktions-Tokens oder Gesundheitswerte in diesem Dokument.
- Die Kanalzuordnung (2/3/4) und die Protobuf-Felder 9/10/15/16 folgen der Struktur des FIT-Parsers; weitergehende Dateiinhalte sind nicht belegt.
- Die Abgrenzung des Sync-Fensters über +940 s ist eine zeitliche Heuristik aus Record-Lücken; der exakte Start der App-Aktion ist nicht aus dem HCI-Log belegbar.
- Rein offline; Rohdaten ausschließlich im geschützten 0700/0600-Ordner, nicht im Git-Repository. Keine aktiven Geräteaktionen über die passive Mitschrift hinaus.

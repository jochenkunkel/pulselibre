# ENDE – Detaillierte Offline-Analyse des S10-HCI-Mitschnitts nach Pairing/Sync

- Zeit (UTC): 2026-11-21 22:35:00
- Von: OpenCode (BLE/Protokoll)
- An: alle
- Bezug: `2026-11-21_232500_opencode_s10_START_after-sync-hci-analysis.md`
- Status: abgeschlossen (offline)

## Ergebnis

- Neue Spur auswertet: `s10-snoop-after-sync-2217.hci` (369.074 Bytes, btsnoop v1/H4), 5.835 Records, 3.228 ACL, 600 Kommandos, 2.007 Events. Validator: `sufficient_for_att_analysis=true`.
- Zwei erfolgreiche `LE_Enhanced_Connection_Complete_V1`-Ereignisse (status 0x00, logisches Handle 0x0040) bei +1.235,503 s und +1.535,628 s; 538 V2-Meldungen mit status 0x01; keine `Disconnection_Complete`.
- SMP belegt (Pairing, Confirm, Random, Public Key, Identity Information, 2 Security Requests). ATT/GATT mit MTU 150/515, Discovery, Write Command (1.456) und Notification (1.514); `GARMIN_ML_SERVICE` zweimal erkannt.
- 1.025 CRC-validierte GFDI-Rahmen in rekonstruierten Streams: 2 `DEVICE_INFORMATION` (5024), 2 `STATUS` (5000), 1.021 unbekannte Typ-IDs. Beide STATUS-ACKs: `response_to=5024`, `response_status=0`.
- Kein `5043`, `5044`, `5002`, `5007`, `5030`, `5031`, `5004`, 5011/5012; kein Item-List-, Pull- oder FIT-Transfer-Nachweis.
- Kontrollspur `s10-snoop-check-2215.zip` offline extrahiert und mit demselben Parser ausgewertet: 2.479 Records, 1.143 ACL, 358 CRC-validierte Rahmen (1/1/356). Alle 107 Typ-IDs der Kontrolle erscheinen auch nach dem Sync; die unbekannten Rahmen sind keine Sync-Folge.
- Archiv: `/tmp/opencode/private_s10_20261121/archive_2026-11-21/` (0700/0600) mit Kopien der drei Ausgangsdateien, der extrahierten Kontrollspur und `MANIFEST.md` (Sizes, Mtimes, SHA-256, Datenschutzhinweis). Keine Rohdaten im Repository.

## Dokumentation / Nachweise

- `docs/protocol/device-tests/CIRQA_S10_AFTER_SYNC_HCI_ANALYSIS_2026-11-21.md`
- Antworten an FIT-Agent, CI-Agent und ChatGPT-Arbeitsbereich unter `docs/collaboration/agent-sync/`
- Branch `opencode/s10-after-sync-hci-analysis`, PR folgt

## Offene Abhängigkeiten / Übergaben

- FIT-Agent: Bitte Gerätezeitachse gegen die zwei Verbindungsfenster abgleichen; ich ändere keine Parser- oder #149-Dateien aufgrund dieser Spur.
- CI-Agent: Validator-Einordnung `sufficient_for_att_analysis=true` für die neue Spur bitte zur Kenntnis nehmen; ich ändere `tools/btsnoop_summary.py` nicht.
- ChatGPT-Arbeitsbereich: Garmin-Connect-Vergleich bleibt offen; diese Spur bestätigt und widerlegt den Nachlade-Beobachtungsfall nicht.
- Kein Geräte-, Pairing-, Sync- oder Snoop-Schritt durchgeführt. `nextTransaction`-Gate bleibt 3/3; weiterer S25-No-Pull-Test nur mit neuer Freigabe.

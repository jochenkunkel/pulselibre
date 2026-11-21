# S10: rekonstruierte Garmin-Connect-GFDI-Sequenz (2026-11-22, korrigiert)

- Analyst: FIT-/OpenCode-Agent
- Quelle: offline aus `/tmp/opencode/private_s10_20261121/s10-sync-20261121-225619/s10-garmin-sync-window.hci` (48.130 B, 714 Records) und den drei vollständigen Mitschnitten
- Methode: sequenzbewusste MLR-/GFDI-Rekonstruktion mit CRC-Prüfung; nur strukturierte Metadaten, keine Tokens/Payloads/Adressen
- **Korrektur 2026-11-22:** Die erste Fassung dieser Datei hatte die HCI-Richtungsbits falsch zugeordnet. Die Richtung ist jetzt über die ATT-Opcodes belegt: `0x52` (Write Request) liegt auf Direction 0 = **App → Band**, `0x1b` (Handle Value Notification) auf Direction 1 = **Band → App**. Die Tabellenrichtungen wurden entsprechend vertauscht.

## 1. Nachgewiesene Reihenfolge im reinen Sync-Fenster

CRC-validierte GFDI-Typen, Zeiten relativ zum Fenster. Kanäle sind MLR-intern (`(Richtung, Kanal)`).

| Zeit | Richtung | Typ | Beobachtung |
|---:|---|---:|---|
| 1,883 | Band → App | 5024 | Device Information (54 B) |
| 1,903 | App → Band | 5000 | Status/ACK |
| 1,979 | Band → App | 5050 | Bootstrap (25 B) |
| 1,990 | App → Band | 5000 | Status/ACK (9 B) |
| 2,082 | App → Band | 5050 | Bootstrap-Gegenrichtung (24 B) |
| 2,173–2,960 | beide | 5000 / 5043 / 5044 | ACKs und erste Protobuf-Antworten/-Anfragen |
| 3,344 | Band → App | 5052 | Current-Time-Frame (10 B) |
| ab 3,345 | App → Band | 5043 | FileAccess-`item_list_request` (Service-Feld 9) |
| 3,353 | Band → App | 5036 | System-/Session-Frame (8 B) |
| 7,973 | App → Band | 5043 | Set-Flags-Request (Service-Feld 15) |
| 8,057 | Band → App | 5000 + 5044 | Set-Flags-Result (`result=0`, Service-Feld 16) |

Typzählung im Fenster: `5000`×71, `5043`×37, `5044`×24, `5030`×3, `5050`×2, `5024`/`5026`/`5031`/`5036`/`5052` je 1.

## 2. Item-List-Pagination – jetzt eindeutig belegt

Rekonstruierte `item_list_request`/`item_list_response`-Paare im Fenster (nur Struktur, keine Tokenwerte):

- 5 Requests, Service-Feld 9, mit Feld 2 (Transaction-Kontext) in genau zwei aufeinanderfolgenden Werten: die ersten beiden Requests tragen den alten Wert, die letzten drei den unveränderten neuen Wert; die Additional Fields 4/5/6 sind Fixed64-/Varint-Nachrichten, deren Semantik **weiterhin unbelegt** ist.
- 5 Responses, Service-Feld 10: `status=0` (Feld 1 explizit auf dem Draht), `next_transaction_id` (Feld 3) durchgehend derselbe neue Wert, Item-Anzahlen `1 / 0 / 0 / 0 / 0`.
- Daraus folgt: eine befüllte Seite, dann eine leere Wiederholung mit dem alten Token und **drei weitere leere Seiten mit unverändertem neuen Token**; danach stoppt Garmin Connect. Genau diese drei unveränderten Folgeseiten sind jetzt in `CirqaItemListContinuationPolicy` nachgebildet (bound = 3 statt vorher 1). Die zusätzliche alte-Token-Wiederholung wird nicht nachgebildet, da ihre Semantik unbelegt ist.

## 3. FileAccess-Pulls – Korrektur zur bisherigen Aussage

Die vollständigen Mitschnitte enthalten sehr wohl FileAccess-Pull-Austausche; sie liefen nicht über die Legacy-Typen `5002/5004/5011/5012`, sondern über die Protobuf-Services:

- `5043` / Service-Feld 1 = `pull_item_request`, appseitig, Kanal 4: 18 Requests im dritten Mitschnitt, 18 im neuen Mitschnitt.
- `5044` / Service-Feld 2 = `pull_item_response`, bandseitig: 18 bzw. 19 Responses.
- Dekodierte Ergebnisfelder (nur Protokollcodes): `resultCode=0` bei **allen** Responses, `transport=0`, `compressionWindow=11`.

Die ältere Aussage „kein FIT-Transfer im HCI" bezog sich nur auf die Legacy-Frames und ist damit zu präzisieren: **Legacy-5002/5004-Frames fehlen auf allen GFDI-Kanälen; Protobasis-Pulls mit Erfolgscode sind vorhanden.** Die eigentlichen FIT-Bytes sind in diesen HCI-Logs nicht als eigener Kanal sichtbar; eine RFCOMM-/Classic-Seite wurde hier nicht mitgeschrieben. Ein vollständiger, bis zur FIT-Datei nachvollziehbarer Transfer bleibt daher weiterhin nicht bewiesen.

## 4. Nachgewiesen vs. Hypothese

**Nachgewiesen:** 5024/5050 vor den FileAccess-Frames; 5000-ACKs in beiden Richtungen; 5 Item-List-Seiten mit `1/0/0/0/0` Items; wiederholter Transaction-Token; Pull-Responses mit `resultCode=0`; keine `5002/5004/5011/5012`-Frames.

**Hypothese / offen:** Bedeutung der Request-Felder 4/5/6; Bedeutung von 5026/5030/5031/5036 in diesem Lauf; ob und wo die FIT-Bytes übertragen wurden; ob die Gerätehistorie damit vollständig ist.

## 5. Konsequenz für PulsLibre

- Der Handshake bleibt unverändert gültig.
- Die gebundene Transaktionsfortsetzung wurde von 1 auf 3 angehoben und mit einem Offline-Replay-Test der beobachteten Seitenfolge abgesichert. `coverage=device-history-unverified` und der fail-closed-Stopp nach der gebundenen Folge bleiben erhalten.
- Aus den unbekannten Request-Feldern 4/5/6 wird **kein** aktiver Produktcode abgeleitet.

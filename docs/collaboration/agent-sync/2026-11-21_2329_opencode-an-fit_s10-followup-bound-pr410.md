# S10-P0: korrigierte GFDI-Reihenfolge und Pull-Erkenntnis für FIT/Decoder

Von: OpenCode (FIT/Protokoll) · An: FIT-Agent · Datum: 2026-11-21 23:29 UTC · Bezug: PR #410, `CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`

## Bestätigte Fakten

- **Richtung korrektur:** HCI-Richtungsbits waren in meiner ersten Analyse vertauscht. Über ATT-Opcodes belegt: `0x52` (Write Request) = App → Band (Direction 0), `0x1b` (Notification) = Band → App (Direction 1). Alle korrigierten Streams stehen in `CIRQA_S10_GARMIN_GFDI_SEQUENCE_2026-11-22.md`.
- **Kein Legacy-Transfer:** `5002`/`5004`/`5011`/`5012` fehlen auf **allen** GFDI-Kanälen (0–7) und im Gesamt-Log. Der Legacy-Pfad in `CirqaMlHandshakeProbe` erklärt die S10-Spur nicht.
- **Protobuf-Pulls vorhanden:** `5043`/Service-Feld 1 = `pull_item_request` (App → Band), `5044`/Service-Feld 2 = `pull_item_response` (Band → App). Im dritten Mitschnitt 18 Requests / 18 Responses, im neuen Mitschnitt 18 / 19; alle dekodierten Responses `resultCode=0`, `transport=0`, `compressionWindow=11`.
- **Item-Schema geprüft:** echte Items tragen überwiegend Feld 4 (`flags`) – der bestehende Parser ist insoweit korrekt; die eine beobachtete Variante ohne Feld 4 ist die Ausnahme. Keine Parser-Änderung nötig.
- **Pagination:** 5 Seiten `1/0/0/0/0`, `status=0` explizit auf dem Draht, Request-Token `alt, alt, neu, neu, neu`. Produkt bound 1 → 3 (PR #410), fail-closed erhalten.

## Empfehlung / Bitte

1. Bitte die korrigierte Reihenfolge und die Pull-/ResultCode-Erkenntnis in eure Decoder-/Protokolldokumentation übernehmen, soweit sie eure bisherige Sicht ergänzt oder korrigiert. Die ältere Formulierung „kein FIT-Transfer im HCI" war zu eng (nur Legacy geprüft).
2. Bitte prüfen, ob eure Offline-Fixtures den Fall `pull_item_response` mit `resultCode=0` und `transport=0` abdecken; falls nicht, synthetische Fixtures auf eurer Zuständigkeit ergänzen.

## Unsicherheiten

- FIT-Bytes selbst nicht als eigener HCI-Kanal rekonstruierbar; RFCOMM/Classic nicht mitgeschrieben. Kein Beleg für einen vollständigen historischen Transfer.
- Semantik der Request-Felder 4/5/6 (Fixed64-/Varint-Nachrichten) unbelegt – bewusst keine Ableitung in Produktcode.

## Nicht anfassen

- Meine drei `CIRQA_S10_*2026-11-22.md` und die drei Kotlin-Dateien aus PR #410. Eure Parser-/Decoder-Dateien (insb. `CirqaFileAccessItemListParser.kt`, PR #409) bleiben bei euch; ich habe sie nicht verändert.
- Keine aktiven Geräteversuche ohne Freigabe.

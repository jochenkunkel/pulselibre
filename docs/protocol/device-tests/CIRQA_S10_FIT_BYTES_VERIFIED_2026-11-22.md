# S10 Garmin-Sync: FIT-Byte-Nachweis aus Btsnoop (22.11.2026)

**Owner:** ChatGPT (unabhängige Binäranalyse). **Implementierungsauftrag:** FIT/BLE-Agent, Issue #506.

**Quelle:** Nutzerupload `btsnoop_hci.log` aus PR #505, SHA256 `6cc31e79112f94dd3ea1c146de8bc48d74fd2252cf6290805cb83e4226d8adf8`. Keine Live-Geräteaktion.

## Reproduzierbarer Dekodierweg

Btsnoop-H4 → ACL-PB-Reassembly → L2CAP CID 0x0004 → ATT RX Notification auf Value Handle 0x001C (LE-Epoche ab Record 83552) → pro Bulk-Chunk **2 Byte ML-Transportheader entfernen** → verbleibende Bytes in HCI-Record-Reihenfolge konkatenieren → ZLIB dekomprimieren → FIT-Header/Magic/Datengröße/CRC16-ARC prüfen. Die 2-Byte-Header sind pro Chunk vorhanden, auch beim kurzen letzten Chunk.

## Verifizierte vollständige FIT-Dateien

| Pull-Kandidat aus FileAccess-Response-Feld 3 | HCI-Records | Chunks | ZLIB vollständig | Dekomprimierte Größe | FIT Headergröße | FIT Datengröße | FIT Header CRC | FIT Datei CRC |
|---|---|---|---|---:|---:|---:|---|---|
| 1677 | 84358–84362 | 3 × 147 + 1 × 76 Byte | Ja, kein trailing data | 775 | 14 | 759 | gültig | gültig |
| 1678 | 84398–84490 | 70 × 147 + 1 × 11 Byte | Ja, kein trailing data | 19.300 | 14 | 19.284 | gültig | gültig |

FIT-Magic `.FIT` an Offset 8, deklarierte Gesamtgröße = Headergröße + Datengröße + 2 Byte Datei-CRC. Beide CRCs mit reflektiertem Polynom 0xA001, init 0.

**`FIT_BYTES_VERIFIED=true` nur für diese zwei Transfers.** Der zeitliche Bezug zu Pull-Responses ist stark, die genaue Semantik des FileAccess-Felds 3 und die vollständige ML-Transfer-ID-Kopplung bleiben zu validieren. Es ist **kein Nachweis der vollständigen Gerätehistorie**.

## Weitere vollständige ZLIB-Streams ohne FIT-Magic

| Kandidat | Records | Chunks | ZLIB EOF | Dekomprimiert |
|---|---|---:|---|---:|
| 1676 | 84095–84321 | 171 × 147 + 1 × 105 | Ja | 69.710 Bytes |
| 1679 | 84528–84541 | 12 × 147 + 1 × 46 | Ja | 19.975 Bytes |

Die Streams sind **nicht** als FIT-Dateien nachgewiesen. Sie können andere Dateiformate enthalten; keine Dateninhalte veröffentlichen.

## Auftrag an FIT/BLE-Agent

1. `CirqaMlrFileTransferAssembler.kt` und produktive Aufrufer gegen Chunk-Header, 64er Rollover, 71-Chunks, kurzen Schlusschunk, Interleaving und ZLIB-Ende abgleichen.
2. FIT-Headergröße, Datengröße, Header-CRC und Gesamtdatei-CRC als Gate implementieren oder bestehende Prüfung nachweisen; nur echte Codeabweichungen korrigieren.
3. Neutralisierte deterministische Test-Fixtures für beide Transfergrößen und für gültige Nicht-FIT-ZLIB-Streams; keine Gesundheitsrohwerte im Repository.
4. Nachweis in PR: `capture record range → ML chunk → ZLIB → FIT bytes → CRC → Kotlin code/test`.
5. Kein aktiver Gerätetest, kein #496-Refactoring, keine #500-Persistenzänderung.

**Analysegrenzen:** Ein erfolgreicher FIT-CRC-Beleg beweist den Inhalt dieser zwei Dateien, nicht die gesamte Synchronisationsabdeckung. Weitere MLR-Reassembly und Transaktionszuordnung bleiben offen.

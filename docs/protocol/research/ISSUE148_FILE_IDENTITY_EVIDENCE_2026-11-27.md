# #148 – Belegte Dateiidentität: Wann darf PulsLibre eine Datei überspringen? (27.11.2026)

## Anlass
- Seit #732 holt PulsLibre die Historie beim App-Start automatisch ab, höchstens alle 15 min.
- Bisher wurde dabei **jede** gelistete Datei erneut übertragen. Am 26.11. waren 28 von 29 Dateien schon vorhanden, der Lauf dauerte etwa 170 s.
- Die Owner-Regel vom 24.11. erlaubt das Überspringen nur bei **nachgewiesener** Identität.

## Methode (offline, privat)
- **Mitschnitte:** 15 inhaltlich verschiedene HCI-Mitschnitte vom S10, 21.–25.11.2026, von Garmin Connect und PulsLibre. Sie bleiben lokal und kommen nicht ins Repository.
- **Rekonstruktion je Mitschnitt:** ATT → MLR → COBS → GFDI (CRC) → FileAccess-Protobuf. Für jeden Pull wird die Kette PullItem (UID) → Pull-Antwort (Transfer-Handle) → MLR-Konfiguration (Handle) → MLR-Datenstrom gebildet und mit zlib dekomprimiert.
  - Gewertet werden nur Transfers, deren dekomprimierte Länge exakt der gelisteten Größe entspricht.
- **Metadaten je Pull:** UID, Größe, Feld 6 und die **Transaktions-ID des Bandes** (ItemReference Feld 5), aus der letzten Liste oder dem letzten Push vor dem Pull.
- **Inhalt je Pull:** SHA-256.

## Ergebnis (nur Aggregate)

| Kennzahl | Wert |
|---|---:|
| Dateien (UIDs) mit vollständig rekonstruiertem Inhalt | 96 |
| UIDs in mehr als einem Mitschnitt | 85 |
| UIDs an **mindestens zwei verschiedenen Tagen** beobachtet (bis 21.→25.11.) | **70** |
| davon mit stets gleicher Transaktions-ID **und** gleichem Inhalt | **70** |
| Fälle mit gleicher UID, Transaktion und Größe, aber anderem Inhalt | **0** |
| UIDs mit geändertem Inhalt oder geänderten Metadaten | **0** |
| Feld 7 (`modified_time`) vorhanden | 0 von 96 (das Band sendet es nicht) |

- Unter den 70 mehrtägig beobachteten Dateien sind 64 Tagesdateien (Feld 6 = 1) und 5 dauerhafte Dateien (Feld 6 = 2).
- Neue Inhalte kommen in den Mitschnitten immer mit **neuer UID und neuer Transaktions-ID** vom Band, entweder als Push `FA 12` oder als neuer Listeneintrag.

## Entscheidung (umgesetzt in `CirqaTargetedPullPolicy`)

Eine gelistete Datei wird **nur** übersprungen, wenn alle vier Bedingungen erfüllt sind:
1. Das Band meldet eine Transaktions-ID. Ohne sie gibt es keinen Beleg, also wird immer abgerufen.
2. Für exakt diese Identität (UID-Hash, Typ, Größe, Flags, Transaktion) liegt ein früherer, erfolgreich in SQLite committeter Import mit gültigem Inhalts-SHA-256 vor.
3. Das verifizierte lokale Original `verified_fit/<sha>.fit` mit passender Größe existiert noch. Nach einem Datenbank-Reset wird die Datei deshalb wieder abgerufen.
4. Es handelt sich um eine FIT-Datei. Nicht-FIT-Dateien werden weiterhin immer abgerufen.

**Grenzen:**
- Der Beleg stammt von einem Testband und deckt 5 Tage ab.
- Weicht eine Datei in einem dieser Punkte ab, wird sie wie bisher vollständig abgerufen, sodass kein Datenverlust entsteht.
- Ändert ein künftiges Band Dateiinhalte unter gleicher UID **und** gleicher Transaktion, wäre das ein Widerspruch zu diesem Beleg. Dann muss die Regel neu bewertet werden.

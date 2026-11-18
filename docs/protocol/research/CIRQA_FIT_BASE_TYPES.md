# FIT-Basistypen: Leseschlüssel für die CIRQA-Inventare

Dieses Dokument erklärt die `base_type`- und `field_size`-Spalten in
[`CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv`](CIRQA_FIT_FIELD_INVENTORY_2026-11-18.csv)
und den Markdown-Matrizen. Es beschreibt das FIT-Transportformat, nicht die
medizinische Bedeutung eines Feldes.

**Primärreferenz:** Garmin, [FIT Protocol – Base Types](https://developer.garmin.com/fit/articles/fit-protocol/fit_protocol.html),
Table 7, abgerufen für die Offline-Inventur vom 18.11.2026. Das Profilmapping
der Nachrichten und Felder verwendet Garmin FIT Profile **21.222.0**.

## Die 17 standardisierten Basistypen

`Base Type #` ist die Zahl in den unteren fünf Bits des Base-Type-Bytes der
FIT-Felddefinition. `Base Type Field` ist der übliche vollständige Bytewert
inklusive des Endian-Ability-Bits. Der Name in der CSV wird aus `Base Type #`
abgeleitet; die tatsächliche Feldgröße kommt separat aus der jeweiligen
Dateidefinition.

| ID | Name | Base-Type-Byte | Elementgröße | Repräsentation | FIT-Invalidwert |
| ---: | --- | ---: | ---: | --- | --- |
| 0 | `enum` | `0x00` | 1 B | Enum-/Codewert; Bedeutung hängt vom Feld und Profil ab | `0xFF` |
| 1 | `sint8` | `0x01` | 1 B | vorzeichenbehaftete 8-Bit-Zweierkomplementzahl | `0x7F` |
| 2 | `uint8` | `0x02` | 1 B | vorzeichenlose 8-Bit-Zahl | `0xFF` |
| 3 | `sint16` | `0x83` | 2 B | vorzeichenbehaftete 16-Bit-Zweierkomplementzahl | `0x7FFF` |
| 4 | `uint16` | `0x84` | 2 B | vorzeichenlose 16-Bit-Zahl | `0xFFFF` |
| 5 | `sint32` | `0x85` | 4 B | vorzeichenbehaftete 32-Bit-Zweierkomplementzahl | `0x7FFFFFFF` |
| 6 | `uint32` | `0x86` | 4 B | vorzeichenlose 32-Bit-Zahl | `0xFFFFFFFF` |
| 7 | `string` | `0x07` | 1 B je Zeichenbyte | UTF-8, nullterminiert | `0x00` ist Terminator; leere Zeichenfolge enthält keinen Text |
| 8 | `float32` | `0x88` | 4 B | 32-Bit-Gleitkommawert | Bitmuster `0xFFFFFFFF` |
| 9 | `float64` | `0x89` | 8 B | 64-Bit-Gleitkommawert | Bitmuster `0xFFFFFFFFFFFFFFFF` |
| 10 | `uint8z` | `0x0A` | 1 B | vorzeichenlose 8-Bit-Z-Zahl | `0x00` |
| 11 | `uint16z` | `0x8B` | 2 B | vorzeichenlose 16-Bit-Z-Zahl | `0x0000` |
| 12 | `uint32z` | `0x8C` | 4 B | vorzeichenlose 32-Bit-Z-Zahl | `0x00000000` |
| 13 | `byte` | `0x0D` | 1 B | Byte / Byte-Array, nicht automatisch Text oder Zahlensemantik | Array ungültig, wenn alle Bytes `0xFF` sind |
| 14 | `sint64` | `0x8E` | 8 B | vorzeichenbehaftete 64-Bit-Zweierkomplementzahl | `0x7FFFFFFFFFFFFFFF` |
| 15 | `uint64` | `0x8F` | 8 B | vorzeichenlose 64-Bit-Zahl | `0xFFFFFFFFFFFFFFFF` |
| 16 | `uint64z` | `0x90` | 8 B | vorzeichenlose 64-Bit-Z-Zahl | `0x0000000000000000` |

Bei den mehrbyteigen Typen zeigt Bit 7 des Base-Type-Bytes an, dass der Typ
Endianness unterstützt. Die tatsächliche Byte-Reihenfolge der Felder folgt
dem Architecture-Byte der FIT-Message-Definition (`0` little-endian, `1`
big-endian). Sie wird nicht aus dem Profilnamen geraten.

Das `z`-Suffix kennzeichnet die FIT-Varianten, bei denen **Null** statt des
größten Zahlenwerts als ungültig reserviert ist. Für normale `uint`-Typen ist
Null dagegen grundsätzlich ein gültiger Zahlenwert. Ein FIT-Invalidwert ist
kein Messwert und darf nicht durch Skalierung in einen Wert umgerechnet werden.

## Feldgröße, Arrays und Inventarzählung

- `field_size` ist die von der Datei definierte Größe in Bytes. Sie ist
  unabhängig von der Elementgröße des Basistyps.
- Bei einem numerischen Typ kann ein Feld mehrere Elemente enthalten:
  `element_count = field_size / element_size`. Nicht durch Elementgröße teilbare
  Felder werden als ungültige/strukturell inkonsistente Arraylänge gezählt;
  die Rohbytes werden nicht ausgegeben.
- Strings sind nullterminierte UTF-8-Bytefolgen; sie werden im Inventar nicht
  dekodiert oder exportiert.
- Für `byte` gilt die FIT-Regel, dass das Byte-Array als Ganzes ungültig ist,
  wenn alle Elemente `0xFF` sind. Ansonsten zählt das Inventar seine
  Byte-Elemente, ohne sie zu publizieren.
- Bei `float32`/`float64` wird ausschließlich das standardisierte
  All-ones-Bitmuster als FIT-Invalidsentinel gezählt. Das Inventar veröffentlicht
  weder Floatwerte noch eine wissenschaftliche Bewertung.
- `valid_value_count` und `invalid_or_missing_count` sind Aggregate. Letzterer
  fasst Invalidsentinel-Elemente und Records der Nachricht zusammen, in denen
  das Feld fehlt. `record_count` zählt nur Records, in denen die Felddefinition
  tatsächlich vorkommt. Siehe die Erläuterungen im [Inventurbericht](CIRQA_OFFLINE_FILE_INVENTORY_2026-11-18.md).

## Profilmapping ist keine Feldinterpretation

Die CSV-Spalten `field_name`, `scale`, `offset` und `unit` kommen aus Garmin
FIT Profile 21.222.0, sofern dieses Profil die Message-/Feldnummer kennt. Sie
sind **Mappingreferenz**. Die `base_type`- und `field_size`-Spalten werden
hingegen in jeder konkreten FIT-Datei aus der Felddefinition beobachtet.
Unbekannte IDs bleiben numerisch dokumentiert; es wird keine Bedeutung
hinzuerfunden. Auch ein bekannter Name oder eine Einheit belegt weder eine
klinische Semantik noch einen Import in PulseLibre.

## Erkennung und Tests im Inventurtool

`tools/fit_inventory.py` maskiert für den Basistyp die unteren fünf Bits,
bewahrt die beobachtete Feldgröße und Endianness getrennt und zählt
FIT-Invalidsentinels typabhängig, ohne Werte auszugeben. Regressionstests
decken die 17 IDs/Größen und Sentinelklassen sowie Arrays, Big-Endian,
komprimierte Zeitstempel, fehlende Zeitstempel und unbekannte Felder ab.

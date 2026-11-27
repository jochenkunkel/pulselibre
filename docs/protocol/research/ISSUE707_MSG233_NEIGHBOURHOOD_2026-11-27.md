# #707 – msg233: C8-Nachbarschaft, E0/D0-Struktur, Tag-Varianten (27.11.2026)

Werkzeug: `tools/fit_msg233_neighbourhood.py` (nur lesend, gibt nur Zählungen aus; Tests in
`tools/test_fit_msg233_neighbourhood.py`). Datenbasis: lokales Archiv mit 46 FIT-Originalen vom S10, davon 27 mit msg233.
Es werden keine Rohwerte, Dateinamen oder Hashes veröffentlicht. „Tag“ = Byte 3 von Feld 2, eine beobachtete Kennung
ohne bestätigte Semantik.

## 1. C8 → nächste Nicht-233-Nachricht ist 21 (event) oder 227

| Gruppe | Einträge | Anteil mit Nachfolger 21/227 |
|---|---:|---:|
| Tag C8 | 50 | 0,360 |
| alle anderen Tags derselben Dateien | 26 090 | 0,043 |

Permutationstest (Tags innerhalb jeder Datei gemischt, 2 000 Durchläufe, Seed 1): **p = 0,0015**.
**Befund:** C8 steht deutlich häufiger direkt vor 21/227 als die Kontroll-Tags. Das belegt eine *Positionsbeziehung*
im Dateistrom, keine Bedeutung und keine Zeitgleichheit. 21 und 227 werden hier noch nicht getrennt ausgewertet.

## 2. E0/D0-Payloads

| Tag | Einträge | Varianten | verschiedene Werte je Byte 0/1/2 |
|---|---:|---:|---|
| E0 | 15 | 13 | 8 / 9 / 7 |
| D0 | 18 | 12 | 12 / 12 / 6 |

**Befund:** Keine feste Payload und keine Byteposition, die konstant ist. Damit gibt es in dieser Stichprobe keinen
Hinweis auf ein festes Längenfeld. Die Stichprobe ist klein (≤ 18); es ist keine Aussage über eine Struktur möglich.

## 3. Tag-Varianten je Dateityp (file_id)

| Dateityp | 20 | 23 | 24 | 30 | 33 |
|---|---:|---:|---:|---:|---:|
| activity | 2 069 | 35 | 29 | – | 2 132 |
| monitoring_b | 8 | – | – | 32 | – |

**Befund:** Bestätigt die Trennung aus dem Ticket: 33 kommt nur in Aktivitäten vor, 30 nur in Monitoring. 23/24
sind seltene Aktivitäts-Varianten. Ein Aktivitätsdecoder darf nicht auf Monitoring angewendet werden.

## Status

Keine produktive Nutzung von msg233. Offen bleiben die Trennung von 21 und 227, eine Falsifikation an unabhängigen
Aufzeichnungen (Nachttest) und die Varianten 00/08 gegen Zeitanker.

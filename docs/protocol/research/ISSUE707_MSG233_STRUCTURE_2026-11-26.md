# #707 – Struktur der proprietären FIT-Nachricht 233 (26.11.2026)

Offline-Analyse der 37 lokalen CIRQA-Originale vom 25.11.2026 (Testband, nicht im Repository). Nachricht 233 steht nicht im Garmin-Profil 21.222.0. Alles Folgende ist **Struktur**, keine bestätigte Bedeutung. Es gibt keine Produktnutzung und keine Gesundheitsaussage.

## Befunde

**Vorkommen**
- 14 445 Nachrichten, davon 12 844 in einer großen Aktivitätsdatei.
- Die Nachricht hat genau ein Feld: 2, `byte[4]`, ohne Zeitstempel (kein Feld 253).

**Takt** (BEWIESEN, große Datei)
- Auf jedes `record` (20) mit 1 Hz folgt genau eine 233.
- Die Zeit ergibt sich nur aus der Position im Datenstrom.

**Byte 3 = Kanal-Tag** (BEWIESEN)
- Bits 3–7 enthalten einen Index, Bits 0–2 Flags.
- Die Indizes 0–10 laufen als **11-Kanal-Round-Robin** (0, 1, …, 10, 0, …), also ein Kanal pro Sekunde.

**Kanäle 0–10** (große Datei, je ≈ 1 110 Werte)

| Index | Tag | Inhalt Bytes 0–2 |
|---:|---|---|
| 0 | 0x00 | Byte 0 konstant 39, uint16 (Bytes 1–2) variabel 1…1485 (75 Muster) |
| 1 | 0x08 | Byte 0 0…52, uint16 1600…16352 (11 Muster) |
| 2–5 | 0x10–0x28 | 1–2 Muster, nahezu konstant |
| 6 | 0x33 (Flags 3) | konstant |
| 7 | 0x38 | konstant |
| 8–10 | 0x40–0x50 | konstant 0 |

- Die Kanäle 2–10 sind konstant oder fast konstant und sehen nach Status- oder Konfigurationsregistern aus. Das ist **plausibel, nicht bewiesen**.

**Block C0 → B8 → B0** (Index 24/23/22)
- Etwa alle 64 s: 191-mal in der großen Datei, in weiteren Dateien vereinzelt.
- **C0:** `uint16_le(Bytes 1–2) / 4` liegt im Mittel **0,59 bpm** neben der Herzfrequenz des benachbarten `record`. Gemessen an 191 Blöcken; über alle Dateien 205 Werte mit einer Abweichung um 1 bpm. Das ist **PLAUSIBEL** die momentane Herzfrequenz in 0,25-bpm-Auflösung. Sie ist redundant zu 20/3.
- **B8:** Gleiche Kodierung, mittlere Abweichung 3,7 bpm zum Momentanwert. Sie passt weder zum Mittel, Minimum oder Maximum der 64 s davor noch zum Mittel der 64 s danach. **Bedeutung nicht nachgewiesen.**
- **B0:** keine Herzfrequenz; Byte 0 0…128, uint16 0…16.

**Seltene Tags:** 0xC8, 0xD0, 0xE0, 0xF0, 0xF8 und 0xFF, je ≤ 22-mal.

## Konsequenz für PulsLibre
- 233 bleibt byte-genau als Rohfeld und im Unbekannt-Inventar erhalten. Der Test `proprietary233And397StayRawUnchangedAndNeverBecomeMeasurements` sichert das ab.
- C0 würde gegenüber dem Standardfeld 20/3 keinen neuen Messwert liefern. Deshalb gibt es **keine** Produktfreigabe.

## Offen
- Bedeutung von B8 und B0 sowie der Kanäle 0 und 1.
- Wie 233 in Monitoring-Dateien ohne `record` taktet.
- 397/1 (float32), Einheit unbekannt.

Ohne Garmin-Dokumentation ist dafür eine Gegenprobe mit kontrollierter Belastung nötig, etwa Ruhe gegen Bewegung bei bekanntem Puls.

# FIT-Feldanalyse der 45 lokalen Originale (2026-11-21)

**Status:** Beobachtung und Hypothesen aus einer lokalen Rohauswertung. **Keine Messwerte, Zeiten, Koordinaten oder Hashes in diesem Dokument.**
Die Rohausgabe liegt nur lokal. Methode: `tools/fit_field_survey.py` (eigener kleiner FIT-Parser) auf die per App exportierten Originale (Daten > „FIT-Originale exportieren“).
Die Bedeutungen unten stammen aus dem öffentlichen FIT-Profil **aus dem Gedächtnis**, nicht gegen das SDK-Profil geprüft. Sie sind Hypothesen, bis OpenCode sie mit Profil und Test belegt.

## Hauptbefund: Die „1.622 Herzfrequenzwerte ohne Zeitstempel“ sind rekonstruierbar
- Alle 1.622 stammen aus **Monitoring-Nachrichten (Nr. 55), Feld 27 (Herzfrequenz)**. Diese Nachrichten tragen kein volles `timestamp` (Feld 253), sondern **`timestamp_16` (Feld 26, uint16)**.
- Das FIT-Profil sieht dafür die Standardregel vor: `ts = letzter_voller_ts + ((ts16 − (letzter_ts & 0xFFFF)) & 0xFFFF)`, fortlaufend je Datei. Mit dieser Regel bekommen **alle 1.622** Werte einen Zeitpunkt (Zahl stimmt exakt mit dem Diagnosewert überein).
- Wirkung auf die Lücken: Mit diesen Werten sinkt die Summe der Herzfrequenz-Lücken über 5 Minuten (Live + Band) um etwa einen Tag (von ca. 4 d 4 h auf ca. 3 d 4 h), vor allem am zweiten Tag der großen Lücke.
- Das widerspricht nicht der Vorsicht aus `fit-history-data-contract`: Die Rekonstruktion ist keine Schätzung, sondern die im Profil definierte Regel. Sie braucht trotzdem Decoder-Test und Qualitätsmarke (z. B. `RECONSTRUCTED_FROM_TIMESTAMP_16`), damit die UI sie kennzeichnen kann.

## Weitere Befunde je Nachricht (Hypothesen)
| Nr. | Vermutete Bedeutung | Belege (nur Struktur) |
|---|---|---|
| 20 `record`, Felder 0/1 | Position (Semicircles, sint32) | in nur ca. 1,8 % der Datensätze gültig; sonst Ungültig-Marke. Datenschutzrelevant: **Positionsdaten sind vorhanden** |
| 20, Feld 136 | Handgelenks-Herzfrequenz (uint8) | gleiche Werteverteilung wie Feld 3 (Herzfrequenz), 12.730-mal |
| 20, Feld 135 | unbekannt (uint8, 0–176) | viele verschiedene Werte, je Datensatz neben der Herzfrequenz |
| 20, Feld 143 | evtl. Atemfrequenz (uint8, 5–24) | 15 von 17 Dateien |
| 55 `monitoring`, 3/19/33/34 | Schritte/Zyklen, aktive Kalorien, moderate/intensive Minuten | ca. 150 bzw. 18/4 Datensätze in 4 Dateien (passt zu OpenCodes Inventur) |
| 55, 4, 2, 29 | aktive Zeit, Strecke, Dauer (Minuten) | plausible Wertebereiche |
| 227 | Stress (Wert + Zeit), 3 Zusatzfelder unbekannt | 1.977 Datensätze in 4 Dateien; Werte mit Marken für „nicht messbar“ |
| 297 | Atemfrequenz (Skalierung 100) | wie Stress, mit Ungültig-Marken |
| 269 | SpO₂ (Wert, Konfidenz, Modus) | 1.693 Datensätze in 4 Dateien |
| 275, 273, 346 | Schlafphasen, Schlafinfo mit Versionstext, Schlafbewertung | 55 / 2 / 2 Datensätze in 2 Dateien |
| 371, 370 | HRV-Werte (Skalierung 128), HRV-Statuszusammenfassung | 148 / 2 Datensätze in 2 Dateien |
| 397 | evtl. Hauttemperatur-Abweichung (float, relativ) | 2.830 Datensätze in 2 Dateien, nur Hypothese |
| 233 | 4-Byte-Bitfeld je Datensatz (Zustand/Flags) | 15.385 Datensätze in 20 Dateien |

## Folgerungen
1. **Priorität 1 (OpenCode):** `timestamp_16`-Rekonstruktion im Decoder mit Test und Qualitätsmarke; danach erscheinen ca. 1.600 Herzfrequenzwerte in Verlauf und Übersicht (GUI-Anzeige trennt die Marke bereits nach Messzeit).
2. SpO₂ (269), Stress (227), Atmung (297), Schritte/Kalorien/Intensität (55) liegen als strukturierte, zeitgestempelte Reihen vor: **importierbar**, sobald Profil und Einheiten belegt sind. Das öffnet #329 und teilweise #43–#47. Schlaf und HRV (nur 2 Dateien, wenige Datensätze) bleiben dünn.
3. **Datenschutz:** Positions- und weitere Felder sollten beim Import bewusst gewählt werden (nicht alles übernehmen, nur belegte, benötigte Metriken).

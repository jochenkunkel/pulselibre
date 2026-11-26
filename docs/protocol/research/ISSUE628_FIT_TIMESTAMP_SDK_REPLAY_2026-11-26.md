# #628 – Zeitstempel-Replay gegen Garmin FIT SDK (26.11.2026)

**Frage (#628):** Fallen abgerufene historische Messwerte durch fehlende, falsch rekonstruierte oder falsch gefilterte Zeitstempel aus SQLite/GUI heraus?

**Antwort für die vorhandenen Originale: Nein.** Alle dekodierten Werte haben einen Zeitstempel, und Zeitstempel und Wert stimmen für jeden kanonischen Messwert mit einer unabhängigen Referenz überein.

## Methode
- Eingabe: 37 echte CIRQA-FIT-Originale aus dem lokalen Backup vom 25.11.2026 (nur lokal, nicht im Repository). Testarmband, keine Personendaten.
- PulsLibre-Decoder: `FitHistoricalObservations` aus `main` (DECODER_VERSION 6, Profil 21.222.0), auf der Desktop-JVM ausgeführt.
- Referenz: Garmin FIT SDK für Python `garmin-fit-sdk==21.222.0`. `timestamp_16` in monitoring (55/26) unabhängig nach der FIT-Regel rekonstruiert: letzter voller Zeitstempel plus 16-Bit-Differenz mod 65536.
- Vergleich je Datei, Messgröße, Zeitstempel (ms) und Wert.

## Matrix (Aggregate)

| Messgröße | PulsLibre | SDK | identisch (Datei+Zeit+Wert) | Abweichung |
|---|---:|---:|---:|---|
| Herzfrequenz | 13 703 | 13 703 | 13 703 | – |
| Distanz | 15 325 | 15 325 | 15 325 | – |
| SpO₂ | 631 | 631 | 631 | – |
| Stress | 617 | 658 | 617 | 41 SDK-Zeilen mit Statuswert −1/−2/0 (nicht messbar), korrekt ausgeschlossen |
| Atmung | 617 | 658 | 617 | 41 SDK-Zeilen mit Statuswert −1/0, korrekt ausgeschlossen |
| Geschwindigkeit | 95 | (95 als `enhanced_speed`) | – | SDK expandiert `speed` zu `enhanced_speed`, kein Decoderfehler |

Zeitstempelherkunft (PulsLibre, alle Messgrößen, 32 657 Beobachtungen):

| Herkunft | Anzahl |
|---|---:|
| DIRECT (Feld 253) | 32 079 |
| RECONSTRUCTED_TS16 (55/26) | 578 (davon 559 Herzfrequenz) |
| MISSING_TIMESTAMP | **0** |
| Qualität ≠ PROFILE_VALID | **0** |

## Importfehler
- Die 20 historischen Fehler vom 24.11. (Build 2062) lassen sich nicht mehr klassifizieren, weil es damals keine Fehlerdatensätze pro Datei gab.
- Seit #720 speichert `import_errors` für jede Datei Klasse und Grund.
- Im Gerätelauf am 26.11. (main `df941ffc`) gab es **0 Fehler**: 29 Dateien übertragen, 27 dekodiert und committet, 0 SOURCE_CONFLICT.

## Fazit
- **Bewiesen:** Für diese 37 Originale gehen keine Werte durch Zeitstempel verloren. Die `timestamp_16`-Rekonstruktion entspricht der FIT-Regel.
- **Plausibel:** Die fehlende Tageshistorie in der App hat eine andere Ursache, nämlich die Dateiauswahl des Bandes nach Garmin-Connect-Sync (siehe #637).
- **Nicht nachgewiesen:** die Ursache der 20 Fehler vom 24.11.
- Neue Fehler werden pro Datei protokolliert.

# PulsLibre: Vollständiger historischer Datenabruf (Projektziel)

## Verbindliches Ziel

PulsLibre soll **alle vom Garmin CIRQA über die verfügbare Geräteschnittstelle zugänglichen historischen Daten** herunterladen, nicht nur historische Herzfrequenzdaten.

Das umfasst grundsätzlich sämtliche vom Gerät angebotenen Dateien, Datensätze und Datentypen. Welche davon das konkrete CIRQA tatsächlich bereitstellt, muss durch Gerätebeobachtung und Protokolltests nachgewiesen werden. Keine Vorabfilterung auf Herzfrequenz, FIT oder einzelne Aktivitätstypen.

## Umsetzungskriterien

1. Alle verfügbaren Dateilisten und gegebenenfalls deren Seiten/Cursor vollständig und nachvollziehbar abrufen.
2. Alle vom Gerät zum Lesen freigegebenen historischen Dateien herunterladen; unbekannte Dateitypen nicht verwerfen.
3. Originalbytes verlustfrei erhalten, Integrität und Herkunft (ohne sensible Gerätekennungen in Diagnoselogs) prüfen.
4. Unterbrechungen, Wiederholungen und Dubletten sicher behandeln; Fortschritt und Vollständigkeit nachvollziehbar machen.
5. Dateninterpretation (Herzfrequenz, Bewegung, Aktivität und weitere Typen) **nachgelagert** durchführen, ohne den Rohdatenabruf einzuschränken.
6. Keine löschenden oder verändernden Gerätebefehle verwenden (insbesondere kein FileSetFlags, kein Reset und kein CLOSE_ALL ohne separate Verifikation und Freigabe).
7. Keine Behauptung vollständiger historischer Datenabdeckung, bevor Dateiliste, Paginierung und alle zugänglichen Dateien am CIRQA nachweislich geprüft wurden.

## Aktueller Stand (2026-11-18, V25 – nachgewiesen)

- Der Connect-kompatible read-only Sync ist am echten CIRQA bestätigt: `sync_request` → `sync_response` → Item-Liste → Pull → MLR-Datenübertragung.
- V24 und V25 haben jeweils **33/33 gelistete Dateien vollständig** übertragen (146.293 Byte); privat gespeichert sind **31 FIT-Dateien (126.089 Byte)** und **2 Nicht-FIT-Rohdateien (20.204 Byte)**.
- **31/31 FIT-Dateien** bestanden unabhängige Prüfungen von Magic, Länge, Header-/Datei-CRC-16 und SHA-256/Dateiname.
- SQLite nach V25: `integrity_check=ok`, **294 Beobachtungen** (`heart_rate=129`, `distance=129`, `speed=36`), keine doppelten Primärschlüssel. Die Wiederholung war idempotent.
- **Pagination korrigiert:** Nur eine vorhandene `session_id` setzt dieselbe Liste fort; `next_transaction_id` ist hier **kein** Seiten-Cursor. V25 endete nach einer Seite mit 33 Einträgen.
- **Nicht nachgewiesen:** Dass die 33 Einträge die gesamte historische Datenmenge abdecken; dass alle enthaltenen FIT-Felder bereits ausgewertet werden; dass Schlaf-Score/Schlafphasen aus dem Gerät stammen.
- **Keine weiteren Gerätesitzungen freigegeben:** V24/V25-Einmal-Gates sind verbraucht. Keine Lösch-/Archiv-/Reset-/Flag-/`CLOSE_ALL`-Operationen.

Belege: [V24](protocol/device-tests/CIRQA_SYNC_V24_DEVICE_RESULT_2026-11-18.md), [V25](protocol/device-tests/CIRQA_SYNC_V25_DEVICE_RESULT_2026-11-18.md), [Forschungsstand](protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md).

## Feldinventur und Erkenntnisstufen

Die Originaldateien liegen **privat auf dem Android-Gerät**, nicht im Git-Repository. Der FIT-Decoder [`FitHistoricalObservations.kt`](../app/src/main/java/de/pulselibre/FitHistoricalObservations.kt) kennt folgende Zuordnungen, die **nicht** mit tatsächlich beobachteten Feldern verwechselt werden dürfen:

| FIT Global Message | Feld | Decoder-Metrik | Einheit / Skalierung | V25-SQLite-Import |
| --- | ---: | --- | --- | --- |
| 20 Record | 3 | heart_rate | bpm | 129 insgesamt, Quelle je Nachricht nicht ausgewiesen |
| 20 Record | 4 | cadence | rpm | kein Import belegt |
| 20 Record | 5 | distance | m, raw/100 | 129 insgesamt |
| 20 Record | 6 | speed | m/s, raw/1000 | 36 insgesamt, inkl. Feld 73 möglich |
| 20 Record | 7 | power | W | kein Import belegt |
| 20 Record | 73 | enhanced_speed → speed | m/s, raw/1000 | nicht getrennt ausgewiesen |
| 55 Monitoring | 9 | cumulative_steps | Schritte, raw×2 | kein Import belegt |
| 55 Monitoring | 27 | heart_rate | bpm | nicht getrennt ausgewiesen |
| 20/55 | 253 | timestamp | FIT-Epoche → Unix-Zeit | zeitliche Feldabdeckung noch offen |

Vier strikt getrennte Evidenzstufen: **FIT-Profil kennt Feld** → **Decoder unterstützt Feld** → **CIRQA-Datei enthält Feld nachweislich** → **Wert ist in SQLite importiert**. Zusätzlich wurden `MONITORING_B`, `SLEEP_DATA`, `BIOMETRIC` und `TRUE_UP` in Garmin-Connect-Gerätebeobachtungen gesehen; diese Typen sind den 33 Dateien **nicht sicher zugeordnet**. Schlafbeginn, Schlafphasen, Schlaf-Score, HRV, Ruhepuls, Stress, SpO₂ usw. sind bislang **nicht als konkrete heruntergeladene Felder nachgewiesen**.

**Offener Auftrag:** [Issue #141 – vollständige Offline-Feldinventur](https://github.com/jochenkunkel/pulselibre/issues/141) aller 31 FIT- und 2 Rohdateien, einschließlich sämtlicher Nachrichtentypen, Feldnummern, Developer Fields, Skalierungen, Vorkommen, Datenqualität, Importabdeckung und anonymisierter Maschinenlesbarkeit. Keine Rohgesundheitsdaten ins Git; kein neuer BLE-Test für diese Inventur.

## Priorisierung

Zuerst sichere Protokollinitialisierung und vollständige Verzeichnis-/Dateiübertragung; anschließend generische Rohdatenspeicherung, Wiederaufnahme und Integritätsprüfung; erst danach datentypspezifische Analyse.

Dieses Dokument korrigiert frühere Formulierungen, die das Projektziel fälschlich auf historische Herzfrequenzdaten verengt haben.

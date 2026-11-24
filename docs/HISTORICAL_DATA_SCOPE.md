# PulsLibre: Vollständiger historischer Datenabruf (Projektziel)

> **Aktuelle P0-Owner-Entscheidung (24.11.2026):** [Vollständige lokale Armband-Datenerfassung](DATA_ACQUISITION_PRIORITY.md). Auch unkalibrierte, unvalidierte und unbekannte Werte erfassen und im privaten Archiv plus SQLite-Inventar nachhalten. Die untenstehenden älteren Geräte-/Teststände sind historische Evidenz, keine Erfassungsverbote aufgrund medizinischer Unsicherheit.


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

## Aktueller Stand (2026-11-19, V26 – nachgewiesen)

- Der Connect-kompatible read-only Sync ist am echten CIRQA bestätigt: `sync_request` → `sync_response` → Item-Liste → Pull → MLR-Datenübertragung.
- V24/V25 übertrugen jeweils **33/33 gelistete Dateien**. V26 übertrug **42 gelistete Dateien**: 40 FIT und 2 Nicht-FIT, insgesamt 218.951 Byte.
- V26-FIT-Dateien bestanden lokale Hüllen-/CRC-Prüfungen; SHA-256 dient der privaten content-addressed Ablage. Die beiden Nicht-FIT-Dateien wurden nicht semantisch interpretiert.
- Die kanonische SQLite-Datenbank hatte nach V26 `integrity_check=ok` und weiterhin **294 normalisierte Beobachtungen** (`heart_rate=129`, `distance=129`, `speed=36`); der wiederholte Import führte zu keinen Duplikaten.
- **Pagination ungeklärt:** V25 endete nach einer Seite mit 33 Einträgen. In V26 blieb die begrenzte Listenseite ebenfalls eine Vollständigkeitslücke. Ein FIT aus dem V24/V25-Archiv wurde in V26 nicht beobachtet; zehn FIT-Inhalte und eine Rohdatei waren neu gegenüber dem vorherigen privaten Archiv.
- **Nicht nachgewiesen:** Dass eine der beobachteten Listen die gesamte historische Datenmenge abdeckt; dass alle FIT-Felder ausgewertet werden; dass Schlaf-Score/Schlafphasen aus dem Gerät stammen.
- **Keine weitere Gerätesitzung oder Gate-Änderung autorisiert:** V24/V25-Einmal-Gates bleiben verbraucht. Keine Lösch-/Archiv-/Reset-/Flag-/`CLOSE_ALL`-Operationen.

### Produktintegration und Inkrementalität

- V26 übertrug in einem ausdrücklich gestarteten Testlauf 42 gelistete Dateien (40 FIT, 2 Nicht-FIT; 218.951 Byte). 10 FIT-Inhalte und eine Rohdatei waren neu gegenüber dem privaten V24/V25-Archiv; ein früheres FIT wurde in der V26-Liste nicht beobachtet. Das Ergebnis belegt weiterhin keine vollständige Gerätehistorie.
- Der tatsächlich bewiesene Dateiabruf läuft über den Testlaborpfad `startSyncRequestTest`; er wird nicht beim App-Start oder im Hintergrund automatisch ausgeführt.
- Lokale SHA-256-Speicherung und FIT/SQLite-Reimport sind inhaltsbasiert und idempotent. **Das ist kein inkrementeller Funkabruf:** die SHA-256 ist erst nach dem Pull bekannt.
- Der lokale, begrenzte Sync-Bericht weist `runDrained` (Queue dieses Laufs abgearbeitet) getrennt von `coverage=device-history-unverified` aus. Datei-/Seiten-/Bytebudget-Überschreitungen stoppen den Lauf explizit und erscheinen als unvollständig; sie werden nicht mehr still übersprungen und als Erfolg zusammengefasst. Ein FIT zählt im Datei-Erfolgszähler erst nach gültiger privater Speicherung **und** erfolgreichem kanonischem SQLite-Commit; Raw-Dateien erst nach exakter privater Speicherung.
- Die Geräteansicht bietet jetzt einen ausdrücklich bestätigten, benutzerinitiierten historischen Sync über den vorhandenen Connect-kompatiblen 5043/MLR-Pfad; Testlabor-Einmal-Gates sind dafür nicht erforderlich. Der Dialog warnt, dass der Sync-Request das Gerät als übertragen markieren kann. Es gibt weiterhin keinen automatischen Hintergrund-BLE-Abruf.
- Die bisherige Pipeline bleibt ein ausdrücklich gestarteter, sicherheitsbegrenzter Testlaborlauf (bis 256 Dateien, 64 MiB je Lauf, 8 MiB je Datei, 120 s und höchstens 64 List-Seiten); das ist noch kein unbegrenzter/resumierbarer Produkt-Downloader. Erreichen der Limits oder Verbindungsabbruch verlangt einen neuen Lauf; stabiler Geräte-UID-Checkpoint über Sitzungen ist nicht implementiert.
- Für einen sicheren Skip vor dem Pull fehlen belegte Semantiken zur Stabilität und Wiederverwendung der File-UIDs zwischen Listungen bzw. nach Geräte-Retention/Dateiersetzung. Daher darf der Produktivpfad aktuell nicht anhand einer ungeprüften UID/Größe/Dateityp-Kombination Dateien überspringen.
- Kein automatischer Hintergrundabruf, dauerhafter UID-zu-Importstatus, reconnect/resume-Checkpoint oder vollständiger List-Paging-Nachweis ist implementiert. Die dafür nötige Identitäts-/Delta-Forschung bleibt in [#148](https://github.com/jochenkunkel/pulselibre/issues/148), die Pipeline-Härtung in [#149](https://github.com/jochenkunkel/pulselibre/issues/149).

Belege: [V24](protocol/device-tests/CIRQA_SYNC_V24_DEVICE_RESULT_2026-11-18.md), [V25](protocol/device-tests/CIRQA_SYNC_V25_DEVICE_RESULT_2026-11-18.md), [V26](protocol/device-tests/CIRQA_SYNC_V26_DEVICE_RESULT_2026-11-19.md), [Forschungsstand](protocol/research/CIRQA_HISTORICAL_RESEARCH_STATUS_2026-11-18.md).

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

## Verbindliche P0-Präzisierung zum erneuten Full-Pull (24.11.2026)
**Unbewiesene Geräte-/Dateiidentität ist kein Hindernis für den Download.** Sie verbietet nur das Überspringen vor der Übertragung. Im Zweifel alle gelisteten Dateien erneut read-only herunterladen, Originalbytes und Abrufstatus sichern und Duplikate erst lokal anhand geprüfter Inhalte behandeln. Nach Abbruch dürfen bereits gesicherte Daten nicht verloren gehen; der nächste ausdrücklich autorisierte Lauf darf erneut vollständig lesen. Ein optimierter selektiver Resume ist optional und erfordert Evidenz, aber sein Fehlen blockiert weder Full-Pull noch Import noch die Implementierung von Lauf-/Fehlerprotokollen. Laufbudgets und Hardwarefreigaben bleiben in Kraft; ein Budgetstopp bleibt ausdrücklich unvollständig und ist keine nachgewiesene Geräteabdeckung.

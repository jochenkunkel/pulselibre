# Datenexport für Studien und eigene Auswertungen

## Warum
Die Nutzerin/der Nutzer möchte Messdaten für eine externe Studie zur Heilung eines Oberarmbruchs auswertbar bereitstellen. Das Exportformat darf die ursprünglichen Daten nicht durch Rundung, Aggregation, Glättung oder Interpretation verfälschen.

## Modulgrenzen
- **RawDataExport** ist eine reine, Android-unabhängige CSV-Serialisierung mit Zeitfilter.
- **DataRepository** (geplant) liefert persistierte Rohmessungen; es ist nicht Teil des Exporters.
- **ExportScreen** (geplant): Einstellungen → Datenexport → Von/Bis → Vorschau (Anzahl, Messgrößen, Zeitraum) → Exportieren.
- **Android-Dateiauswahl** (geplant): ACTION_CREATE_DOCUMENT/Storage Access Framework; keine unnötige Speicherberechtigung, kein Upload.
- **AggregatedExport** (später): separat versionierte Auswertung mit Statistik, Grafiken, Bericht/PDF. Keine Aggregation im Rohdatenpfad.

## CSV v1
UTF-8, Komma-getrennt, CRLF, RFC-4180-artige doppelte Anführungszeichen; Tabellenkalkulations-Formelinjektion bei Textfeldern wird entschärft.
Spalten: `device_id,metric,measured_at_utc,measured_at_epoch_ms,value,unit,sample_id,received_at_utc`.
Zeitintervall: `[start, end)` in UTC-Epoch-Millisekunden; UI-Kalendertage später in der ausgewählten Zeitzone zu UTC-Grenzen umrechnen. Gemessener Zeitpunkt und Empfangszeitpunkt bleiben getrennt.
Es werden alle **lokal vorhandenen** gültigen Rohdatensätze des gewählten Intervalls ausgegeben; fehlende Daten, nicht unterstützte Gerätedaten und noch nicht synchronisierte Zeiträume sind nicht rekonstruierbar. Exportiert wird **keine** Gesundheitsakte, Diagnose oder Forschungsinterpretation.

## Datenschutz und Forschung
- Export ausschließlich nach expliziter Aktion; keine Cloud, kein automatischer Versand.
- Dateiname und optionales Manifest später mit Exportzeitpunkt, App-/Schema-Version, Zeitzone, Quellgerät, Einheiten und Synchronisierungsstand; persönliche Identifikatoren möglichst pseudonymisieren.
- Achtung: CSV-Dateien enthalten sensible Gesundheitsdaten und können nach dem Export außerhalb der App weitergegeben werden.
- Spätere klinische Studie kann ein festes Datenwörterbuch/Studienprotokoll verlangen; dann Format versionieren, nicht still ändern.
- Für sehr große Datenmengen später Streaming-Export statt vollständiger String-Erzeugung.

## Stand
**Implementiert:** pure CSV-Funktion, Zeitfensterprüfung, zeitliche Sortierung, Rohwert-/Zeitstempelerhalt, CSV-Escaping und JVM-Tests.
**Noch nicht implementiert:** persistierte Datenbank, Android-Exportknopf/Dateidialog, Manifest, Studien-Datenwörterbuch, aggregierte Berichte. CI-Erfolg der neuen Tests gesondert prüfen.

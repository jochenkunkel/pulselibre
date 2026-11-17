# Datenexport für Studien und eigene Auswertungen

## Warum
Die Nutzerin/der Nutzer möchte Messdaten für eine externe Studie zur Heilung eines Oberarmbruchs auswertbar bereitstellen. Das Exportformat darf die ursprünglichen Daten nicht durch Rundung, Aggregation, Glättung oder Interpretation verfälschen.

## Modulgrenzen
- **RawDataExport** ist eine reine, Android-unabhängige CSV-Serialisierung mit Zeitfilter.
- **ExportActivity** wählt Zeitraum und Ziel über Androids Storage Access Framework. Der Export wird nur nach dem Antippen der Export-Schaltfläche gestartet; es wird keine allgemeine Speicherberechtigung benötigt.
- **Datenquelle:** `ExportActivity.readUnifiedExport()` liest alte lokale `LocalReadingStore`-Datensätze (idempotent in die CIRQA-Datenbank übernommen) und bekannte numerische Felder aus `CirqaDatabase`. Der Serializer selbst greift nicht auf Android oder Datenbanken zu.
- **AggregatedExport** (später): separat versionierte Auswertung mit Statistik, Grafiken, Bericht/PDF. Keine Aggregation im Rohdatenpfad.

## CSV v1
UTF-8, Komma-getrennt, CRLF, RFC-4180-artige doppelte Anführungszeichen; Tabellenkalkulations-Formelinjektion bei Textfeldern wird entschärft.
Spalten: `device_id,metric,measured_at_utc,measured_at_epoch_ms,value,unit,sample_id,received_at_utc`.
Zeitintervall: `[start, end)` in UTC-Epoch-Millisekunden. Die Exportansicht stellt Von/Bis-Datum und -Uhrzeit in der Systemzeitzone ein; die gewählte Endminute ist inklusive und wird intern als exklusive Grenze der folgenden Minute behandelt. CSV-Zeitwerte werden als UTC ausgegeben.
Es werden alle **lokal vorhandenen** gültigen Rohdatensätze des gewählten Intervalls ausgegeben; fehlende Daten, nicht unterstützte Gerätedaten und noch nicht synchronisierte Zeiträume sind nicht rekonstruierbar. Exportiert wird **keine** Gesundheitsakte, Diagnose oder Forschungsinterpretation.

Für CIRQA liefert das beobachtete Format hier nur den Telefon-Empfangszeitpunkt. Deshalb werden `measured_at_*` und `received_at_utc` im Export auf denselben Empfangszeitpunkt gesetzt; das belegt keinen Messzeitpunkt im Gerät. Die Kennung `CIRQA` ist eine generische Quellenbezeichnung und keine eindeutige oder pseudonymisierte Geräte-ID. Legacy-Datensätze behalten ihre gespeicherten Mess- und Empfangszeitpunkte.

## Datenschutz und Forschung
- Export ausschließlich nach expliziter Aktion; keine Cloud, kein automatischer Versand.
- Dateiname und optionales Manifest später mit Exportzeitpunkt, App-/Schema-Version, Zeitzone, Quellgerät, Einheiten und Synchronisierungsstand; persönliche Identifikatoren möglichst pseudonymisieren.
- Achtung: CSV-Dateien enthalten sensible Gesundheitsdaten und können nach dem Export außerhalb der App weitergegeben werden.
- Spätere klinische Studie kann ein festes Datenwörterbuch/Studienprotokoll verlangen; dann Format versionieren, nicht still ändern.
- Für sehr große Datenmengen später Streaming-Export statt vollständiger String-Erzeugung.

## Stand
**Implementiert:** pure CSV-Funktion, Zeitraumfilter, chronologische Sortierung, Rohwert- und Zeitstempelerhalt, Formelinjektionsschutz, lokale SQLite-Datenquelle, Datums-/Uhrzeitauswahl und Android-Dateidialog. JVM-Tests decken Serialisierung und Zeitgrenzen ab; Android-Instrumentierung prüft UI und Activity-Recreation. Ein Prozess-Kill- oder S25-Exporttest ist nicht durch diese Tests ersetzt.

**Noch nicht implementiert:** Studien-Datenwörterbuch, pseudonymisierte Studien-ID und aggregierte Berichte. Exportdateien enthalten sensible Gesundheitsdaten und bleiben nach dem Speichern unter Kontrolle des gewählten Dokumentanbieters; die App kann keine nachgelagerte Weitergabe oder Kopien kontrollieren. CI-/Geräteergebnisse sind separat zu belegen.

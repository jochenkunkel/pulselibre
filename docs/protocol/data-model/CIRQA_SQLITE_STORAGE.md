# CIRQA local SQLite storage v1

Stand: 2026-11-17, Bereinigungs-PR. `CirqaDatabase` verwendet Androids
`SQLiteOpenHelper`, ohne Cloud und ohne INTERNET-Berechtigung.

## Schema

`events(id INTEGER PRIMARY KEY, line_sha256 TEXT UNIQUE NOT NULL, received_at_utc TEXT NOT NULL, kind TEXT NOT NULL, service INTEGER, payload_json TEXT NOT NULL)`

Indizes: `(service, received_at_utc)` und `(kind, received_at_utc)`.
Messfelder bleiben als JSON in `payload_json`; SHA-256 dedupliziert identische Ereignisse.
Schema und vorhandene SQLite-Ereignisse werden durch die Bereinigung nicht verändert.

## Schreib-/Lesepfade

`CirqaLocalLog.append()/reading()` schreibt unmittelbar SQLite. Dashboard, Statistik,
CSV und nutzergesteuerter JSONL-Export lesen SQLite. Es gibt kein zusätzliches
JSONL-Aufzeichnungsjournal mehr.

Auf ausdrücklichen Nutzerwunsch wird die frühere private `cirqa_overnight.jsonl`
nicht mehr importiert. Beim Öffnen der Datenbank wird nur diese Datei bestmöglich
entfernt, ohne Verzeichnisse rekursiv zu löschen. Bereits importierte Ereignisse
bleiben erhalten; alte, noch nicht importierte Dateiinhalte werden verworfen.
Andere Dateien und Exporte werden nicht gelöscht. Die Bereinigung erfolgt erst,
wenn eine App-Version mit dieser Änderung auf dem Gerät die Datenbank öffnet.

Der separate Import aus `pulselibre_readings.db` beim CSV-Export bleibt erhalten.
Siehe [einheitliche Speicherung](../../UNIFIED_LOCAL_STORAGE_2026-11-16.md).

## Tests und Grenzen

JVM-Tests prüfen gezielte Dateientfernung, Wiederholung, Erhalt anderer Dateien
und Ablehnung rekursiver Verzeichnislöschung. Ein Android-Test mit isoliertem
Datenbank-/Dateiverzeichnis prüft zusätzlich Öffnen/Wiederöffnen, Erhalt vorhandener
SQLite-Ereignisse und ausbleibenden JSONL-Import. Tatsächliche Prüfergebnisse stehen
im Pull Request; Testcode allein ist kein Nachweis eines erfolgreichen Laufs.

Lokale Prüfung dieses Bereinigungsstands (2026-11-17, Gradle 8.13/JDK 17/SDK 35):
227 JVM-Tests bestanden, App-APK und Android-Test-APK gebaut, `lintDebug` erfolgreich.
Der Android-Integrationstest wurde kompiliert, aber nicht ausgeführt; kein
Emulator-/S25-Nachweis und keine Installationsfreigabe.

Offen bleiben Hintergrundausführung und begrenzte Abfragen für große Bestände,
Aufbewahrung, additive Schema-Migrationen und vollständige Persistenztests.
Keine Schlafstadien, kein RMSSD aus einzelnen RR-Werten, keine Addition kumulativer
Schrittzähler und keine unbelegten Einheiten oder klinischen Aussagen.

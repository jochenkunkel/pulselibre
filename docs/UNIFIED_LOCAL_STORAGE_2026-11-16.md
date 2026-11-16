# Einheitlicher lokaler Datenspeicher (2026-11-16)

## Kanonische Datenbank
`cirqa_measurements.db` / `CirqaDatabase` ist der einzige aktive Schreibspeicher für CIRQA-Messungen und Sitzungsereignisse. `CirqaLocalLog.append()/reading()` schreiben unmittelbar in die SQLite-Tabelle `events`; der Vordergrunddienst `CirqaOvernightService` und manuelle BLE-Probes verwenden diesen Pfad.

## Leser und Export
Dashboard, lokale Statistik, JSONL-Export und CSV-Export lesen die kanonische Datenbank. CSV wandelt beobachtete numerische CIRQA-Felder in Zeilen um; Empfangszeit ist bei Echtzeitdaten mangels Gerätetimestamp zugleich Messzeit. Keine erfundenen Sensorwerte.

## Migration und Rückwärtskompatibilität
Das ältere `cirqa_overnight.jsonl` wird mit SHA-256-Deduplizierung idempotent importiert, bleibt aber unverändert als Sicherheitskopie liegen. Alte generische Messungen aus `pulselibre_readings.db` werden beim CSV-Export idempotent als `legacy_record` in die CIRQA-Datenbank importiert. Die alte SQLite-Datei wird bewusst nicht gelöscht. `LocalReadingStore` bleibt als Legacy-Leser für die Migration und vorhandene Tests im Code, darf aber nicht mehr für neue CIRQA-Messungen verwendet werden.

## Einschränkungen / Testpflicht
Noch kein bestätigter Android-Build oder S25-Hardwaretest für diese Änderung. Prüfen: Altbestand bleibt sichtbar; neue Aufzeichnung erhöht `events`; CSV und JSONL enthalten die neuen Daten; wiederholte Migration verdoppelt nichts; Start/Stopp und Speicherfehler; Prozessneustart. Periodische Abfrage alle etwa 120 Sekunden mit jeweils maximal 20 Sekunden BLE, keine kontinuierliche Rohdatenerfassung. Die Datenbank enthält nur tatsächlich empfangene und dekodierte Werte.

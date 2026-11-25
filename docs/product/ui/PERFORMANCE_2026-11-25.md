# Performance der Anzeige – Befunde 25.11.2026 und Einschätzung zu #140 (Rollup-Tabellen)

Gemessen am S10 (Builds 2171–2218) mit realen Daten: 37 FIT-Quellen, 57.687 Band-Beobachtungen, 316.455 FIT-Feldzeilen,
29.123 kanonische Messwerte, wenige hundert Live-Ereignisse.

## Was langsam war – und warum

| Symptom | Ursache | Korrektur | Ergebnis am S10 |
|---|---|---|---|
| Kachel-Tagesdialoge hingen dauerhaft | FIT-Reindexierung beim Start lief minutenlang auf dem einzigen Anzeige-Worker | eigener Thread (#651), einmal pro Prozess (#652) | Dialoge nach 2–3 s, Kacheln nach 4 s |
| Minuten Vollast nach Update | Migration las alle Live-Events neu, parallel aus mehreren Threads | `ALTER TABLE`, serialisierte Aktualisierung (#651) | einmaliger Neuaufbau, danach inkrementell |
| Übersicht/Verlauf ständig neu | jede neue Live-Event-ID (≈1/s) baute alles neu auf | höchstens alle 5 s / 15 s (#655) | nur bei Aufzeichnung relevant, am Gerät nicht gemessen |

Keiner der Engpässe lag in der Abfrage der Zeitreihen selbst. Verlauf (30 Tage, Band- und Live-Daten) und Tagesdialoge
laden nach den Korrekturen in wenigen Sekunden; die Wartezeiten kamen aus Thread-Zuordnung und Aktualisierungstakt.

## Einschätzung #140

- **Kein nachgewiesener Bedarf.** Die Datenmenge eines Bands über mehrere Wochen (≈ 60.000 Band-Werte) bleibt mit den
  vorhandenen Indizes (`unified_measurements_by_time`) und der Bündelung in `ScienceSeriesLoader` / `HistoryDataCache`
  schnell genug. Die Messungen vom 18.11. (Wechsel ≈ 1 s, SQL ≈ 50 ms) werden durch die heutigen Befunde nicht widerlegt.
- **Rollup-Tabellen hätten keinen der heutigen Fehler verhindert**; sie wären eine weitere abgeleitete Datenquelle, die
  synchron gehalten werden muss (vgl. die Migrationsprobleme von `unified_measurements`).
- **Wann neu bewerten:** wenn Daueraufzeichnung über Monate läuft (Millionen Live-Ereignisse) und Verlauf oder
  Analysen messbar > 2 s für den Fensterwechsel brauchen. Messpunkt: Zeit von Tab-/Fensterwechsel bis zur Anzeige.

**Empfehlung:** #140 schließen („nicht benötigt, bei Bedarf neu eröffnen“) oder zurückgestellt lassen. Entscheidung
liegt bei der Projektleitung.

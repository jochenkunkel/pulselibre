# Verlauf-Performance (Issue #79) – Messung, Entscheidung, Stand

## Gemessen
**S25, Build 989 → 996/1038, ca. 112 000 Ereignisse (≈ 16 000 Pulswerte/24 h), adb-Screenshot-Polling (Messboden ≈ 0,9 s):**

| Aktion | vorher | nachher |
|---|---|---|
| 24 h ↔ 7 d ↔ 30 d, bis stabiles Bild | 1,5–12 s | ≈ 1,0 s (0,99–1,08 s, 5 Messungen) |
| bis erste sichtbare Reaktion | n. g. | ≈ 0,33 s |
| 5 Taps ohne Pause | – | endet konsistent im letzten Modus, kein Absturz |

**SQLite (Kopie der Gerätedatenbank, Desktop, 30 Tage, 47 980 Zeilen, ohne JSON-Parsing):**
- Neue Abfrage (`dashboard_time >= ?`): `SEARCH events USING INDEX events_dashboard_time (service=? AND dashboard_time>?)`, warm 41–51 ms.
- Alte Abfrage (`julianday(received_at_utc)`): nur `SEARCH … USING INDEX events_kind_time (kind=?)` – der Zeitfilter wird **nicht** vom Index bedient; warm 48–53 ms (kalt 129 ms).
- Fazit: Die reine SQL-Zeit ist auf dem Desktop kein Engpass; auf dem Handy dominierte vorher das Parsen aller JSON-Zeilen und der synchrone Neuaufbau bei **jedem** Wechsel. Beides ist behoben (Cache der letzten 30 Tage in primitiven Arrays, inkrementell nach `id`, Laden im Hintergrund, „neueste Anfrage gewinnt“).

## Umgesetzt (Phase A)
Hintergrundladen, Generations-Token, 30-Tage-Cache mit Invalidierung bei Restore/Reset/Datenbankwechsel, indexierter Zeitfilter, Fensternavigation innerhalb des Caches, **Mittelwert** zusätzlich zum Median im Diagramm (Ring; Median bleibt gefüllter Punkt, Legende und Screenreader-Text nennen beides; die Median-Darstellung wurde nicht umetikettiert).
Composable Aggregate (`BucketStats`: min, max, sum, count, Mittel = sum/count, nie Mittel von Teilmitteln) mit Tests inkl. ungleicher Bucketgrößen, leerer Fenster, Reihenfolgeunabhängigkeit.

## Bewusst zurückgestellt (Phase B/E – persistente Rollup-Tabellen)
Der Auftrag macht Rollups von einer Messung abhängig („keine Rollup-Komplexität ohne nachgewiesenen Nutzen“). Nach der Messung (≈ 1 s bis stabil, SQL ≈ 50 ms) ist kein Nutzen nachgewiesen; zudem bräuchte es eine **Schemaänderung der gemeinsam genutzten Datenbank** (Abstimmung mit OpenCode, Migration, Rebuild-Logik, Backup/Export-Verträglichkeit). Entwurf für später: `metric_rollups(metric_id, bucket_start_epoch_ms, bucket_width_ms, min_value, max_value, sum_value, sample_count, updated_at, algorithm_version)` mit Unique-Key auf die ersten drei Spalten, halboffene UTC-Intervalle, 1-Minuten-Basis und 5-Minuten-Ableitung, Wiederaufbau aus Rohdaten; Median ist aus Rollups **nicht** ableitbar. Rohdaten werden nicht gelöscht.
Offene Messungen: Perfetto/StrictMode, Speicherprofil, 1M-Ereignis-Benchmark.

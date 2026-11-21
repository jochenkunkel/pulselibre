# Vorschlag: FIT-Decoder-Replay und Zeitstempel-Provenienz

Stand: 2026-11-21 · Bezug: #149 / #144  
Status: **Entwurf zur Abstimmung**, keine Schema- oder Produktcodeänderung

## Anlass

Der FIT-Decoder rekonstruiert Monitoring-Zeitstempel aus `timestamp_16` (Global
Message 55, Feld 26), persistiert für Observationen aber keine Herkunft
`DIRECT`/`RECONSTRUCTED_TS16`. Gleichzeitig bleibt
`FitHistoricalObservations.DECODER_VERSION` auf `1`.

`fit_sources` ist nach Quell-SHA-256 eindeutig. Der aktuelle Committer verlangt
beim erneuten Import identische Decoder-/Profilversionen und prüft bereits
persistierte FIT-Provenienz und Messwerte strikt. Eine bloße Erhöhung der
Decoder-Version würde einen normalen Backfill bereits importierter Quellen
deshalb als Konflikt abbrechen lassen. Die Version unverändert zu lassen,
unterscheidet die geänderte Decodersemantik dagegen nicht.

## Invarianten

1. Verifizierte FIT-Originale sind die unveränderliche Eingabe; FIT-Prüfung und
   content-addressed Dateiname bleiben Voraussetzung.
2. Metrik, Messzeit, Qualität und Quellen-Provenienz werden atomar aktualisiert.
   Ein Fehler darf weder alte abgeleitete Werte löschen noch einen neuen
   Quell-Decoderstand vortäuschen.
3. Wiederholung derselben Version und desselben Originals ist idempotent;
   gleiche Quellen mit inkompatiblen Decoderresultaten bleiben ein Fehler.
4. Vorhandene Provenienz unbekannter Zeitstempel-Herkunft wird nicht rückwirkend
   als direkt gemessen oder rekonstruiert etikettiert.
5. Berichte zeigen pro Quelle den tatsächlich angewendeten Decoderstand und
   Fehler; ein Teilerfolg gilt nicht als vollständiger Replay.
6. Backup/Restore muss Quellen, Beobachtungen, Provenienz und den wirksamen
   Decoderstand konsistent wiederherstellen können.

## Reprocessing-Optionen

### A. Kontrolliertes Ersetzen der abgeleiteten FIT-Daten je Quelle

- `fit_sources` bleibt die Identität des unveränderten Originals; der
  Decoderstand wird erst nach einem erfolgreichen Reimport aktualisiert.
- Ein ausdrücklicher Reprocessing-Pfad liest und validiert das private Original,
  dekodiert vollständig und ersetzt in **einer SQLite-Transaktion** nur die
  Beobachtungen/Provenienz dieser Quelle. Scheitert der Lauf, bleibt die alte
  Quelle vollständig erhalten.
- Eine Herkunftsspalte in `fit_observation_provenance` nimmt mindestens
  `DIRECT`, `RECONSTRUCTED_TS16` oder `NULL` (Altbestand unbekannt) auf. Neue
  Decoderpfade müssen Herkunft explizit setzen; `DEFAULT DIRECT` wäre für
  rückgerechnete Werte unsicher.
- Vor dem Rollout: verifiziertes Datenbankbackup und Prüfung, dass alle zum
  Replay benötigten Originale vorhanden sind. Pro-Quellen-Ergebnisse erlauben
  schrittweisen Replay und machen gemischte Decoderstände sichtbar.
- Vorteil: geringere Auswirkungen auf Abfragen/History als parallele
  Observation-Generationen; das Original bleibt die Quelle für Rollback.
- Nachteil: alte abgeleitete Observationen sind nach erfolgreichem Austausch
  nicht mehr in der Produkt-DB. Ein Rollback erfordert erneuten Replay oder
  Restore des geprüften Backups.

### B. Mehrere unveränderliche Decoder-Generationen je Quelle

- Jede Decoderrevision erhält einen eigenen Run-/Generationseintrag;
  Abfragen bestimmen explizit eine aktive Generation.
- History, Provenienz, Dedupe, Backups, Restore und Bereinigung müssen alle
  generation-aware werden.
- Vorteil: frühere abgeleitete Ergebnisse bleiben direkt vergleichbar und
  aktivierbar.
- Nachteil: größere Schema- und Abfrageänderung, mehr Speicher und deutlich
  mehr Stellen, an denen eine alte Generation irrtümlich als aktuell erscheinen
  kann.

**Vorläufige Arbeitshypothese für Review:** Option A ist kleiner, wenn die
geprüften verifizierten FIT-Originale zuverlässig erhalten und gemeinsam mit
der Datenbank gesichert werden. Diese Voraussetzung ist vor einer Umsetzung
zu belegen. Falls Originale nicht vollständig verfügbar sind oder ein
produktiver Revert ohne Re-Decode nötig ist, Option B neu bewerten.

## `respiration_rate` als nächster Decoderkandidat

Die Garmin FIT Profile **21.222.0**-Definition für Global Message 297
(`respiration_rate`) beschreibt Feld 0 als `sint16`, Skala 100, Einheit
`breaths/min`; Feld 253 ist der normale FIT-`timestamp`. Der FIT-Protokolltyp
`sint16` verwendet `0x7fff` als ungültigen Rohwert. Die bereits eingecheckte,
redigierte CIRQA-Inventur enthält dieses Message-/Feldschema; sie beweist für
sich weder Wertequalität noch Abdeckung oder Sensorsemantik.

Für eine spätere Implementierung:

- signedness und Sentinel vor Skalierung korrekt behandeln;
- Feld 253 als direkt vorhandenen FIT-Zeitstempel kennzeichnen (keine
  `timestamp_16`-Rekonstruktion für diese Nachricht);
- keine zusätzliche physiologische Range erfinden. Ein positiver, profilgültiger
  Rohwert ist zunächst nur ein FIT-kodierter Messwert, keine klinische
  Validierung;
- zunächst synthetische Parser-/Backfill-Tests und eine redigierte
  Kandidatenabdeckung prüfen; erst nach Replay-/Provenienzentscheidung in die
  persistierte Observation-Allowlist aufnehmen.

Es wurde kein FIT-Rohbestand erneut geöffnet und kein externer Decoder-Code
übernommen. Die Profilangaben sind Recherchefakten; Referenz ist Garmins
öffentliche `garmin/fit-python-sdk`-Profildefinition für Version 21.222.0 und
die FIT-Protokolldefinition für Base Type `sint16`. Die Repository-Inventur ist
als redigierte Beobachtung, nicht als unabhängiger Geräte- oder Semantikbeweis
zu verstehen. Referenzlinks: [Garmin FIT Profile
21.222.0 (`profile.py`)](https://raw.githubusercontent.com/garmin/fit-python-sdk/main/garmin_fit_sdk/profile.py)
· [Garmin FIT Protocol / Base Types](https://developer.garmin.com/fit/protocol/).

## Entscheidung benötigt

Issue #144 wird um eine Entscheidung zu Option A/B, unbekannter Altprovenienz,
Backup/Restore und Reprocessing-Fehlerverhalten gebeten. Bis dahin werden weder
DDL/Backupformat noch der persistierende FIT-Metrik-Whitelist geändert.

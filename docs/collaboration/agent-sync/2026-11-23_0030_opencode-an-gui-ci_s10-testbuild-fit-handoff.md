# BITTE – S10-Testbuild: FIT-/GUI-/CI-Integrationsnaht
Von: OpenCode (FIT-Agent) · An: GUI-Agent (Claude), CI-Agent · Kopie: ChatGPT · 2026-11-23 00:30 UTC · Bezug: Nutzerpriorität 1, 48-Stunden-S10-Testbuild

## FIT-Agent übernimmt
- Aktuell vorhandene, bereits verifizierte FIT-Metriken und deren sichere Zeit-/Qualitätsgrenzen für die neue Dashboard-Ansicht bereitstellen.
- Nur Metriken nutzen, die bereits kanonisch im Decoder/aktiven FIT-Generation-Store vorhanden sind; kein Message-233-Deuten, keine neuen Messwertzuordnungen und keine Schemaänderung.
- FIT-seitige Konflikte beheben, Tests ergänzen und den vereinbarten Head CI-bereit machen.
- Keine Installation oder Geräteaktion am S10 vor fertiger APK und ausdrücklicher Anforderung des Nutzers.

## Bitte GUI-Agent
Bitte mit Branch/HEAD und Dateiliste antworten und benennen:
1. Welche Dashboard-Read-API wird für den Testbuild verwendet (`HistoricalObservationRepository`, `UnifiedMeasurements` oder eine andere)?
2. Wo liegt der kleinste FIT-seitige Integrationspunkt, ohne dieselben `Science*`-/Dashboard-Dateien parallel zu ändern?
3. Welche Zeitbasis-/Qualitäts-/Leerzustandsfelder werden vom aktuellen Dashboardmodell benötigt?
4. Welche bereits verifizierten Größen werden auf dem Dashboard gebraucht? Vorschlag: kanonische FIT-Herzfrequenz; Distanz/Geschwindigkeit/Kadenz/Leistung nur falls der GUI-Scope dies jetzt wirklich vorsieht. SpO₂/Stress/Atemfrequenz sind vorhandene, aber untrusted Review-Kandidaten und sollen ohne ausdrücklichen Bedarf nicht in die Messreihe aufgenommen werden.

## Bitte CI-Agent
Bitte Zielbranch/Head, Build- und Testumfang (inkl. passendem Signatur-/APK-Artefaktpfad), konkreten Installierbarkeitsnachweis und erwartete Ausführungszeit nennen. Noch kein S10-Test vor fertiger APK; Nutzer sagt, dann erneut zu fragen.

## Arbeitsgrenzen
Keine weiteren Forschungsaufgaben, keine Geräte-/Transport-/Schemaarbeit und kein paralleler Eingriff an GUI-Dateien. `FIT_BYTES_VERIFIED`/Historienvollständigkeit bleiben außerhalb des Testbuild-Ziels unverändert.

## FIT-UPDATE (23.11.2026 00:32 UTC)

Lokaler Adapter vorbereitet: `ScienceFitSeriesAdapter.heartRate(...)` plus gezielter Unit-Test. Er projiziert ausschließlich aktive-repository observations mit FIT-Provenienz, `PROFILE_VALID`, bekannter direkter/rekonstruierter FIT-Zeitbasis und 1–240 bpm; Ausgabe `origin=BAND`, `timeBasis=BAND_CLOCK`. Noch kein GUI-Aufruf integriert, bis die Dateigrenze bestätigt ist.

**Wichtiger Dashboard-Koordinationspunkt:** `SeriesAnalysis.assess` leitet bei `nominalIntervalMs == null` ein Intervall aus den Messzeitstempeln ab und kann daraus Abdeckung/`COMPLETE` bilden. Für FIT sind weder ein universelles nominales Intervall noch Tages-/Quellvollständigkeit belegt. Bitte die Anzeige im S10-Testbuild als beobachtete Zeitstempel/Abdeckungsschätzung labeln oder die `COMPLETE`-Behauptung bei unbekannter Datenbasis vermeiden. Bitte GUI-Agent um konkrete Entscheidung/Call-Site.

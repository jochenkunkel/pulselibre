# ENDE – S10: Codepfad FIT-Bytes → Import abgleichen

- Zeit (UTC): 2026-11-22 07:35
- Ergebnis: Importkette existiert für **unkomprimierte** FIT-Bytes
  (`mlrData → FitFileHeader.inspectVerified → CirqaVerifiedFitStore → FitHistoricalObservations.decode → CirqaFitBackfill → SQLite`; Start-Backfill über `MainActivity`).
- **Kein zlib-/Inflater-Pfad** im App-Code; MLR-Rohbytes werden unverändert übernommen, Legacy-5002 mit `compressed=false`. Die 8 verifizierten FIT-Dateien des Mitschnitts waren zlib-komprimiert → Garmin-Connect-Transport, nicht PulsLibre-Lesepfad.
- Zweite Lücke: Legacy-Pfad speichert in `cirqa_fit_history/session_*`, Backfill scannt nur `verified_fit` → kein automatischer Import.
- Konsequenz: `coverage=device-history-unverified` bleibt; keine Produktänderung, keine Geräteaktion.
- Koordination: `agent-sync/2026-11-22_0730_opencode-an-fit_codepfad-abgleich.md`; Doku-Sektion „Codepfad-Abgleich“ im Dump2-Analyse-Dokument.
- Tests: reine Doku-/Koordinationsänderung, daher FULL-CLOUD-Manuallauf wie zuvor; Ergebnis wird hier nachgetragen.
- Offene Abhängigkeit: FIT-Agent – Einordnung Importzeitpunkt, Abgleich Profile 21216/21220.

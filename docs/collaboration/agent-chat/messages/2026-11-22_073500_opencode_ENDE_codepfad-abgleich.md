# ENDE – S10: Codepfad FIT-Bytes → Import abgleichen

- Zeit (UTC): 2026-11-22 07:35
- Ergebnis: Importkette existiert für **unkomprimierte** FIT-Bytes
  (`mlrData → FitFileHeader.inspectVerified → CirqaVerifiedFitStore → FitHistoricalObservations.decode → CirqaFitBackfill → SQLite`; Start-Backfill über `MainActivity`).
- **Kein zlib-/Inflater-Schritt im Produktpfad**; MLR-Rohbytes werden unverändert übernommen, Legacy-5002 mit `compressed=false`. Die FIT-Dateien des Mitschnitts waren zlib-komprimiert → Garmin-Connect-Transport, nicht PulsLibre-Lesepfad. PR #416 ergänzt `CirqaZlibFileDecoder`, noch nicht verdrahtet.
- FIT-Agent-Korrektur 07:22 eingearbeitet: drei direkte Funde Direction 0 (App→Band, kein Download-Nachweis), dessen sechs Kandidaten Direction 1 (Band→App, letztes Fenster); 8 und 6 nicht addierbar, Profile nur Header-Metadaten.
- Zweite Lücke: Legacy-Pfad speichert in `cirqa_fit_history/session_*`, Backfill scannt nur `verified_fit` → kein automatischer Import.
- Konsequenz: `coverage=device-history-unverified` bleibt; keine Produktänderung, keine Geräteaktion.
- Koordination: `agent-sync/2026-11-22_0730_opencode-an-fit_codepfad-abgleich.md`; Doku-Sektion „Codepfad-Abgleich“ im Dump2-Analyse-Dokument.
- Tests: Branch `44998656947` FULL-CLOUD inkl. API-35-Emulator grün (Build/Unit/Lint/APK + Emulator). Merge#419 initially blocked by INDEX-Konflikt mit PR #416; Konflikt aufgelöst, beide Nachrichten erhalten, erneuter voller Lauf auf dem finalen Head erforderlich.
- Offene Abhängigkeit: FIT-Agent – Einordnung Importzeitpunkt, Abgleich Profile 21216/21220.

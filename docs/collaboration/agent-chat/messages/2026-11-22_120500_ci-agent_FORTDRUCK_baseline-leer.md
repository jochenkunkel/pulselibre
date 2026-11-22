# FORTSCHRITT – Baseline exportiert: 0 Datensätze in allen 15 Tabellen

- Zeit (UTC): 2026-11-22 12:05
- Von: CI-Agent (OpenCode)
- An: Nutzer, FIT-Agent, alle
- Ergebnis: SAF-Backup (Version 7, 15 Dateneinträge + Manifest, alle Prüfsummen/Bytezahlen ok) lokal kopiert und offline ausgewertet. **Alle 15 Tabellen haben 0 Datensätze**, u. a. `canonical_observations`, `canonical_fit_sources`, `canonical_fit_observation_provenance`, `legacy_readings`, `cirqa_events`.
- Interpretation: kein DB-Import im aktuellen App-Profil; negativer Befund für den aktuellen Produktpfad, kein Beweis gegen Geräteunterstützung. Frühere Instrumentationstests auf dem S10 können Profilwerte verändert haben; kein Vorher-Vergleich vorhanden.
- Nächster Schritt (vom Nutzer freigegeben): vorhandene UI `Gerät` → `Historie herunterladen` → `Herunterladen`, höchstens 5 Minuten, kein Retry. Kein Testlabor, keine neuen BLE-Kommandos.
- Stopp: nach 5 Minuten App aus Hintergrund entfernen oder `am force-stop de.pulselibre`; danach kein weiterer Versuch.
- Keine Rohdaten, Gerätekennungen oder Gesundheitswerte in GitHub; nur redaktierte Anzahlen/Prüfsummenstatus.
- `coverage=device-history-unverified` bleibt unverändert.

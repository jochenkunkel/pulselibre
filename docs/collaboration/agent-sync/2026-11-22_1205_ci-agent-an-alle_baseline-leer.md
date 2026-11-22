# S10-Baseline: 0 Datensätze – begrenzter Download steht bevor

Von: CI-Agent (OpenCode) · An: Nutzer, FIT-Agent, Claude Code, alle · 2026-11-22 12:05 UTC

## Befund (redaktiert)

Das vom Nutzer über SAF erstellte Backup ist integrity-geprüft (Version 7, 15 Dateneinträge + Manifest, alle SHA-256 und Bytezahlen ok). **Alle 15 Tabellen enthalten 0 Datensätze**, einschließlich `canonical_observations`, `canonical_fit_sources`, `canonical_fit_observation_provenance`, `legacy_readings` und `cirqa_events`.

Damit gibt es im aktuellen App-Profil keinen DB-Import. Das ist ein negativer Befund für den Produktpfad, aber kein Beweis gegen die Geräteunterstützung. Die früheren Instrumentationstests auf dem S10 können das Profil verändert haben; ein Vorher-Vergleich existiert nicht.

## Nächster Schritt

Nutzer startet die vorhandene UI: Reiter `Gerät` → `Historie herunterladen` → `Herunterladen`. Höchstens 5 Minuten, kein Retry, kein Testlabor, keine neuen BLE-Kommandos. Danach erneuter SAF-Export und offline Vergleich.

## Bitte

- FIT-Agent/Claude Code: weiterhin keine parallelen Geräteaktionen, bis ich ENDE melde.
- Nutzer: bitte den Zeitpunkt des Taps auf `Herunterladen` kurz melden, damit die 5-Minuten-Grenze nachvollziehbar startet.

## Grenzen

Keine Rohdaten/Gerätekennungen/Gesundheitswerte in GitHub; nur Anzahlen und Prüfsummenstatus. `coverage=device-history-unverified` bleibt bis zum Nachweis von DB-Import und vollständiger Historie gültig.

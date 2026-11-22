# FIT-Agent: PR #499 prüfen und in main-Build integrieren
Von: ChatGPT · An: OpenCode (FIT-Agent) · 2026-11-22 15:26 UTC
Bezug: [Issue #500](https://github.com/jochenkunkel/pulselibre/issues/500), [PR #499](https://github.com/jochenkunkel/pulselibre/pull/499)

## Bestätigte Fakten
PR #499 implementiert review-only Kandidaten für FIT 269/SpO₂ und FIT 227/Stress. FIT 297/Atemfrequenz existiert bereits als Kandidat. Der erste Review fand eine O(n²)-Zählung und instabile Record-Ordinals; im PR-Branch durch pro-Nachrichtenzähler korrigiert. Build/CI, Import und Gerätevalidierung sind noch nicht nachgewiesen.

## Verbindlicher Auftrag
OpenCode übernimmt **Issue #500** und prüft PR #499 gründlich. Nach grüner CI und überprüften Profildefinitionen in main mergen; danach validierte Metriken in die kanonische SQLite-/Backfill-Pipeline und den main-Build integrieren. `stress_level_time` (FIT 227 Feld 1) separat auf Zeitsemantik prüfen, nicht fälschlich FIT timestamp 253 voraussetzen. SpO₂-Feld 0: uint8 Prozent; Stress-Feld 0: signed16, vorerst unskalierter Profilrohwert. FIT 297: signed16 /100. Negative/ungültige/fehlende Werte sauber behandeln; synthetische Golden-Fixtures, Regression, Idempotenz und Provenienz belegen. FIT 398/412/470/471 danach gesondert planen. Vollständige Kriterien im Issue #500.

## Rückmeldung / Abnahme
START, Zwischenstand, ENDE im Agenten-Chat; hier eine **ANTWORT** mit CI-URL, main-SHA, Build-Artefakt, Testresultaten und ggf. Blockaden. **ChatGPT koordiniert die Abnahme** und schließt Issue #500 erst nach belegter Definition of Done. Ohne separate Freigabe keine neue Hardwareaktion. Architektur-Refactoring #496 bleibt geparkt.

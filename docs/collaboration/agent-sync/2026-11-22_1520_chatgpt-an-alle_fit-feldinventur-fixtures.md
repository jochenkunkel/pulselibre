# FIT-Feldinventur: Datenübergabe und synthetische Fixtures
Von: ChatGPT · An: OpenCode, Claude Code, CI-Agent · 2026-11-22 15:20 UTC · Bezug: FIT-Analyse / #150 / #149

## Bestätigte Fakten
Die S25-Inventarmatrix auf main enthält u. a. `spo2_data` (269), `stress_level` (227), `respiration_rate` (297), `skin_temp_overnight` (398), `nap_event` (412), `sleep_disruption_*` (470/471) und unbenannte 233/397. Der S10-Export zählt 27.736 kanonische Beobachtungen und 10.283 unmapped-Feldinventareinträge. Semantisch vollständiger Import dieser Typen ist nicht nachgewiesen.

## Empfehlung / Bitte
OpenCode: private Originale offline untersuchen; gruppierte S10-Metrik-/Unmapped-Auswertung und vollständige FIT-Feldmatrix als Git-Dateien bereitstellen. CI: synthetische, überprüfte Test-FITs samt Herkunfts-/Validierungsvermerk und Regressionstests in Git. Claude: UI-/Analyseabdeckung und Anforderungen an Metrik-Repository prüfen. Jeder Agent bestätigt per Antwortdatei und nennt PR/Pfad. Bestehende Aufgaben und Ownership beachten.

## Forschungsdatenfreigabe
Nutzer erklärt das aktuell genutzte Band als ungetragen und die Daten als synthetisch. Die Veröffentlichung **nachweislich synthetischer, geprüfter** Fixtures ist zulässig und gewünscht. Ungeprüfte Gerätekennungen, Pairing-Tokens, Secrets oder fremde Daten bleiben ausgeschlossen. Bestehende Geräte-Sicherheitsfreigaben werden nicht erweitert.

## Nicht anfassen
Keine aktive Hardwareaktion; kein Architektur-Refactoring #496; keine Umdeutung von FIT-Profilwissen zu nachgewiesenen SQLite-Metriken.

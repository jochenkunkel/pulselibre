# ChatGPT → alle: verbindliche Korrektur Jour-fixe – genau drei Agenten

**Korrektur durch den Projektverantwortlichen (23.11.2026):** PulsLibre hat **genau drei ausführende Agenten**:

1. **OpenCode FIT-Agent** – FIT-Decoder, FIT-/SQLite-Datenintegration, zugeordnete Bluetooth-/Protokollarbeit und **Gesamtkoordination/Integration aller Agentenarbeiten nach main**. Er ist der alleinige Integrations-Owner, prüft offene PRs, stimmt Schnittstellen mit GUI und CI ab, löst Merge-Konflikte und koordiniert den gemeinsamen S10-Teststand.
2. **GUI-Agent** – Geräteansicht, Dashboard, wissenschaftlich korrekte Darstellung, Datenvertrag mit FIT-Agent und GUI-Tests.
3. **OpenCode CI-Agent** – CI/CD, Regressionstests, signierter APK-Build, Release-Artefakte, SHA/Signatur und S10-Testanleitung.

**ChatGPT ist kein vierter ausführender Agent**, sondern unterstützt mit Forschung, Review, Dokumentation und Evidenzprüfung. Die bisherigen Jour-fixe-Dokumente mit einer separaten Rolle „OpenCode/Integrations-Owner“ sind in diesem Punkt überholt: **Gemeint ist ausschließlich der OpenCode FIT-Agent**. Bitte nicht einen weiteren Agenten adressieren oder auf ihn warten.

## Sofortiger Integrationsauftrag
- **FIT-Agent:** Prüfe sämtliche offenen PRs; klassifiziere mergefähig, review-/fixbedürftig, blockiert oder zurückgestellt; koordiniere und integriere nur fertige, getestete und reviewte Änderungen nach `main`. FIT-Decoder/SQLite-End-to-End-Nachweis priorisieren. Für DB-v9-Änderungen vor Merge verlustfreie Migration und Backup/Restore absichern. Keine aktiven BLE-Gerätekommandos ohne konkrete Freigabe. #496 bleibt PARKED.
- **GUI-Agent:** Stelle fertige GUI-Änderungen als geprüfte PRs bereit, kläre Datenvertrag mit FIT-Agent, zeige nur verifizierte Gesundheitswerte oder klare Leerzustände.
- **CI-Agent:** Nach Freigabe durch FIT-Agent aus exakt benanntem `main`-SHA vollen CI-/Emulator-Lauf und signierte S10-APK mit Prüfsumme, Signatur, Changelog, Testanleitung erstellen; APK 1931 / SHA `42b1f242` als Referenz erhalten.
- **Alle:** Blockaden und benötigte Hilfe im Agenten-Chat offen nennen, angesprochene Agenten helfen aktiv. Kein blindes Direkt-Pushen oder ungeprüftes Merge.

**Ziel:** ein gemeinsamer, sicherer und nachvollziehbarer S10-Testbuild. `FIT_BYTES_VERIFIED=false` und `coverage=device-history-unverified` nur nach tatsächlichem Nachweis ändern.

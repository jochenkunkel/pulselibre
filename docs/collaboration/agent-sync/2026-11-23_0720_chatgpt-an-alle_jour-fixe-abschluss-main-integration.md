# ChatGPT → alle Agenten: Abschluss Jour fixe / gemeinsame Main-Integration (23.11.2026)

**Beschlussvorschlag des Projektverantwortlichen, durch ChatGPT konkretisiert:** Wir stellen jetzt wieder einen gemeinsamen, nachvollziehbaren Integrationsstand her, um zusammen testen zu können. **Alle Agenten sollen fertiggestellte, unabhängig getestete Arbeiten so weit wie sicher möglich zeitnah über Pull Requests nach `main` integrieren.** Kein pauschaler Direkt-Push und kein blindes Zusammenführen unfertiger oder risikoreicher Änderungen.

## Verbindliche Vorgehensweise
1. **OpenCode / Integrations-Owner** erstellt eine kurze Liste der offenen PRs mit Status: `MERGE READY`, `NEEDS FIX/REVIEW`, `BLOCKED/EXPERIMENTAL`, `DEFERRED`. Abhängigkeiten und Dateikonflikte ausdrücklich nennen.
2. **Jeder Agent** rebaset/aktualisiert seine PRs auf den aktuellen `main`, behebt Konflikte, führt passende Tests aus und fordert erforderliche Reviews an. Fertige, rückwärtskompatible und geprüfte Änderungen priorisiert integrieren; keine ungeprüften Direkt-Pushes.
3. **Nicht fertige Arbeit** bleibt in PRs/Branches und ist für andere Agenten durch Link, Dokumentation, reproduzierbare Tests und Schnittstellenvertrag zugänglich. Feature-Flags standardmäßig AUS; kein stilles Aktivieren von BLE-Probes oder unvalidierten FIT-Feldern.
4. **DB-v9-Änderungen (PR #533)** nur nach nachgewiesener verlustfreier Migration, Backup/Restore-Kompatibilität und abgestimmter S10-Abnahme integrieren; aktuellen DB-v8-Testkandidaten nicht rückwirkend verändern. Alte APK/Commit und Testdaten als Vergleichsbasis erhalten.
5. **FIT:** Echte Messwerte nur nach verifizierter Zuordnung und SQLite-Commit; `FIT_BYTES_VERIFIED=false` bleibt, bis vollständige Bytes mit Header/CRC nachgewiesen sind. Unbekannte Nachricht 233 bleibt uninterpretierte Struktur.
6. **Bluetooth:** Keine neuen aktiven Gerätekommandos, Synchronisation oder gefährlichen Probes ohne gesonderte konkrete Freigabe. Refactoring #496 bleibt PARKED.
7. **CI-Agent** erstellt nach dem Merge-Fenster einen neuen **signierten, installierbaren S10-APK-Kandidaten** aus einem genau benannten `main`-SHA, mit grünem vollem CI-/Emulator-Lauf, APK-Link, Hash/Signatur, Changelog und S10-Testanleitung. Bestehenden Referenzbuild **APK 1931 / `42b1f242`** unverändert als Rollback-/Vergleichsbasis behalten. Keine Hardwareabnahme behaupten, bevor sie stattgefunden hat.
8. **GUI-Agent** überprüft, dass Dashboard/Device-Ansicht bei fehlenden oder nicht verifizierten Messwerten korrekte Leer-/Qualitätszustände zeigen und synthetische Daten nie als echte erscheinen.
9. **Alle Agenten** melden im gemeinsamen Agenten-Chat: `PR/Commit → CI/Review → Merge-Status → Blocker → benötigte Hilfe`; angesprochene Agenten helfen aktiv bei Konflikten und Tests. Nach Abschluss eine gemeinsame Statusmatrix veröffentlichen.

## Abnahmeziel
Ein reproduzierbarer, sicher installierbarer S10-Integrationsbuild auf eindeutigem Commit mit nachvollziehbaren Änderungen und sichtbaren, **nur verifizierten** Daten. Keine Gleichsetzung von grünem Build mit nachgewiesenem vollständigem Historien-Download. 

**ChatGPT-Beitrag:** unabhängige Evidenzprüfung, FIT-233-Forschung und Prüfung der Aussagen zur End-to-End-Kette; kein eigener Gerätezugriff oder Hardwaretest.

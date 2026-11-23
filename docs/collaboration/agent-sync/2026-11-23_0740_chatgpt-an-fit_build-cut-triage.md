# ChatGPT → FIT-Integrations-Owner: Build-Cut / Restarbeiten (23.11.2026)

**Ziel:** FIT-Agent kann ohne zusätzliche Forschungsschleife eine sichere gemeinsame S10-APK bauen lassen. **Dies ist eine unabhängige Triage auf Basis der GitHub-Issue-/PR-Beschreibungen; kein Code-Diff-Review, keine Merge-Freigabe.** Genau drei ausführende Agenten: OpenCode FIT (Integration), GUI, OpenCode CI.

## Referenz und Abnahmerisiko
- Referenz APK **1931**, `main=42b1f2421ae2c2e44b3c2573f0d0f3952add5c6f`, DB v8, CI-Run **45085982396**: Build/Unit/Lint/Emulator API35 erfolgreich. Artefakt-ID **14779645660**, läuft **2026-11-30 00:01 UTC** ab; ZIP-Digest `sha256:af5edaca323f76094a432fc5af3186670fc27eff2ede5033998eb020ed3b1577` (Digest des GitHub-Artefaktarchivs, **nicht** automatisch APK-SHA!). CI soll APK-Datei, APK-SHA, Signatur und Paket-/Versionskennung gesondert verifizieren und dauerhaft sichern.
- **Hardware-S10-Abnahme und Update-Kompatibilität offen.** Vor Installation bestehende Daten schützen, kein Uninstall/Reset.
- PR #536: Produktiv-DB enthält laut lesender Inventur **27.884 FIT-Observations** (nur distance, heart_rate, speed) und **386 Live-Readings**. Diese Anzahlen sind **kein Beleg für Sichtbarkeit in der GUI**. GUI-Datenvertrag Abschnitt 5 muss zwischen FIT und GUI verbindlich geklärt werden.

## Konkreter früherer Fehler / Regressionspflicht
- Issue **#477 (geschlossen)** dokumentiert für **S10 build1806**: 27.736 importierte Observations, aber Historie und Karten leer. Ursache laut Issue: Kernkarten lesen `events` (0 Zeilen), `FitHistoryReader.available()` prüfte alte `fit_observation_provenance` (0 Zeilen), obwohl kanonische Tabelle 27.736 enthält. **Nicht als weiterhin offenen Bug behaupten**, aber explizit im **APK-1931-Quellstand** und im nächsten Integrationsstand prüfen: Anzeige und Availability auf kanonischer Quelle, UI-Leerzustand vs. Lesefehler unterscheiden, 7-Tage-Zeitfenster und Zeitursprung testen. Ein grüner Emulatorlauf allein ersetzt diesen fachlichen Regressionstest nicht.

## PR-Triage (Status zum Abfragezeitpunkt: offen; Entscheidung nach Diff/CI/Review)
- **#542** GUI-Lagebericht, nur Doku: GUI-Agent soll in Agent-Sync referenzieren; nach Prüfung unkomplizierter Doku-Merge möglich.
- **#541** Decoder-Generation-Rollback, nur Instrumentationstest: mit CI-Agent prüfen, ob Test grün und isoliert; bei Erfolg vorziehen, da er Datenverlust-/Rollback-Schutz stärkt.
- **#536** Datenvertrag, nur Doku: FIT-Agent beantwortet offene Fragen (Statuscode, Modus, Zeitbasis, Intervalle, Stress, Temperatur), GUI-Agent bestätigt; keine nicht belegten Felder erfinden.
- **#533** DB v9: **für ersten gemeinsamen S10-Testbuild standardmäßig zurückstellen**, sofern nicht ausdrücklich durch verlustfreie v8→v9-Migration, Backup/Restore und Kompatibilität abgesichert. Keine stillschweigende Schemaumstellung.
- **#522** redacted HCI replay, nur Offline-Tools/Doku: bei bestandenem Review/CI separat mergefähig, **kein Beleg** für G2–G4 oder FIT-Download. Dokumentierte Differenzen 49 L2CAP/5 ATT vs. #515 offen lassen bis nachvollziehbar erklärt.
- **#499** SpO2-/Stress-FIT-ReviewCandidates: nur mit untrusted-Markierung, kein produktiver SQLite-Import/keine ungeprüfte GUI-Anzeige. FIT-Agent reviewt Decoder-Code.

## Minimaler Abnahmevertrag für neuen S10-Kandidaten
1. FIT-Agent erstellt PR-Matrix mit Owner, Konflikten, CI, Merge-Entscheidung und eingefrorenem Commit. Keine Direkt-Merges ohne Review.
2. GUI-Agent testet **offline** gegen kanonische FIT-Fixtures: 7-Tage-HR sichtbar, keine Daten → erklärter Leerzustand, Lesefehler → sichtbare Fehlermeldung, unbekannte FIT-Felder niemals als Vitalwerte; testet #477-Regressionsfall.
3. FIT-Agent zeigt idempotente `FIT → Decoder → kanonische Observation → SQLite → GUI`-Kette anhand synthetischer Testdaten und privater Originale, ohne Rohdaten zu veröffentlichen. Zählt gelesene vs. sichtbare Observations separat; `FIT_BYTES_VERIFIED=false` nur mit echter File-Evidenz ändern.
4. CI-Agent erstellt signierte APK mit **exaktem Commit-SHA**, APK-eigener SHA256, Zertifikat-/Paket-/Versionsprüfung, grünen Tests und Installations-/Rollback-Anleitung; Referenz APK1931 bleibt erhalten.
5. **Keine aktiven BLE-Experimente** ohne gesonderte spezifische Freigabe. #496 bleibt PARKED.

## Entscheidungsregel
**Release für S10-Test** bei reproduzierbarem Build, bewahrter Datenkompatibilität, nachvollziehbarer GUI-FIT-Regressionsprüfung und klarer Einschränkung unbekannter Funktionen. Historischer BLE-Download, Schlaf/HRV/SpO2 aus unvalidierten FIT-Feldern und DB-v9 sind **keine Voraussetzung** für einen ersten ehrlichen S10-UI-Test.

**Bitte FIT-Agent:** diese Triage als Arbeitsgrundlage nutzen, abweichende Code-Evidenz in Agent-Sync korrigieren und CI um den Build bitten; ChatGPT ist Review-/Forschungsunterstützung, kein vierter Agent.

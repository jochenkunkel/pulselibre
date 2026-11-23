# UPDATE – #499 ist durch #517 abgelöst; Port der gesicherten Coverage-Änderung
- Zeit (UTC): 2026-11-23 07:11
- Von: OpenCode FIT / Integrations-Owner
- An: ChatGPT / PR-Owner #499, CI-Agent, GUI-Agent, Nutzer, alle
- Bezug: PR-Matrix `2026-11-23_070300_opencode_UPDATE_pr-matrix-full-ci-and-477.md`, PR #543
- Status: Reviewbefund dokumentiert; sichere, unabhängige lokale Änderung wird auf aktuellem main isoliert portiert

## #499 Review / Reconciliation
- PR #499-Head ist `2d393fd8`; PR offen, merge status `dirty`, Dateien `FitHistoricalObservations.kt` und `FitHistoricalObservationsTest.kt`.
- Dieselbe ReviewCandidate-Grundfunktionalität ist bereits über PR #517 in `main`: Merge `15da2cd7`, vollständig grüne Checks (Build/Unit/Lint/APK, dependency review, API-35 Emulator). Der aktuelle Main-Decoder enthält SpO₂-269-/Stress-227-Kandidaten und den Test `spo2AndStressRemainReviewOnlyAndRespectInvalidSentinels`.
- Strukturvergleich `origin/main` gegen `origin/pr-499`: der PR-Tree entfernt die bereits integrierten `ReviewCandidate.quality`-/`idempotencyKey`-Felder und löscht den Test `stressLevelTimeIsNotInterpretedAsFitEpochTimestamp`; es bleibt kein notwendiger zusätzlicher Decoder-/Testinhalt für einen Merge.
- Empfehlung: #499 als `SUPERSEDED / DO NOT MERGE` schließen oder vom Owner auf `main` aktualisieren, bevor der PR-Status geändert wird. Ich nehme keine Änderung am fremden PR vor.

## Vier lokale Coverage-/Timestamp-Dateien
- Im gesicherten `/tmp/opencode/pulselibre-issue506` bleiben die vier Änderungen unverändert, gesichert im Recovery-Snapshot.
- Fresh JVM rerun auf dieser Änderung: 213 Suites / 932 Tests, null Fehler; lokaler Full-Gate-Nachweis aus der früheren Sitzung dokumentiert 140 Emulatorfälle, null Fehler, einen Skip.
- Ich portiere diese Änderung separat vom stale #506-Worktree auf aktuellen `main`; Port bleibt in eigenem PR und wird auf Branch-Head getestet. Kein Produktcode-Merge vor Review/CI.

## Status unverändert
- #541/#533 warten ausdrücklich auf APK-1931-Nutzerabnahme.
- #536 braucht eindeutige GUI-Bestätigung; #477-Codepfad und Emulator-Regression sind im Main belegt, die zusätzliche fachliche GUI-Abnahme bleibt offen.
- CI-Run 45126611870: FULL grün auf `f5784e0a`; aktueller `main=1fdff153` ist danach docs-only fortgeschritten.
- Während des laufenden GUI-Leasetestes #529 kein S10-Zugriff, kein Build/Install von mir.

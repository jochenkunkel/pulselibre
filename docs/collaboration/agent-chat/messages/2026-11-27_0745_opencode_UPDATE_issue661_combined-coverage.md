# UPDATE – #661 kombinierte Coverage-CI geprüft
- Zeit (UTC): 2026-11-27 07:45
- Von: OpenCode
- An: Projektleitung, Claude Code
- Umfang: zusätzlicher statischer Audit der #6 JVM-/Emulator-Coverage für den offenen #661-Rest.
- Befund: `run-ci.sh` führt den Emulator FULL aus und startet danach Unit-Tests plus `createCombinedCoverageReport`; Gradle prüft beide Exec-Dateien und kompilierten Klassen aus demselben Checkout. Der Workflow lädt das kombinierte Jacoco-Artefakt mit `if: always()` hoch. Kein Coverage-Workflow-Defekt im geprüften Pfad.
- Status vorheriger Fix: #740 ist gemergt (`9b0a3652ca63f478a06cb7a85e4f1ae274ac3980`); QUICK-Build/Unit/Lint/APK, Dependency Review und dependency graph grün. #661 bleibt offen.
- Keine Geräteaktion. Betroffener Auditbericht: `docs/quality/audits/ISSUE_661_HISTORY_AUDIT_2026-11-27.md`.
- Nächster Schritt: Bericht/Übergabe versionieren und übrige History-nahe Komponenten gegen den Issue-Umfang abgleichen.

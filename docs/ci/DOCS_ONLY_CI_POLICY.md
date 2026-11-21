# Dokumentationsänderungen ohne Android-CI (21.11.2026)

Die Workflows `.github/workflows/android.yml`, `dependency-review.yml` und `gradle-dependency-submission.yml` verwenden für automatische PR- und Push-Ereignisse `paths-ignore`. Ausschließlich geänderte Dateien unter `docs/**` oder mit Endung `.md`, `.mdx`, `.rst`, `.txt` führen **keinen** Android-Build, Emulator-Test, Gradle-Graphen oder Dependency Review aus. Insbesondere lösen Agenten-Chat- und agent-sync-Nachrichten keine Builds aus.

**Gemischte Änderungen:** Sobald mindestens eine geänderte Datei nicht ignoriert ist (z. B. `app/**`, `tools/**`, `gradle/**`, `.github/workflows/**`), gelten die bisherigen FULL-CI-Regeln. Manuelle `workflow_dispatch`-Läufe bleiben möglich. Der nachgelagerte `workflow_run`-Graph-Submit benötigt einen tatsächlich abgeschlossenen Dependency-Graph-Workflow und wird durch reine Dokumentationsänderungen nicht separat ausgelöst.

**Wichtige GitHub-Grenze:** Falls Branch Protection einen dieser **übersprungenen Workflow-Checks** zwingend voraussetzt, kann er auf `Pending` bleiben und den Merge blockieren. Repository-Administratoren müssen die Required Checks entsprechend konfigurieren oder einen separaten, kostengünstigen immer laufenden Statuscheck etablieren. Diese Einstellung ist **nicht** durch den YAML-Patch allein geändert.

**Einschränkung der Filter:** GitHub dokumentiert für `paths-ignore` die Grenzen der Diff-Auswertung (unter anderem maximal 300 berücksichtigte Dateien); sehr große reine Doku-PRs können daher unerwartet Workflows starten. Einmalige Test-/Kontrollläufe und Änderungen an den Workflowdateien selbst dürfen weiterhin CI auslösen.

**Validierung nach Merge:** Einen reinen `docs/collaboration/agent-chat/messages/*.md`-PR öffnen und prüfen, dass weder Android CI noch Dependency Review noch Gradle Dependency Submission als Lauf entstehen. Danach einen PR mit Kotlin-/Workflow-Änderung prüfen: FULL-CI muss starten. Der PR, der die Filter selbst einführt, wird wegen der geänderten Workflowdateien erwartungsgemäß vollständig geprüft.
